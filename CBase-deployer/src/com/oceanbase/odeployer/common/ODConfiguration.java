package com.oceanbase.odeployer.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.parser.ODCommonSectionParser;
import com.oceanbase.odeployer.parser.ODISectionParser;
import com.oceanbase.odeployer.task.ODTask;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 配置文件加载器
 * <p>读取配置文件, 自动调用相应解析器识别配置项, 保存到键值集合</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODConfiguration {
    
    /** 配置项的键值对集合 */
    private static Map<ODItem, Object> CONFIGURATIONS = new HashMap<>();
    
    /** 解析配置文件的当前行 */
    private static int LINE_NUMBER = 0;
    
    /** 解析函数返回值 */
    private static ODError RETURN = ODError.SUCCESS;
    
    /** 保存所有的解析错误信息 */
    private static StringBuilder ERROR_BUILDER = new StringBuilder();
    
    /** first是&lt;sectionName, taskName&gt;, second是ip列表, 若引用了某个task, 会添加该task的IP列表<br>
     * 用于预连接和打印到头部 */
    private static List<Pair<Pair<String, String>, List<String>>> IP_LIST = new ArrayList<>();
    
    /** first是&lt;sectionName, taskName&gt;, second是配置项值对列表, 若引用了某个task, 会添加该task的值列表<br>
     * 用于打印到头部 */
    private static List<Pair<Pair<String, String>, List<Pair<String, String>>>> ITEM_LIST = new ArrayList<>();
    
    //add zhangyf [paxos] 20170109
	/** 判断所所启动的RS、UPS数目与所设定的RS、UPS数目是否一致 */
	private static String stValueRsCount;
	private static String stValueUpsCount;
    
	/** 设定所启动的RS、UPS为主RS以及主UPS*/
	private static String stValueMrs;
	private static String stValueMups;
	//end add
    /**
     * 加载配置文件
     * @param filename 路径/文件
     * @param cmd 子命令
     * @param sectionParsers section解析器, 自定义的解析器必须实现SectionParser接口
     * @return 已捕获解析相关的所有异常
     */
    public Pair<ODError, Map<ODItem, Object>> loadConf(String filename, ODCommand cmd, List<ODISectionParser> sectionParsers) { //启用默认解析器 	       
    	if(RETURN.isSuccess()) {
            BufferedReader bReader = null;
            try {
                try {
                    bReader = new BufferedReader(new FileReader(new File(filename)));
                } catch (Exception e) {
                    System.out.println("[ERROR] Open configure file '" + filename + "' error!");
                    RETURN = ODError.ERROR;
                }
                List<String> lineList = new ArrayList<>(); // 先保存配置文件
                if(RETURN.isSuccess()) {
                    String tmpLine;
                    LINE_NUMBER = 1; // 文件行号从1开始
                    while(null != (tmpLine = bReader.readLine())) {
                        tmpLine = tmpLine.trim();
                        lineList.add(tmpLine);
                        if(!tmpLine.startsWith("#")) { // #行注释
                            String itemStr = tmpLine.split("=")[0].trim();
                            ODItem item = ODItem.valueOf(itemStr);
                            if(item == ODItem.UNKNOWN || item.isHidden()) { // 检查配置项是否已定义
                                printError(ODError.ERROR_UNKNOWN_ITEM, itemStr);
                            }
                        }
                        LINE_NUMBER++;
                    } // end while
                    for(ODISectionParser parser: sectionParsers) {
                        LINE_NUMBER = 1; // 文件行号从1开始
                        // 解析开始前
                        parser.before();
                        for(String line: lineList) {
                            if(!line.startsWith("#")) { // #行注释
                                String[] args = line.split("#")[0].split("="); // 行后#注释
                                String itemStr = args[0].trim();
                                // 解析对应的配置项前缀和子命令
                                if(itemStr.startsWith(parser.getSectionName() + ".")) {
                                    ODItem item = ODItem.valueOf(itemStr);
                                    if(item != ODItem.UNKNOWN && !item.isHidden()) { // 忽略H_前缀的配置项
                                        boolean isEof = item.isClose(); // 结束符不满足"配置项 = 值"的格式
                                        boolean isNullable = item.isNullable(); // 配置项可为空                       
                                        if(args.length == 2 || isEof) { // "配置项 = 值"
                                            String value = null;
                                            if(!isEof) {
                                                value = args[1].trim();                              
                                            }
                                            if(isEof || value.length() > 0) {                                  
												//add zhangyf [paxos] 20170109
												if(String.valueOf(item).equals("start.rscount"))
												{																										
													stValueRsCount = value;														
												}else if(String.valueOf(item).equals("start.upscount"))
												{
													stValueUpsCount = value;														
												}else if(String.valueOf(item).equals("start.mrs"))
												{
													 stValueMrs = value;
												}else if(String.valueOf(item).equals("start.mups"))
												{
													 stValueMups = value;												 
												}
												//end add											
                                                parser.parseConfigureItems(cmd, item, value);
                                            } else if (!isNullable) {
                                                printError(ODError.ERROR_EMPTY_VALUE, item);
                                            }
                                        } else if(line.length() > 0 && !isNullable) {
                                            printError(ODError.ERROR_PATTERN);
                                        }
                                    } 
                                }
                            }
                            LINE_NUMBER++;
                        }
                        // 解析结束后
                        parser.after();
                    }
                    @SuppressWarnings("unchecked")
                    List<ODServer> serverList = (List<ODServer>) CONFIGURATIONS.get(ODItem.H_SERVERS);
                    // 检查主机列表是否为空
                    if(!ODDeployer.DEBUG && serverList.size() == 0) {
                        printError(ODError.ERROR_EXCEPTION, "Server list is empty!");
                    }
                }
            } catch (Exception e) {
                printError(ODError.ERROR_EXCEPTION, ODUtil.parseException(e));
                RETURN = ODError.ERROR;
            } finally {
                if(bReader != null) {
                    try {
                        bReader.close();
                    } catch (IOException e) {
                        if(ODDeployer.DEBUG) {
                            e.printStackTrace();
                        }
                        RETURN = ODError.ERROR;
                    }
                }
                // 添加错误信息到返回值
                addConfigureItem(ODItem.H_ERROR_MESSAGE, ERROR_BUILDER.toString());
            }
        }
        return new Pair<>(RETURN, CONFIGURATIONS);
    }

    /**
     * 添加新的配置项
     * @param item 配置项
     * @param value 值
     */
    public static void addConfigureItem(ODItem item, Object value) {
        if(!CONFIGURATIONS.containsKey(item)) {
            CONFIGURATIONS.put(item, value);
        } else {
            printError(ODError.ERROR_CONFLICT, item);
        }
    }
    
    /**
     * 获取配置项集合
     * @return 在解析过程中调用，集合可能不完整
     */
    public static Map<ODItem, Object> getConfigurations() {
        return CONFIGURATIONS;
    }

    public static int getLineNumber() {
        return LINE_NUMBER;
    }
    
    // ------------------------------------------------------------------ Ip list
    
    /**
     * 添加使用到的IP
     * @param sectionName section
     * @param taskName task
     * @param ip ip
     */
    public static void addIp(String sectionName, String taskName, String ip) {
        List<String> ipList = getIpListOrAdd(sectionName, taskName);
        ipList.add(ip);
    }
    
    /**
     * 将source section的IP列表添加到target task中去
     * @param targetSection 当前的section
     * @param targetTask 当前的task
     * @param sourceSection 引用的section
     */
    public static void addIpList(String targetSection, String targetTask, String sourceSection) {
        List<String> source = getIpList(sourceSection);
        List<String> target = getIpListOrAdd(targetSection, targetTask);
        if(source != null) {
            target.addAll(source);
        }
    }
    
    /**
     * 将source task的IP列表添加到target task中去
     * @param targetSection 当前的section
     * @param targetTask 当前的task
     * @param sourceSection 引用的section
     * @param sourceTask 引用的task
     */
    public static void addIpList(String targetSection, String targetTask, String sourceSection, String sourceTask) {
        List<String> source = getIpList(sourceSection, sourceTask);
        List<String> target = getIpListOrAdd(targetSection, targetTask);
        if(source != null) {
            target.addAll(source);
        }
    }
    
    /**
     * 获取section使用到的IP列表
     * @param sectionName section
     * @return 可能为null
     */
    public static List<String> getIpList(String sectionName) {
        List<String> tmp = new ArrayList<>();
        for(Pair<Pair<String, String>, List<String>> p: IP_LIST) {
            if(p.first.first.equals(sectionName)) {
                tmp.addAll(p.second);
            }
        }
        return tmp;
    }
    
    /**
     * 若不存在，则添加一个空列表
     * @param sectionName section
     * @param taskName task
     * @return 不会为null
     */
    private static List<String> getIpListOrAdd(String sectionName, String taskName) {
        List<String> list = getIpList(sectionName, taskName);
        if(list == null) {
            //若不存在，则添加一个空列表
            list = new ArrayList<>();
            IP_LIST.add(new Pair<Pair<String, String>, List<String>>(new Pair<String, String>(sectionName, taskName), list));
        }
        return list;
    }
    
    /**
     * 获取task使用到的IP列表
     * @param sectionName section
     * @param taskName task
     * @return 若能为null
     */
    public static List<String> getIpList(String sectionName, String taskName) {
        for(Pair<Pair<String, String>, List<String>> p: IP_LIST) {
            if(p.first.equals(sectionName, taskName)) {
                return p.second;
            }
        }
        return null;
    }
    
   // ------------------------------------------------------------------ Item list
    
    /**
     * 添加配置项列表
     * @param sectionName section
     * @param taskName task
     * @param item 会截去前面section的名称
     * @param value 未经任何处理的配置项值
     */
    public static void addItem(String sectionName, String taskName, ODItem item, String value) {
        List<Pair<String, String>> ipList = getItemListOrAdd(sectionName, taskName);
        if(item.isName()) {
            ipList.add(new Pair<String, String>("[task]", value));
        } else {
            ipList.add(new Pair<String, String>(item.getShorName(), value));
        }
    }
    
    /**
     * 将source section的配置项列表添加到target task中去
     * @param targetSection 当前的section
     * @param targetTask 当前的task
     * @param sourceSection 引用的section
     */
    public static void addItemList(String targetSection, String targetTask, String sourceSection) {
        List<Pair<String, String>> source = getItemList(sourceSection);
        List<Pair<String, String>> target = getItemListOrAdd(targetSection, targetTask);
        if(source != null) {
            List<Pair<String, String>> tmp = new ArrayList<>();
            tmp.add(new Pair<String, String>("[include section]", sourceSection));
            tmp.addAll(source);
            tmp.add(null); // 添加一个空行
            target.addAll(0, tmp);
        }
    }
    
    /**
     * 将source task的配置项列表添加到target task中去
     * @param targetSection 当前的section
     * @param targetTask 当前的task
     * @param sourceSection 引用的section
     * @param sourceTask 引用的task
     */
    public static void addItemList(String targetSection, String targetTask, String sourceSection, String sourceTask) {
        List<Pair<String, String>> source = getItemList(sourceSection, sourceTask);
        List<Pair<String, String>> target = getItemListOrAdd(targetSection, targetTask);
        if(target != null) {
            List<Pair<String, String>> tmp = new ArrayList<>();
            tmp.add(new Pair<String, String>("[include section]", sourceSection));
            tmp.addAll(source);
            tmp.add(null); // 添加一个空行
            target.addAll(0, tmp);
        }
    }
    
    /**
     * 获取section的配置项列表
     * @param sectionName section
     * @return 可能为null
     */
    public static List<Pair<String, String>> getItemList(String sectionName) {
        List<Pair<String, String>> tmp = new ArrayList<>();
        for(Pair<Pair<String, String>, List<Pair<String, String>>> p: ITEM_LIST) {
            if(p.first.first.equals(sectionName)) {
                tmp.addAll(p.second);
            }
        }
        return tmp;
    }
    
    /**
     * 若不存在，则添加一个空列表
     * @param sectionName section
     * @param taskName task
     * @return 不会为null
     */
    private static List<Pair<String, String>> getItemListOrAdd(String sectionName, String taskName) {
        List<Pair<String, String>> list = getItemList(sectionName, taskName);
        if(list == null) {
            //若不存在，则添加一个空列表
        list = new ArrayList<>();
        ITEM_LIST.add(new Pair<Pair<String, String>, List<Pair<String, String>>>(new Pair<String, String>(sectionName, taskName), list));
        }
        return list;
    }
    
    /**
     * 获取task的配置项列表
     * @param sectionName section
     * @param taskName task
     * @return 若不存在，则添加一个空列表
     */
    public static List<Pair<String, String>> getItemList(String sectionName, String taskName) {
        for(Pair<Pair<String, String>, List<Pair<String, String>>> p: ITEM_LIST) {
            if(p.first.equals(sectionName, taskName)) {
                return p.second;
            }
        }
        //若不存在，则添加一个空列表
        List<Pair<String, String>> list = new ArrayList<>();
        ITEM_LIST.add(new Pair<Pair<String, String>, 
                List<Pair<String, String>>>(new Pair<String, String>(sectionName, taskName), list));
        return list;
    }
    
    // ------------------------------------------------------------------ Section and Task
    
    /**
     * section必须继承 ODCommonSectionParser
     * @param sectionName section
     * @return 可能为null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ODTask> getSection(String sectionName) {
        ODISectionParser parser = ODDeployer.getInstance().getSectionParser(sectionName);
        if(parser != null) {
            ODCommonSectionParser p = null;
            try {
                p = (ODCommonSectionParser) parser;
            } catch (Exception e) {
                ODLogger.error("Parser '" + sectionName + "' must extend ODCommonSectionParser!", 
                        (new Throwable()).getStackTrace());
            }
            return (Map<String, ODTask>) getConfigurations().get(p.getItem());
        }
        return null;
    }
    
    /**
     * 从配置单元中获取task, 可在解析过程中调用
     * @param sectionName section type必须是0
     * @return 可以null
     */
    public static ODTask getTaskFromSection(String sectionName) {
        return getTaskFromSection(sectionName, sectionName);
    }
    
    /**
     * 从配置单元中获取task, 可在解析过程中调用
     * @param sectionName section type应该是1或2
     * @param taskName task的名称
     * @return 可能null
     */
    public static ODTask getTaskFromSection(String sectionName, String taskName) {
        Map<String, ODTask> map = getSection(sectionName);
        // map may be null
        if(map != null) {
            return map.get(taskName);
        }
        return null;
    }

    // ------------------------------------------------------------------ Error Message
    
    /**
     * 自定义的异常信息
     * @param message　提示信息
     * @param lineNumber　配置项所有行号
     */
    public static void printError(String message, int lineNumber) {
        printError(ODError.ERROR_EXCEPTION, null, message, true, lineNumber);
    }
    
    /**
     * 配置项值格式错误
     * @param code ERROR_PATTERN
     */
    private static void printError(ODError code) {
        printError(code, null, null, true);
    }
    
    // add zhangyf [paxos] Ds:U/u for paxos
    /** 获取所预先设定的U/u值*/
    public static String getValueRsCount() {	
  	    return stValueRsCount;
    }
    
    public static String getValueUpsCount() {
    	return stValueUpsCount;
    }
    /** 获取所预先设定的主RS和主UPS*/
    public static String getValueMrs() {
    	return stValueMrs;
    }
    public static String getValueMups() {
    	return stValueMups;
    }
    
    /**
     * @param code ERROR_EMPTY_VALUE, ERROR_WRONG_VALUE, ERROR_CONFLICT
     * @param item 配置项
     */
    public static void printError(ODError code, ODItem item) {
        printError(code, item, null, true);
    }
    
    public static void printError(ODError code, String message) {
        printError(code, null, message, true);
    }
    
    public static void printError(ODError code, String message, boolean withLineNumber) {
        printError(code, null, message, withLineNumber);
    }
    
    public static void printError(ODError code, ODItem item, boolean withLineNumber) {
        printError(code, item, null, withLineNumber);
    }

    public static void printError(ODError code, ODItem item, String message, int lineNumber) {
        printError(code, item, message, true, lineNumber);
    }
    
    public static void printError(ODError code, ODItem item, String message) {
        printError(code, item, message, true);
    }    

    private static void printError(ODError code, ODItem item, String message, boolean withLineNumber) {
        printError(code, item, message, withLineNumber, -1);
    }
    /**
     * 
     * @param code 错误类型
     * @param item 配置项
     * @param message 提示信息
     * @param withLineNumber 是否输出行号
     * @param lineNumber 若=-1，则使用全局的行号
     */
    private static void printError(ODError code, ODItem item, String message,
                                   boolean withLineNumber, int lineNumber) {
        String msg = "";
        String itemStr = null;
        if(item != null) {
            itemStr = item.toString();
        }
        if(lineNumber < 0) {
            lineNumber = LINE_NUMBER;
        }
        switch(code) {
        case ERROR_PATTERN:
            msg += "Should be '配置项 = 值' in configuration file: " + lineNumber;
            break;
        case ERROR_UNKNOWN_ITEM:
            msg += "Unknown item '" + message + "' is found in configuration file: " + lineNumber;
            break;
        case ERROR_EMPTY_VALUE:
            msg += "The value of '" + itemStr + "' is empty in configuration file: " + lineNumber;
            break;
        case ERROR_WRONG_VALUE:
            assert item != null;
            msg += "The value of '" + itemStr + "' is expected to be '" + item.getValuePattern().toString() + "' in configuration file: " + lineNumber;
            break;
        case ERROR_MISS_ITEM:
            msg += "Configure item '" + itemStr + "' is expected";
            if(message != null) {
                msg += " for '" + message + "'";
            }
            if(withLineNumber && lineNumber > 0) {
                msg += " in configuration file: " + lineNumber;
            } else {
                msg += "!";
            }
            break;
        case ERROR_CONFLICT:
            msg += "Conflict item '" + itemStr + "' is found in configuration file: " + lineNumber;
            break;
        default:
            msg += "Exception: '" + message + "'";
            if(withLineNumber && lineNumber > 0) {
                msg += " in configuration file: " + lineNumber;
            } else {
                msg += "!";
            }
            break;
        }
        RETURN = ODError.ERROR;
        ERROR_BUILDER.append("[").append(code.toString()).append("] ").append(msg).append(ODUtil.SEPARATOR);
    }
}
