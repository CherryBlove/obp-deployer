package com.oceanbase.odeployer.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.task.ODStartTask;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODShell;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * Oceanbase
 * <p>单例模式: ODOceanbase oceanbase = ODOceanbase.getInstance()</p>
 * 管理主机列表, 定义的集群级别的基本功能
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public final class ODOceanbase {

    
    /** 单例模式 */
    private static ODOceanbase instance = new ODOceanbase();

    /** 是否初始化 */
    private boolean isInit;

    /** 主机列表 */
    private List<ODServer> serverList = new ArrayList<>();

    /** 禁用默认的构造函数 */
    private ODOceanbase() {}

    /** Oceanbase惟一实例 */
    public static ODOceanbase getInstance() {
        return instance;
    }

    /**
     * 初始化目录和端口号，并预连接各主机结点
     * @param serverList 主机列表
     * @param oceanbaseDir 安装目录
     * @param rsPort RS端口号
     * @return 只能初始化一次
     */
    public ODError init(List<ODServer> serverList, String oceanbaseDir, String rsPort, String upsPortp, String upsPortm, 
    		String msPortp, String msPortz, String csPortp, String csPortn, boolean noPreConnect) {
        ODError ret = ODError.SUCCESS;
        if(!isInit) {
                this.serverList.addAll(serverList);
                ODShell.init(oceanbaseDir, rsPort, upsPortp, upsPortm, msPortp, msPortz, csPortp, csPortn);
                isInit = true;
                if(ODDeployer.CONNECT && !noPreConnect) {
                    if(!connect()) { // 连接成功
                        ret = ODError.ERROR;
                    }
                }
        }
        return ret;
    }

    /**
     * 启动所有Server
     * @return 必须先初始化
     * @throws Exception 
     */
    public ODError start() throws Exception {
        ODError ret = ODError.SUCCESS;
        if(isInit) {
            if(!serverList.isEmpty()) {
                //构建ODStartTask启动所有Server
                ODStartTask task = new ODStartTask("all-start");
                for(ODServer server: serverList) {
                    String[] servernames = {"rs", "ups", "ms", "cs"};
                    // action类型为 START_SERVER 
                    ODAction action = new ODAction(ODItem.START_SERVER, server, Arrays.asList(servernames));
                    task.addAction(action);
                }
                task.execute(null, "start");
            }
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
            ret = ODError.ERROR;
        }
        return ret;
    }

    /**
     * 停止所有Server
     * @param force 强制停止
     * @return 必须先初始化
     */
    public ODError stop(boolean force) {
        ODError ret = ODError.SUCCESS;
        if(isInit) {
            for(ODServer server: serverList) {
                server.stop(force);
            }
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
            ret = ODError.ERROR;
        }
        return ret;
    }

    /**
     * 部署集群
     * @param sourceIp 源Oceanbase所在主机IP
     * @param sourceName 登录帐号
     * @param sourcePass 登录密码
     * @param sourceOB 源安装目录
     * @return 必须先初始化且主机列表不为空
     */
    public ODError deploy(String sourceIp, String sourceName, String sourcePass, String sourceOB) throws Exception {
        ODError ret = ODError.SUCCESS;
        if(isAnyServerAlive()) {
            ODLogger.log("Please stop Oceanbase before deploy!");
            ret = ODError.ERROR;
        } else {
            if(isInit && serverList != null && serverList.size() > 0) {
                ODLogger.log("Deploy oceanbase to:");
                for(ODServer server: serverList) {
                    ODLogger.log("[" + server.ip + "]: " + ODShell.getOceanbaseDir());
                }
                for(ODServer server: serverList) {
                    ret = server.mkdir();
                    if(ret.isError()) {
                        break;
                    }
                }
                if(ODDeployer.CONNECT && ret.isSuccess()) {
                    // user指当前OD工具的用户, 由第一个Server的username + oceanbase拼接而成
                    // 在复制文件时用于区分不同的程序产生的expect文件
                	String user = serverList.get(0).username + ODShell.getOceanbaseDir().replace("~", "").replace("/", ".");
//                    String user = serverList.get(0).username + ODShell.getOceanbaseDir().replace("~", "_").replace("/", ".");
                	ODRemoteConnector rc = new ODRemoteConnector(sourceIp, sourceName, sourcePass);
                	Pair<ODError , String> testRet = rc.executeValue("expect -v");
                	if (testRet.second != null&& testRet.second.contains("command not found")){
                		System.out.println("'expect' is not found");
                		ret = ODError.ERROR;
                	}else{  
                    rc.executeDirect(ODShell.getDeployCmd(serverList, sourceOB, user));
                    Thread.sleep(1000); // 等待一定时间
                    int sec = 0;
                    int lastLineLen = 0;
                    int total = serverList.size();
                    String filename = "*" + user + ".exp.tmp";
//                    String filename = "*." + user + ".exp.tmp";

                    while (true) {
                        Pair<ODError, String> expRet = rc.executeValue("ls " + sourceOB + "/" + filename);
//                    	Pair<ODError, String> expRet = rc.executeValue("cd " + sourceOB + "; ls " + filename);
                        ret = expRet.first;
                        String result = expRet.second;                           
                        if(result == null || (ret.isError() && !result.startsWith("ls:"))) {   	
                        	if(result != null && !result.startsWith("ls:") && result.contains("No such file or directory")){
                        		System.out.println("[ERROR] No such directory:" +sourceOB);
                        	}
                            break;
                        } 
                        else if(result.startsWith("ls:")) {	                       	
                            ODUtil.clearConsole(lastLineLen); //删除上一行             
                            String line = "Deploying to [" + serverList.get(total - 1).ip+ "](" + total + "/" + total + "), " +
                                    "Wait:" + sec + " sec";
                            System.out.println(line);
                            ret = ODError.SUCCESS;
                            break;
                        } else {
                            String[] ips = result.split("." + user + ".exp.tmp");
                            int index = serverList.size() - ips.length;                                
                            if(index >= 0 && index < serverList.size()) {                           
                                int done = total - ips.length;
                                ODUtil.clearConsole(lastLineLen); //删除上一行
                                String line = "Deploying to [" + serverList.get(index).ip+ "](" + done + "/" + total + "), " +
                                        "Please wait:" + sec + " sec";
                                lastLineLen = line.length();
                                System.out.print(line);
                            }
                        }                                              
                        Thread.sleep(1000);
                        sec++;
                    }
                    rc.close();
                    if(ret.isSuccess()) {
                        ODLogger.log("Deploy done!");
                       }
                    }
                } else {
                    ret = ODError.ERROR;
                }
            } else {
                ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
            }
        }
        return ret;
    }

    /**
     * 是否有任意Server进程存活
     * @return boolean
     */
    public boolean isAnyServerAlive() {
        if(isInit) {
            for(ODServer server: serverList) {
                if(server.isAnyServerAlive()) {
                    return true;
                }
            }
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
        }
        return false;
    }

    /**
     * 按Server列表的顺序获取可用的MS
     * @return ip
     */
    public String getAliveMsIp() {
        if(isInit) {
            for(ODServer server: serverList) {
                if(server.isMSAlive()) {
                    return server.ip;
                }
            }
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
        }
        return null;
    }

    /**
     * 集群主机节点列表
     * @return 必须先初始化
     */
    public List<ODServer> getServerList() {
        if(isInit) {
            return serverList;
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
        }
        return null;
    }

    /** 关闭连接 */
    public void close() {
        if(isInit) {
            for(ODServer server: serverList) {
                server.close();
            }
        }
    }

    /** 用于调试 */
    public String toString() {
        String str = "Oceanbase={serverList=[";
        for(ODServer server: serverList) {
            str += server + ",";
        }
        str = ODUtil.removeLastChar(str);
        str += "]}";
        return str;
    }

    // ----------------------------------------------------------- private

    /** 连接各主机节点 */
    private boolean connect() {
        if(isInit) {
            for(ODServer server: serverList) {
                if(!server.connect(true)) {
                    return false;
                }
            }
        } else {
            ODLogger.error("ODOceanbase is not initialized!", (new Throwable()).getStackTrace());
            return false;
        }
        return true;
    }


}
