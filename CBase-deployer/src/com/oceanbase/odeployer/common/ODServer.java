package com.oceanbase.odeployer.common;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.start.ODStartParameter;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODPrinter;
import com.oceanbase.odeployer.util.ODShell;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 服务器主机节点
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODServer {

    /** Server名字数组 */
    public static ODServerName[] SERVER_NAMES = {
            ODServerName.RS,
            ODServerName.UPS,
            ODServerName.MS,
            ODServerName.CS
    };

    public String ip;

    public String username;

    public String password;

    protected String network;
    
    /** 获取主机列表*/
    public static List<String> allServerList = new ArrayList<>();

    /** 是否已清除系统日志./data/ */
    private boolean hasWipeData = false;

    /** 是否已清除./log/数据 */
    private boolean hasWipeLog = false;
    
    /** 是否已清除./etc/*.bin数据 */
    private boolean hasWipeEtc = false;

    private ODRemoteConnector conn;

    public ODServer(String ip, String username, String password, String network) {
        this.ip = ip;
        this.username = username;
        this.password = password;
        this.network = network;
    }
    
    @Override
    public String toString() {
        return "[" + ip + "," + username + "," + password + "," + network + "]";
    }

    /**
     * 建立远程连接
     * @param printLog 是否打印连接提示信息
     * @return 是否连接成功
     */
    public boolean connect(boolean printLog) {
        if(conn == null) {
            if(printLog) {
                System.out.println("> Connect to -----> " + ip + " >");
                allServerList.add(ip);
            }
            conn = new ODRemoteConnector(ip, username, password);
            return conn.isConnectSuccess();
        }
        return true;
    }

    /**
     * 启动Server
     * 若是第一次启动默认会清除系统日志和data数据
     * @param servernames Server的类型
     * @param rsIP 主RS的IP
     * @param isWipeData 启动OB时重建./data目录
     * @param isWipeLog 启动OB时清除系统日志./log
     * @param isWipeEtc 启动OB时清除系统配置文件./etc/*.config.bin
     * @param parameters 启动附加参数
     */
    public void start(double serverWait,List<ODServerName> servernames, String masterRsIP,String masterUpsIP, ODStartParameter parameters,
                boolean isWipeData, boolean isWipeLog, boolean isWipeEtc) {
    	ODUtil.sleep(serverWait * 1000);
        if(servernames != null) {
        	StringBuilder logBuilder = new StringBuilder(); // 提示信息
        	boolean wLog = false; // 本次start操作是否清空log
        	boolean wEtc = false;
        	boolean wData = false;
            StringBuilder sb = new StringBuilder();
            sb.append(ODShell.getCdOceanbaseDir()).append(";");
            if(isWipeLog && !hasWipeLog) { // 是否已清除系统日志log/
                sb.append(ODShell.getClearSystemLogCmd()).append(";");
                hasWipeLog = true;
                wLog = true;
            }
            if(isWipeEtc && !hasWipeEtc) { // 是否已清除系统配置文件
                sb.append(ODShell.getClearConfigureCmd()).append(";");
                hasWipeEtc = true;
                wEtc = true;
            }
            if(isWipeData && !hasWipeData) { // 是否已清除data/数据
                sb.append(ODShell.getResetDataCmd());
                hasWipeData = true;
                wData = true;
            }
            if(wLog || wEtc || wData) {
            	logBuilder.append("wipe:");
            	if(wLog) {
            		logBuilder.append(" log");
            	}
            	if(wEtc) {
            		logBuilder.append(" etc");
            	}
            	if(wData) {
            		logBuilder.append(" data");
            	}
            	logBuilder.append(", ");
            }
            logBuilder.append("start:");
            String rsIp = masterRsIP;
            if(!parameters.isUseMasterRsIp()) {
                rsIp = parameters.getLocalRsIp(); // 使用小集群RS的IP
            }
            //e:mod
            for(ODServerName server: servernames) {
            	logBuilder.append(server.toShortName() + " ");
                switch (server) {
                case RS:
                	sb.append("./bin/").append(ODShell.getStartRSCmd(ip, masterRsIP, network, parameters.getRsParameter())).append(";");
                    break;
                case UPS:
                    sb.append("./bin/").append(ODShell.getStartUPSCmd(rsIp, network, parameters.getUpsParameter())).append(";");
                    break;
                case MS:
                    sb.append("./bin/").append(ODShell.getStartMSCmd(rsIp, network, parameters.getMsParameter())).append(";");
                    break;
                case LMS:
                    sb.append("./bin/").append(ODShell.getStartLMSCmd(rsIp, network, parameters.getMsParameter())).append(";");
                    break;
                case CS:
                    sb.append("./bin/").append(ODShell.getStartCSCmd(rsIp, network, parameters.getCsParameter())).append(";");
                    break;
                default:
                    ODLogger.error((new Throwable()).getStackTrace());
                    break;             
                }
            }
            ODLogger.log("[" + ip + "]: " + logBuilder.toString());
            exec(sb.toString());
        }
    }

    /**
     * 停止所有Server
     * @param force 是否强制停止
     */
    public void stop(boolean force) {
//    	ODLogger.log("pid   +   ret   "+ODShell.getAllPidCmd()+"|"+ODShell.getKillAllCmd(force));
        Pair<ODError, String> pidRet = execValue(ODShell.getAllPidCmd());
        Pair<ODError, String> ret = exec(ODShell.getKillAllCmd(force));

        //检查是否kill成功
        String pidResult = pidRet.second;
        String result = ret.second;
        int count = 4;
        String fails = "";
        if(pidRet.first.isSuccess() && pidResult != null) {
            String[] servernames = {"rs", "ups", "ms", "cs"};
            String[] pids = pidResult.split(ODUtil.SEPARATOR);
            for(int i = 0; i < pids.length; i++) {
                String pid = pids[i];
                if(result != null && result.contains("(" + pid + ")")) { // kill success
                    count--;
                    fails += servernames[i] + " ";
                }
            }
        }
        String log = "[" + ip + "]: Stop all server, force=" + force + ", Success: " + count;
        if(count < 4) {
            log += ", (Fail:" + fails.substring(0, fails.length() - 1) + ")";
        }
        ODLogger.log(log);
    }
    
    /**
     * 停止单个Server
     * @param servername server名称
     * @param force 是否强制kill -15/-9
     */
    public void stop(ODServerName servername, boolean force) {
        ODLogger.log("[" + ip + "]: " + servername);
        exec(ODShell.getKillServerCmd(servername, force));
    }

    /**
     * 停止部分Server
     * @param servernames Server类别
     * @param force 是否强制kill -15/-9
     */
    public void stop(List<ODServerName> servernames, boolean force) {
        StringBuilder sb = new StringBuilder();
        for(ODServerName serverType: servernames) {
            ODLogger.log("[" + ip + "]: " + serverType);
            sb.append(" && ").append(ODShell.getKillServerCmd(serverType, force));
        }
        exec(sb.toString().substring(4));
    }

    /**
     * 检查MS进程是存活
     * @return boolean
     */
    public boolean isMSAlive() {
        Pair<ODError, String> ret = exec(ODShell.getCheckServerAliveCmd(ODServerName.MS));
        String result = ret.second;
        //noinspection ConstantConditions
        return (result != null && result.contains(ODServerName.MS.toString()));
    }

    /**
     * 发送bootstrap命令到本主机的RS
     * @param bootstrapTimeout 超时
     * @return bootstrap是否成功
     */
    public ODError bootstrap(int bootstrapTimeout) {
        ODError ret = ODError.SUCCESS;
        ODPrinter.printMessageLine("Bootstrap");
        String shell = ODShell.getCdOceanbaseDir() + ";./bin/" + ODShell.getBootstrapCmd(ip, bootstrapTimeout);
        Pair<ODError, String> retBoot = execWaiting(shell);
        ODUtil.sleep(1000);
        String result = retBoot.second;
        ODLogger.log(result);
        if(result == null || !result.contains("Okay")) {
            ret = ODError.ERROR;
            ODLogger.error("Bootstrap failed, please check your Configuration file and IPport", new Throwable().getStackTrace());
        }
        return ret;
    }

    /**
     * 检查是否有任意Server存活
     * @return boolean
     */
    public boolean isAnyServerAlive() {
        Pair<ODError, String> ret = checkServerAlive();
        if(ret.first.isSuccess()) {
            for(ODServerName serverName: SERVER_NAMES) {
                //noinspection ConstantConditions
                if(ret.second.contains(serverName.toString())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查指定的Server是否存活
     * @param servername
     * @return boolean
     */
    public boolean isServerAlive(ODServerName servername) {
        Pair<ODError, String> ret = exec(ODShell.getCheckServerAliveCmd(servername));
        if(ret.first.isSuccess()) {
            if(ret.second.contains(servername.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建oceanbase的安装目录
     * @return 是否执行成功
     */
    ODError mkdir() {
        ODError ret = exec(ODShell.getMkOceanbaseDir()).first;
        if(ret.isError()) {
            ODLogger.log("[ERROR] mkdir [" + ODShell.getOceanbaseDir() + "] fail in [" + ip + "] to deploy cluster!");
        }
        return ret;
    }
    
    public List<List<String>> checkServerStatus() {
        List<List<String>> rows = new ArrayList<>();
        Pair<ODError, String> ret = checkServerAlive();
        if(ret.first.isSuccess()) {
            String result = ret.second;
            boolean isFirstRow = true;
            for(ODServerName servername: ODServer.SERVER_NAMES) {
                List<String> row = new ArrayList<>();
                if(isFirstRow) { // 只有第一行的第一列添加IP
                    row.add(ip);
                    isFirstRow = false;
                } else {
                    row.add("");
                }
                row.add(servername.toString());
                row.add(getServerStatus(servername.toString(), result));
                rows.add(row);
            }
            rows.add(null); // 横线
        }
        return rows;
    }
    
    private String getServerStatus(String servername, String result) {
        if(result != null && result.contains(servername)) {
            return "Yes";
        }
        return "No";
    }
    
    /**
     * 执行远程命令
     * @param shell shell命令
     * @return second.result
     */
    public Pair<ODError, String> exec(String shell) {
        connect(true);
        Pair<ODError, String> ret =  conn.execute(shell);
        String result = ret.second;
        if(ret.first.isError()) {
            if(result != null
                    && !result.startsWith("1 :ERROR: List of process IDs must follow -p")
                    && !result.contains("No such process") && !result.contains("Server Not Start")
                    && !result.contains("ob_pcap.cpp:559")
            		&& !result.contains("(1) - Operation not permitted") 
            	//add zhangyf 161103 b:
                    && !result.contains("不允许的操作")
                    && !result.contains("没有此进程")){
            	//e:add
                ODLogger.log(result);
            }
        }
        return ret;
    }

    /**
     * 关闭远程连接
     */
    void close() {
        if(conn != null) {
            conn.close();
        }
    }

    // ----------------------------------------------------------- protected
    /**
     * 显示计时器
     * @param shell shell shell命令
     * @return second.result
     */
    protected Pair<ODError, String> execWaiting(String shell) {
        connect(false);
        return conn.executeWaiting(shell);
    }

    /**
     * 返回结果不加行号
     * @param shell shell shell命令
     * @return second.result
     */
    protected Pair<ODError, String> execValue(String shell) {
        connect(false);
        return conn.executeValue(shell);
    }

    /**
     * 检查各Server进程是否存活
     * @return second.result
     */
    protected Pair<ODError, String> checkServerAlive() {
        return exec(ODShell.getCheckAllAliveCmd());
    }

}
