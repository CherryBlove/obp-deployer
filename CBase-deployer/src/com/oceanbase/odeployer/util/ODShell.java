package com.oceanbase.odeployer.util;

import java.util.List;

import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.start.ODParameter;

/**
 * Shell命令
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODShell {

    /** 各Server启动所需要的端口号 */
    private static String rsPort;
    private static String upsPort;
    private static String upsMPort;
    private static String csPort;
    private static String msPort;
    private static String msZPort;

    /**CHUNKSERVER -N*/
    private static String csPortN;
    
    /** 创建oceanbase的安装目录 */
    private static String oceanbaseDir;

    // ----------------------------------------------------------- 初始化

    /**
     * 根据RS的端口号计算其它Server的端口号
     * @param rsPort RS端口号
     */
    public static void init(String oceanbaseDir, String rsPort, String upsPortp, String upsPortm, String msPortp, String msPortz, String csPortp, String csPortn) {
//        String prefix = rsPort.substring(0, rsPort.length() - 1);
          ODShell.rsPort = rsPort;
//        upsPort = prefix + "1";
//        upsMPort = prefix + "2";
//        msPort = prefix + "3";
//        msZPort = prefix + "4";
//        csPort = prefix + "5";

          upsPort = upsPortp;
		  upsMPort = upsPortm;
		  msPort = msPortp;
		  msZPort = msPortz;
		  csPort = csPortp;
		  csPortN = csPortn;
        ODShell.oceanbaseDir = oceanbaseDir;
    }

    // ----------------------------------------------------------- 启动相关

    /**
     * Bootstrap
     * @param RSIp RS所在主机IP
     * @bootstrapTimeout 超时时间,默认600
     * @return shell命令
     */
    public static String getBootstrapCmd(String RSIp, int bootstrapTimeout) {
        if(bootstrapTimeout > 0) {
            bootstrapTimeout *= 1000000;
        } else {
            bootstrapTimeout = 600000000;
        }
        return "rs_admin -r " + RSIp + " -p " + rsPort + " -t " + bootstrapTimeout + " boot_strap";
    }

    /**
     * 启动RS
     * rootserver -r 0.0.0.0 -R 0.0.0.0:0 -i bond0 {-OPTION VALUE}
     * @param RSIp RS的IP
     * @param masterRSIp 主RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @return shell命令
     */
    public static String getStartRSCmd(String RSIp, String masterRSIp, String network,
                                       List<ODParameter> parameters) {
        return getStartServerCmd(RSIp, masterRSIp, network, parameters, ODServerName.RS);
    }

    /**
     * 启动UPS
     * updateserver -r 0.0.0.0 -p 0 -m 0 -i bond0 {-OPTION VALUE}
     * @param RSIp RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @return shell命令
     */
    public static String getStartUPSCmd(String RSIp, String network, List<ODParameter> parameters) {
    	return getStartServerCmd(RSIp, null, network, parameters, ODServerName.UPS);
    }

    /**
     * 启动MS
     * mergeserver -r 0.0.0.0 -p 0 -z 0 -i bond0 {-OPTION VALUE}
     * @param RSIp RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @return shell命令
     */
    public static String getStartMSCmd(String RSIp, String network, List<ODParameter> parameters) {
    	return getStartServerCmd(RSIp, null, network, parameters, ODServerName.MS);
    }

    /**
     * 启动LMS
     * mergeserver -r 0.0.0.0 -p 0 -z 0 -t lms -i bond0 {-OPTION VALUE}
     * @param RSIp RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @return shell命令
     */
    public static String getStartLMSCmd(String RSIp, String network, List<ODParameter> parameters) {
    	return getStartServerCmd(RSIp, null, network, parameters, ODServerName.LMS);
    }

    /**
     * 启动CS
     * chunkserver -r 0.0.0.0 -p 0 -n obtest -i bond0 {-OPTION VALUE}
     * @param RSIp RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @return shell命令
     */
    public static String getStartCSCmd(String RSIp, String network, List<ODParameter> parameters) {
    	return getStartServerCmd(RSIp, null, network, parameters, ODServerName.CS);
    }

    /**
     * 启动四类Server
     * @param RSIp RS的IP
     * @param masterRSIp 主RSIP
     * @param network 网卡名称
     * @param isLMS MS类型是否为LMS
     * @param rsParameters RS的附加启动参数
     * @param upsParameters UPS的附加启动参数
     * @param msParameters MS的附加启动参数
     * @param csParameters CS的附加启动参数
     * @return shell命令
     */
    public static String getStartAllCmd(String RSIp, String masterRSIp, String network, boolean isLMS,
                                        List<ODParameter> rsParameters, List<ODParameter> upsParameters,
                                        List<ODParameter> msParameters, List<ODParameter> csParameters) { 	
    	StringBuilder sb = new StringBuilder();
        sb.append("./bin/").append(getStartRSCmd(RSIp, masterRSIp, network, rsParameters)).append(";");
        sb.append("./bin/").append(getStartUPSCmd(masterRSIp, network, upsParameters)).append(";");
        if(isLMS) {
            sb.append("./bin/").append(getStartLMSCmd(masterRSIp, network, msParameters)).append(";");
        } else {
            sb.append("./bin/").append(getStartMSCmd(masterRSIp, network, msParameters)).append(";");
        }
        sb.append("./bin/").append(getStartCSCmd(masterRSIp, network, csParameters)).append(";");
        return sb.toString();
    }


    /**
     * 启动Server命令
     * @param RSIp RS的IP
     * @param masterRSIp 主RS的IP
     * @param network 网卡名称
     * @param parameters 附加启动参数
     * @param serverName Server的名称
     * @return shell命令
     */
    private static String getStartServerCmd(String RSIp, String masterRSIp, String network,
                                           List<ODParameter> parameters, ODServerName serverName) {  
    	StringBuilder sb = new StringBuilder();
        sb.append(serverName);
        
        sb.append(" -r ").append(RSIp).append(":").append(rsPort);
        switch (serverName) {
            case RS:
                sb.append(" -R ").append(masterRSIp).append(":").append(rsPort);
                break;
            case UPS:
                sb.append(" -p ").append(upsPort);
                sb.append(" -m ").append(upsMPort);
                break;
            case MS:
                sb.append(" -p ").append(msPort);
                sb.append(" -z ").append(msZPort);
                break;
            case LMS:
                sb.append(" -p ").append(msPort);
                sb.append(" -z ").append(msZPort);
                sb.append(" -t lms");
                break;
            case CS:
                sb.append(" -p ").append(csPort);
                sb.append(" -n ").append(csPortN);
                break;
            default:
                ODLogger.error((new Throwable()).getStackTrace());
                break;
        }
        sb.append(" -i ").append(network);
        //add 20170222
          if(parameters != null) {
              for(ODParameter parameter: parameters) {
                String option = parameter.name;
                String value = parameter.value;
                if(RSIp.equals(masterRSIp))
                {
                	if(option.equals("U")||option.equals("u")||option.equals("C"))
                	{
                		sb.append(" -").append(option).append(" ").append(value);	
                	}
                }else if(option.equals("C"))
                {
                         sb.append(" -").append(option).append(" ").append(value);               	
                }
              }
           }
        return sb.toString();
    }

    // ----------------------------------------------------------- 停机相关

    /**
     * 停止Server
     * @param serverName Server的名称
     * @param force 是否强制停止 kill -9/-15
     * @return shell命令
     */
    public static String getKillServerCmd(ODServerName serverName, boolean force) {
        int signal = -15;
        if(force) {
            signal = -9;
        }
        return "kill " + signal + " `" + getServerPidCmd(serverName) + "`";
    }

    /**
     * 停止所有Server
     * @return shell命令
     */
    public static String getKillAllCmd(boolean force) {
        StringBuilder sb = new StringBuilder();
        for(ODServerName server: ODServer.SERVER_NAMES) {
            sb.append(getKillServerCmd(server, force)).append(";");
        }
        return sb.toString();
    }

    // ----------------------------------------------------------- 进程相关

    /**
     * 获取所有Server的进程号
     * @return shell命令
     */
    public static String getAllPidCmd() {
        StringBuilder sb = new StringBuilder();
        for(ODServerName server: ODServer.SERVER_NAMES) {
            sb.append(getServerPidCmd(server)).append(";");
        }
        return sb.toString();
    }

    /**
     * Server的进程号
     * @param serverName Server的名称
     * @return shell命令
     */
    public static String getServerPidCmd(ODServerName serverName) {
        return "cat " + oceanbaseDir + "/run/" + serverName + ".pid";
    }

    /**
     * 检查Server进程是否存活
     * @param serverName Server的名称
     * @return shell命令
     */
    public static String getCheckServerAliveCmd(ODServerName serverName) {
        return "ps -p `" + getServerPidCmd(serverName) + "`";
    }

    /**
     * 检查所有Server进程是否存活
     * @return shell命令
     */
    public static String getCheckAllAliveCmd() {
        StringBuilder sb = new StringBuilder();
        for(ODServerName server: ODServer.SERVER_NAMES) {
            sb.append(getCheckServerAliveCmd(server)).append(";");
        }
        return sb.toString();
    }

    // ----------------------------------------------------------- 初始化Oceanbase,目录文件操作相关

    /**
     * 安装目录
     * @return shell命令
     */
    public static String getOceanbaseDir() {
        return oceanbaseDir;
    }
    
    /**
     * 清除系统日志./log/
     * @return shell命令
     */
    public static String getClearSystemLogCmd() {
        return "rm -f log/*";
    }
    
    /**
     * 清除系统配置文件./etc/*.bin
     * @return shell命令
     */
    public static String getClearConfigureCmd() {
        return "rm -f etc/*.bin";
    }

    /**
     * 创建oceanbase的安装目录
     * @return shell命令
     */
    public static String getMkOceanbaseDir() {
        return "mkdir -p " + oceanbaseDir + "/log";
    }

    /**
     * 打开到oceanbase安装目录
     * @return shell命令
     */
    public static String getCdOceanbaseDir() {
        return "cd " + oceanbaseDir;
    }
    
    public static String getRsPort() {
        return rsPort;
    }
    
    public static String getUpsPort() {
        return upsPort;
    }
    
    public static String getMsPort() {
        return msPort;
    }
    
    public static String getCsPort() {
        return csPort;
    }

    public static String getMsZPort() {
        return msZPort;
    }
    
    /**
     * 重建data目录
     * @return shell命令
     */
    public static String getResetDataCmd() {
        StringBuilder sb = new StringBuilder();
        sb.append("rm -rf ./data/;");
        sb.append("mkdir -p ./data/admin_ups1;");
        sb.append("mkdir -p ./data/admin_ups2;");
        for(int i = 1; i <= 6; i++) {
            sb.append("mkdir -p ./data/").append(i).append("/obtest/sstable;");
        }
        sb.append("mkdir -p ./data/rs;");
        sb.append("mkdir -p ./data/rs_commitlog;");
        sb.append("mkdir -p ./data/ups_commitlog;");
        sb.append("mkdir -p ./data/ups_data/raid0;");
        sb.append("ln -s ").append(oceanbaseDir).append("/data/admin_ups1 ./data/ups_data/raid0/store0;");
        sb.append("ln -s ").append(oceanbaseDir).append("/data/admin_ups2 ./data/ups_data/raid0/store1;");
        return sb.toString();
    }

    /**
     * 部署
     * @param serverList 主机列表
     * @param originOceanbase 源oceanbase安装目录
     * @param user 程序用户名，用于区分不同的部署事件
     * @return shell命令
     */
    public static String getDeployCmd(List<ODServer> serverList, String originOceanbase, String user) {  	
        StringBuilder sb = new StringBuilder();
        sb.append("cd ").append(originOceanbase)
                .append(";rm -f *.").append(user).append(".exp.tmp;mkdir -p run;")
                .append("echo 1 > run/rootserver.pid;echo 1 > run/updateserver.pid;")
                .append("echo 1 > run/chunkserver.pid;echo 1 > run/mergeserver.pid;");
        // create expect file      
        for(ODServer server: serverList) {
            String filename = server.ip + "." + user + ".exp.tmp";
            sb.append("echo -e '");
            sb.append(getCreateExpectFileCmd(server.ip, server.username, server.password));
            sb.append("' > ").append(filename).append(";");
        }
        // execute expect file
        for(ODServer server: serverList) {
            String filename = server.ip + "." + user + ".exp.tmp";
            sb.append("expect ").append(filename).append(";rm -f ").append(filename).append(";");
        }
        return sb.toString();
    }

    /**
     * 生成用于复制oceanbase目录的.expect文件
     * @param ip 远程主机的IP
     * @param username 帐号
     * @param password 密码
     * @return shell命令
     */
    private static String getCreateExpectFileCmd(String ip, String username, String password) {
        return "#!/usr/bin/expect -f\\n" +
                "spawn scp -r bin etc include run lib mrsstable_lib_5u mrsstable_lib_6u "
                + username + "@" + ip + ":" + oceanbaseDir + "\\n" +
                "expect {\\n" +
                "\"*(yes/no)*\" { send \"yes\\\\n\"; exp_continue }\\n" +
                "\"*password:\" { send \"" + password + "\\\\n\" }\\n" +
                "}\\n" +
                "set timeout 30000\\n" +
                "expect eof";
    }

}
