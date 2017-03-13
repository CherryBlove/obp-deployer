package com.oceanbase.odeployer.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.oceanbase.odeployer.ODDeployer;

/**
 * 系统日志
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public class ODLogger {
    
    private static boolean isInit = false;

    /** 换行符 */
    private static String SEPARATOR = System.getProperty("line.separator");
        
    private static FileWriter fw;

    /**
     * 初始化
     * @param filepath 日志文件路径和文件名
     */
    public static void init(String filepath) {
        String dir = filepath.substring(0, filepath.lastIndexOf("/"));
        File fileDir = new File(dir);
        fileDir.mkdirs();
        File file = new File(filepath);
        try {
            file.createNewFile();
            fw = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        isInit = true;
    }

    public static void error(StackTraceElement stacks[]) {
        error("unexpected error!", stacks);
    }

    /**
     * 打印错误信息,含时间、代码行等
     * @param msg 信息
     * @param stacks (new Throwable()).getStackTrace()
     */
    public static void error(String msg, StackTraceElement stacks[]) {
        String log = "[" + ODUtil.getSystemTime() +"]";
        StackTraceElement stack = stacks[0];
        log += " ERROR " + stack.getMethodName() + "(" + stack.getFileName() + ":" + stack.getLineNumber() + ") " + msg;
        log(log);
    }

    /**
     * 只有在debug模式下才打印，且不输出到日志文件
     * @param msg 信息
     * @param stacks 调用栈
     */
    public static void debug(String msg, StackTraceElement stacks[]) {
        if(ODDeployer.DEBUG) {
        	String log = "[" + ODUtil.getSystemTime() +"]";
            StackTraceElement stack = stacks[0];
            log += " DEBUG " + stack.getMethodName() + "(" + stack.getFileName() + ":" + stack.getLineNumber() + ") " + msg;
            System.out.println(log);
        }
    }

    /**
     * 输出日志，同时打印信息到控制台和日志文件
     * @param msg 信息
     */
    public static void log(String msg) {
        log(msg + SEPARATOR, true);
    }

    /**
     * @param msg 信息
     * @param isPrintToConcole 是否输出到控制台
     */
    private static void log(String msg, boolean isPrintToConcole) {
        if(isPrintToConcole) {
            System.out.print(msg);
        }
        if(isInit) {
            try {
                fw.write(msg);
                fw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[ERROR] Logger has not been initialized!");
        }
    }

    public static void destroy() {
        if(null != fw) {
            try {
                fw.close();
                isInit = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
