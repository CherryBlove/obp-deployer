package com.oceanbase.odeployer.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oceanbase.odeployer.annotation.ODCommandAttribute;
import com.oceanbase.odeployer.parser.ODISectionParser;
import com.oceanbase.odeployer.parser.ODOceanbaseSectionParser;
import com.oceanbase.odeployer.parser.ODStartSectionParser;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 子命令类
 * <p>继承该类, 添加自定义的子命令:</p>
 * <blockquote><pre>
 * 1. public static final ChildCommand NEW_COMMAND = new ChildCommand("NEW_COMMAND");
 * 2. 定义ChildCommand(String)型的构造函数
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODCommand {
    
    /** 子命令的名称 */
    private String name;
    
    /** 缩写名称，由若干首字母+尾字母组成  */
    private String shortName;

    /** 子命令列表 */
    private static List<ODCommand> values = new ArrayList<>();
    
    /** 子命令名称和缩写的集合，用于重复性检查 */
    private static Set<String> nameSet = new HashSet<>();

    /** 实际输入参数 */
    private List<String> argumentList;
    
    /** 子命令属性 */
    private Attribute attribute = new Attribute();

    /** 子命令 */
    // ------------------------------------------------------- commands
    
    public static final ODCommand UNKNOWN = new ODCommand("UNKNOWN");
    
    @ODCommandAttribute(
            argument="startname",
            section=ODStartSectionParser.class,
            description="custom start")
    public static final ODCommand START = new ODCommand("START");
    
    @ODCommandAttribute(
            connectAll=true,
            description="start all server(clear all data)")
    public static final ODCommand ALL_START = new ODCommand("ALL_START");
    
    @ODCommandAttribute(
            argument="f|nf",
            connectAll=true,
            description="stop all server(force:-9, nonforce:kill -15)")
    public static final ODCommand ALL_STOP = new ODCommand("ALL_STOP");
    
    @ODCommandAttribute(
            section=ODOceanbaseSectionParser.class,
            connectAll=true,
            description="deploy oceanbase to all nodes")
    public static final ODCommand DEPLOY = new ODCommand("DEPLOY");
    
    @ODCommandAttribute(
            description="create 'config/odeploy.cfg.template'")
    public static final ODCommand CONFIGURATION = new ODCommand("CONFIGURATION");
    
    // ------------------------------------------------------- public
    
    /**
     * 子类的构造函数必须为public
     * @param name 子命令名称
     */
    public ODCommand(String name) {
        if(name != null) {
            this.name = name.replace("_", "-").toLowerCase(); //转换名称
//System.out.println("debug (ODCommend) : "+this.name);//debug 161227
            if(nameSet.contains(this.name)) { // 检查重复
                ODLogger.error("command '" + name + "' is duplicate!", (new Throwable()).getStackTrace());
            } else {
                values.add(this); // 添加到列表
                String sname = null;
                int shortSize = 1;
                if(this.name.length() < 3) {
                    sname = this.name;
                } else {
                    do { // 获取名称缩写形式
                        if(shortSize < name.length()) {
                            sname = this.name.substring(0, shortSize) + this.name.substring(name.length() - 1);
//System.out.println("debug (shortname) : "+sname);
                        } else {
                            ODLogger.error((new Throwable()).getStackTrace());
                        }
                        shortSize++;
                    } while(nameSet.contains(sname));
                }
                this.shortName = sname; 
                nameSet.add(this.name);
                nameSet.add(this.shortName);
//debug 161227
//for(ODCommand debugod:ODCommand.values)
//{
//	System.out.println("debug (ODCommand nameset)"+debugod);
//}
            }
        } else {
            ODLogger.error("command name is null!", (new Throwable()).getStackTrace());
        }
    }

    public String toString() {
        return name;
    }

    /**
     * 子命令首字字母和尾字母的缩写形式, 若命令长度小于3, 则返回原值<br>
     * 缩写字母长度动态计算，若新增命令的缩写与原有命令冲突，则缩写长度会自增，直到没有冲突
     * @return 子命令缩写
     */
    public String getShortCommand() {
        return shortName;
    }
    
    /**
     * 转换子命令
     * @param name 全写
     * @return
     */
    public static ODCommand valueOfFullName(String name) {
        if(name != null) {
            return valueOf(name.replace("_", "-").toLowerCase());
        }
        return UNKNOWN;
    }
    
    /**
     * 转换子命令
     * @param cmd 子命令字符串, 首字母和尾字母的缩写组合或全
     * @return 子命令
     */
    public static ODCommand valueOf(String cmd) {
        for(ODCommand command: ODCommand.values) {
            if(cmd.equalsIgnoreCase(command.name) || cmd.equalsIgnoreCase(command.shortName)) {
                return command;
            } 
        }
        return UNKNOWN;
    }
    
    /**
     * 子命令参数数目
     * @return 可能为0
     */
    public int getArgumentsNum() {
        try {
            return ODUtil.splitToList(attribute.argument).size();
        } catch(Exception e) {
            ODLogger.error((new Throwable()).getStackTrace());
            return 0;
        }
    }
    
    /**
     * 是否绑定的配置单元
     * @return
     */
    public boolean isBindingSection() {
    	return attribute.section != ODISectionParser.class;
    }
    
    /**
     * 绑定的配置项
     * @return
     */
    public Class<? extends ODISectionParser> getBindingSection() {
        return attribute.section;
    }
    
    /**
     * 子命令参数名称
     * @return 按配置原样返回
     */
    public String getArgumentNames () {
        String names = attribute.argument;
        if(names != null) {
            names = names.replace(",", " ");
//ODLogger.log("debug names "+names);
//System.out.println("debug names "+names);
        }
        return names;
    }

    /**
     * 是否需要预连接所有节点
     * @return boolean
     */
    public boolean getNeedPreconnectAll() {
        return attribute.connectAll;
    }
    
    /**
     * 是否需要连接MS
     * @return boolean
     */
    public boolean getNeedAliveMS() {
        return attribute.connectMS;
    }

    /**
     * 子命令的使用说明
     * @return first: command|cmd [arguments], second: description
     */
    public static List<Pair<String, String>> getUsage() {
        List<Pair<String, String>> ret = new ArrayList<>();
        for(ODCommand cmd: values) {
            String usage = cmd.toString();
            if(cmd != UNKNOWN && usage.length() > 0) { // 忽略未定义的子命令
                usage += "|" + cmd.getShortCommand(); // 缩写
                if(cmd.getArgumentsNum() > 0) {
//ODLogger.log("debug (getArguments) "+cmd.getArguments());
                    usage += " [" + cmd.getArguments() + "]";
                }
                ret.add(new Pair<>(usage, cmd.getDescription()));
                if(cmd == ODCommand.CONFIGURATION /*||
                    cmd == ODCommand.FETCH_SYSTEM_LOG*/) {
                    ret.add(null); // 加入空行
                }
            }
        }
        return ret;
    }

    /** 实际输入参数 */
    public List<String> getArgumentList() {
//    	ODLogger.log("para_stop"+argumentList);
        return argumentList;
    }

    /** 实际输入参数 */
    public void setArgumentList(List<String> argumentList) {
        this.argumentList = argumentList;
    }
    
    /**
     * 设置属性值
     * @param argument
     * @param section
     * @param connectAll
     * @param connectMS
     * @param description
     */
    public void setAttribute(String argument, Class<? extends ODISectionParser> section,
            boolean connectAll, boolean connectMS, String description) {
        attribute.argument = argument;
        attribute.section = section;
        attribute.connectAll = connectAll;
        attribute.connectMS = connectMS;
        attribute.description = description;
    }

    // ----------------------------------------------------------- private
    /**
     * 子命令参数文字描述
     * @return 多个值时, 以空格分隔
     */
    private String getArguments() {
        return attribute.argument.trim().replace(",", " ");
    }

    /**
     * 描述信息,用于生成程序用法
     * @return string
     */
    private String getDescription() {
        return attribute.description;
    }

    private class Attribute {
        
        /** 子命令接受参数, 用','号分隔 */
        private String argument = "";
        
        /** 子命令绑定的配置单元, 每个子命令最多只可绑定一个配置单元 */
        private Class<? extends ODISectionParser> section = ODISectionParser.class;
        
        /** 是否需要预连接所有IP节点 */
        private boolean connectAll = false;
        
        /** 是否需要连接MS */
        private boolean connectMS = false;
        
        /** 说明 */
        private String description = "";
    }

}
