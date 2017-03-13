package com.oceanbase.odeployer.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 远程连接
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public class ODRemoteConnector {
    
    private Connection conn = null;
    
    private boolean isAuthenticated;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    /** 忽略返回的错误信息前缀 */
    private static String[] IGNORED_PREFIX = {"kill: usage", "cat:"};
    
    public ODRemoteConnector(String ip, String username, String password) {
        try {
            conn = new Connection(ip);
            if(ODDeployer.CONNECT) {
                conn.connect();
                isAuthenticated = conn.authenticateWithPassword(username, password);
            } else {
                isAuthenticated = true;
            }
            if(!isAuthenticated) {
                ODLogger.log("[ERROR] Connect to [" + ip + "] using [user:'" + username + "', password:'" + password + "'] fail!");
            }
        } catch (Exception e) {
            ODLogger.log("[ERROR] Connect to " + ip + " timeout!");
            if(ODDeployer.DEBUG) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean isConnectSuccess() {
        return isAuthenticated;
    }
    
    /**
     * 不处理返回结果
     * @param cmd shell命令
     */
    public void executeDirect(String cmd) {
        execute(cmd, false, false, false);
    }
    
    /**
     * 不加行号
     * @param cmd shell命令
     * @return second.result
     */
    public Pair<ODError, String> executeValue(String cmd) {
        return execute(cmd, true, false, false);
    }
    
    /**
     * 显示计时器
     * @param cmd shell命令
     * @return second.result
     */
    public Pair<ODError, String> executeWaiting(String cmd) {
        return execute(cmd, true);
    }
    
    /**
     * 不显示计时器
     * @param cmd shell命令
     * @return second.result
     */
    public Pair<ODError, String> execute(String cmd) {
        return execute(cmd, false);
    }
    
    /**
     * 
     * @param cmd shell命令
     * @param needCount 是否显示计时器
     * @return second.result
     */
    public Pair<ODError, String> execute(String cmd, boolean needCount) {
        return execute(cmd, true, needCount, true);
    }
    
    public void close() {
        try {
            if(conn != null) {
                conn.close();
            }
            if(executor != null) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**************** private ****************/
    
    /**
     * @param cmd shell命令
     * @param needResult 是否返回远程执行结果
     * @param needCount 是否显示计时器
     * @param needLineNumber 返回结果是否添加行号
     * @return second.result
     */
    private Pair<ODError, String> execute(String cmd, boolean needResult, boolean needCount,
            boolean needLineNumber) {
        if(ODDeployer.PRINT_SHELL) {
            ODUtil.printShell(cmd);
        }
        if(!ODDeployer.CONNECT) {
            return new Pair<>(ODError.SUCCESS, "");
        }
        ODError ret = ODError.SUCCESS;
        String result = null;
        ODCountRunnable countRunnable = null;
        try {
            if(isAuthenticated) {
                Session session = conn.openSession();
                if(needCount) { //显示计时器
                    countRunnable = new ODCountRunnable();
                    executor.submit(countRunnable);
                }
                session.execCommand(cmd);
                if(needResult) { //处理返回值
                    String err = readInputStream(session.getStderr(), needLineNumber);
                    String out = readInputStream(session.getStdout(), needLineNumber);
                    if(err != null && err.length() > 0) { 
                        if(ODDeployer.PRINT_SHELL) {
                            System.out.println("ERROR:");
                        }
                        ret = ODError.ERROR;
                        result = err;
                    } else if(out != null && out.length() > 0){
                        if(ODDeployer.PRINT_SHELL) {
                            System.out.println("SUCCESS:");
                        }
                        result = out;
                    }
                    if(ODDeployer.PRINT_SHELL) {
                        System.out.println(result);
                        System.out.println("------------ End shell -----------");
                    }
                }
                
                session.close();
            } else {
                ret = ODError.ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret = ODError.ERROR;
        } finally {
            if(countRunnable != null) {
                countRunnable.stop();
            }
        }
        return new Pair<>(ret, result);
    }
    
    private String readInputStream(InputStream in, boolean needLineNumber) throws IOException {
        StringBuilder retBuffer = new StringBuilder();
        BufferedReader bReader = new BufferedReader(
                new InputStreamReader(in, Charset.defaultCharset().toString()));
        String line;
        int index = 1;
        while(null != (line = bReader.readLine())) {
            boolean isIgnored = false;
            for(String prefix: IGNORED_PREFIX) {
                if(line.startsWith(prefix)) {
                    isIgnored = true;
                    break;
                }
            }
            if(!isIgnored) {
                if(needLineNumber) {
                    String indexStr = index + "  ";
                    retBuffer.append(indexStr.substring(0, 2)).append(":");
                }
                retBuffer.append(line).append(System.getProperty("line.separator"));
                index++;
            }
        }
        bReader.close();
        String ret = retBuffer.toString();
        if(ret.length() > System.getProperty("line.separator").length()) {
            ret = ret.substring(0, ret.length() - System.getProperty("line.separator").length());
        }
        return ret;
    }
    
    /**
     * 计时器
     * OceanbaseDeployer
     * @author lbzhong
     * @date 2016年3月30日
     */
    private class ODCountRunnable implements Runnable {
        
        private boolean isStop = false;

        @Override
        public void run() {
            int i = 1;
            System.out.print("Wait: ");
            while(!isStop) {
                System.out.print(i);
                if(!isStop) {
                    ODUtil.sleep(1000);
                    for(int j = 0; j < String.valueOf(i).length(); j++) {
                        System.out.print("\b");
                    }
                    i++;
                }
            }
            System.out.println(i - 1);
        }
        
        void stop() {
            isStop = true;
        }

    }
}
