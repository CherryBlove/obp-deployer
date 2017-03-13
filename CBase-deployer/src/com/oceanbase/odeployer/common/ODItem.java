package com.oceanbase.odeployer.common;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.action.ODStartActionExecutor;
import com.oceanbase.odeployer.annotation.ODItemAttribute;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 配置项
 * "H_"前缀表示hidden, 在配置文件中不可见
 * <p>继承该类, 添加自定义的配置项:</p>
 * <blockquote><pre>
 * 1. public static final ChildItem NEW_ITEM = new ChildItem("NEW_ITEM");
 * 2. 定义ChildItem(String)型的构造函数
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODItem {

    /** 配置项的名称 */
    private String name;
    
    /** 配置项列表 */
    private static List<ODItem> values = new ArrayList<>();
    
    private Attribute attribute = new Attribute();

    // ------------------------------------------------------- items
    
    public static final ODItem UNKNOWN = new ODItem("UNKNOWN");
    public static final ODItem H_ERROR_MESSAGE = new ODItem("H_ERROR_MESSAGE"); // 解析错误信息
    
    // ------------------------------------------------------- oceanbase
    @ODItemAttribute(
            pattern="source ip",
            defaultValue="0.0.0.0")
    public static final ODItem OCEANBASE_SOURCE_IP = new ODItem("OCEANBASE_SOURCE_IP");
    
    @ODItemAttribute(
            pattern="$H_OCEANBASE_SOURCE_USERNAME,$H_OCEANBASE_SOURCE_PASSWORD",
            defaultValue="admin,admin")
    public static final ODItem OCEANBASE_SOURCE_USER = new ODItem("OCEANBASE_SOURCE_USER");
    
    @ODItemAttribute(
            pattern="directory",
            defaultValue="~/oceanbase_source",
            description="source oceanbase installation directory")
    public static final ODItem OCEANBASE_SOURCE_DIR = new ODItem("OCEANBASE_SOURCE_DIR");
    
    @ODItemAttribute(
            pattern="directory",
            defaultValue="~/oceanbase",
            description="target oceanbase installation directory")
    public static final ODItem OCEANBASE_TARGET_DIR = new ODItem("OCEANBASE_TARGET_DIR");
    
    @ODItemAttribute(
            pattern="$H_OCEANBASE_AFTERSTART_WAIT,$H_OCEANBASE_BOOTSTRAP_WAIT",
            defaultValue="5,3",
            description="wait after start(second), wait before bootstrap(second)")
    public static final ODItem OCEANBASE_WAIT = new ODItem("OCEANBASE_WAIT");
    
    @ODItemAttribute(pattern="username")
    public static final ODItem H_OCEANBASE_SOURCE_USERNAME = new ODItem("H_OCEANBASE_SOURCE_USERNAME");
    
    @ODItemAttribute(pattern="password")
    public static final ODItem H_OCEANBASE_SOURCE_PASSWORD = new ODItem("H_OCEANBASE_SOURCE_PASSWORD");
            
    @ODItemAttribute(pattern="number")
    public static final ODItem H_OCEANBASE_AFTERSTART_WAIT = new ODItem("H_OCEANBASE_AFTERSTART_WAIT");
    
    @ODItemAttribute(pattern="number")
    public static final ODItem H_OCEANBASE_BOOTSTRAP_WAIT = new ODItem("H_OCEANBASE_BOOTSTRAP_WAIT");
    
    // ------------------------------------------------------- server
    @ODItemAttribute(
            pattern="number",
            defaultValue="0")
    public static final ODItem SERVER_RS_PORT = new ODItem("SERVER_RS_PORT");
    
    //add upsport/csport/msport/lmsport
    @ODItemAttribute(
    		pattern="string",
    		defaultValue="0,0")
    public static final ODItem SERVER_UPS_PORT = new ODItem("SERVER_UPS_PORT");
    
    @ODItemAttribute(
    		pattern="String",
    		defaultValue="0,0")
    public static final ODItem SERVER_MS_PORT = new ODItem("SERVER_MS_PORT");
    
//    @ODItemAttribute(
//    		pattern="String",
//    		defaultValue="0,0")
//    public static final ODItem SERVER_lMS_PORT = new ODItem("SERVER_LMS_PORT");
    
    @ODItemAttribute(
    		pattern="String",
    		defaultValue="0,obtest"
    		)
    public static final ODItem SERVER_CS_PORT = new ODItem("SERVER_CS_PORT");
    //end 
    
    @ODItemAttribute(
            pattern="version name",
            defaultValue="OB_PAXOS")
    public static final ODItem SERVER_VERSION = new ODItem("SERVER_VERSION");
    
    @ODItemAttribute(
            pattern="string",
            defaultValue="admin,admin",
            description="default user")
    public static final ODItem SERVER_COMMON_USER = new ODItem("SERVER_COMMON_USER");
    
    @ODItemAttribute(
            pattern="string",
            defaultValue="bond0",
            description="default network adapter name")
    public static final ODItem SERVER_COMMON_NETWORK = new ODItem("SERVER_COMMON_NETWORK");
    
    @ODItemAttribute(
            pattern="ip",
            defaultValue="0.0.0.0",
            description="default network adapter name")
    public static final ODItem SERVER_IP = new ODItem("SERVER_IP");
    
    @ODItemAttribute(
            pattern="username,password",
            defaultValue="admin,admin",
            description="ignore 'server.common.user'")
    public static final ODItem SERVER_USER = new ODItem("SERVER_USER");
    
    @ODItemAttribute(
            pattern="string",
            defaultValue="bond0",
            description="ignore 'server.common.network'")
    public static final ODItem SERVER_NETWORK = new ODItem("SERVER_NETWORK");
    
    public static final ODItem SERVER_CLOSE = new ODItem("SERVER_CLOSE");
    
    public static final ODItem H_SERVERS = new ODItem("H_SERVERS");
    //add upsport/msport/csport
    public static final ODItem H_SERVER_UPS_PORT_P = new ODItem("H_SERVER_UPS_PORT_P");
    public static final ODItem H_SERVER_UPS_PORT_M = new ODItem("H_SERVER_UPS_PORT_M");
    public static final ODItem H_SERVER_MS_PORT_P = new ODItem("H_SERVER_MS_PORT_P");
    public static final ODItem H_SERVER_MS_PORT_Z = new ODItem("H_SERVER_MS_PORT_Z");
//    public static final ODItem H_SERVER_LMS_PORT_Z = new ODItem("H_SERVER_LMS_PORT_Z");
    public static final ODItem H_SERVER_CS_PORT_P = new ODItem("H_SERVER_CS_PORT_P");
    public static final ODItem H_SERVER_CS_PORT_N = new ODItem("H_SERVER_CS_PORT_N");
    //end
    public static final ODItem H_SERVER_COMMON_USERNAME = new ODItem("H_SERVER_COMMON_USERNAME");
    public static final ODItem H_SERVER_COMMON_PASSWORD = new ODItem("H_SERVER_COMMON_PASSWORD");
    public static final ODItem H_SERVER_USERNAME = new ODItem("H_SERVER_USERNAME");
    public static final ODItem H_SERVER_PASSWORD = new ODItem("H_SERVER_PASSWORD");
    
    // ------------------------------------------------------- start
    @ODItemAttribute(
    		nullable=true,
            pattern="[none|data|log|etc]",
            defaultValue="data|log|etc", 
            description="clear directory of './data','./log','./etc', clear all in default")
    public static final ODItem START_WIPE = new ODItem("START_WIPE");
  
    @ODItemAttribute(
    		pattern="number",
    		defaultValue="3",
    		description="assign the count of rootserver")
    public static final ODItem START_RSCOUNT = new ODItem("START_RSCOUNT");
    
    @ODItemAttribute(
    		pattern="number",
    		defaultValue="3",
    		description="assign the count of updateserver")
    public static final ODItem START_UPSCOUNT = new ODItem("START_UPSCOUNT");
       
    @ODItemAttribute(
    		pattern="string",
    		defaultValue="0.0.0.0",
            description="assign the master rootserver ip")
    public static final ODItem START_MRS = new ODItem("START_MRS");
    
    @ODItemAttribute(
    		pattern="string",
    		defaultValue="0.0.0.0",
            description="assign the master updateserver ip")
    public static final ODItem START_MUPS = new ODItem("START_MUPS");
    
    @ODItemAttribute(
    		pattern="ip,[rs|ups|ms|lms|cs],String,String",
            executor=ODStartActionExecutor.class,
            defaultValue="0.0.0.0, rs|ups|ms|cs,1,0.5")
    public static final ODItem START_SERVER = new ODItem("START_SERVER");
    
    
    
    // ------------------------------------------------------- public

    public ODItem(String name) {
        if(name != null) {
            this.name = name.replace("_", ".").toLowerCase(); //转换名称
        }
        boolean isDuplicate = false; // 检查重复
        for(ODItem item: values) {
            if(item.name.equals(this.name)) {
                isDuplicate = true;
                ODLogger.error("item duplicate! '" + this.name + "' is existed",
                        (new Throwable()).getStackTrace());
                break;
            }
        }
        if(!isDuplicate) {
            values.add(this); // 添加到列表
        }
    }

    /**
     * toString
     */
    public String toString() {
        return name;
    }
    
    /**
     * item所属的section
     * @return 不会为null
     */
    public String getSectionName() {
        String tmp = name;
        if(isHidden()) {
            tmp = tmp.substring(2);
        }
        String[] names = ODUtil.split(tmp, "\\.");
        return names[0];
    }
    
    /**
     * 去掉h前缀和section前缀
     * @return 可能为null
     */
    public String getShorName() {
        String tmp = name;
        if(isHidden()) {
            tmp = tmp.substring(2);
        }
        int pos = tmp.indexOf(".");
        if(pos > 0 && pos < tmp.length() - 1) {
            return tmp.substring(pos + 1);
        } else {
            ODLogger.error((new Throwable()).getStackTrace());
        }
        return null;
    }
    
    /**
     * 获取配置项映射到task属性的名称<br>
     * 规则：去掉h前缀、section前缀和"."符号, 剩余字母转换成驼峰式命名<br>
     *   如：server.common.network 对应的属性名称是 commonNetwork<br>
     * task属性必须以此命名才能自动映射赋值<br>
     * @return 可能为null
     */
    public String getPropertyName() {
        String tmp = getShorName();
        String[] names = ODUtil.split(tmp, "\\.");
        if(names.length > 0) {
            tmp = "";
            boolean isFirst = true;
            for(String n: names) {
                if(!isFirst) {
                    if(n.length() > 1) {
                        n = n.substring(0, 1).toUpperCase() + n.substring(1);
                    } else {
                        n = n.toUpperCase();
                    }
                }
                isFirst = false;
                tmp += n;
            }
        }
        return tmp;
    }

    /**
     * 是否在配置文件中可见
     * 例如：A是配置项成员，但其isHidden=true，则在配置文件不能给其赋值:a = 值
     * "H_"前缀表示hidden
     * 默认可见
     */
    public boolean isHidden() {
        return name.startsWith("h.");
    }
    
    /**
     * 单独识别"H_ITEM"类型的字符串
     * @param item 配置项字符串
     * @return 配置项
     */
    static ODItem valueOfWithH(String item) {
        if(item.startsWith("H_")) {
            item = item.replace("_", ".").toLowerCase();
        }
        return valueOf(item);
    }
        
    /**
     * 转换为ODItem
     * @param item 配置项字符串
     * @return ODItem
     */
    public static ODItem valueOf(String item) {
        if(item != null) {
            item = item.trim();
            for(ODItem it: ODItem.values) {
                if(it.toString().equals(item)) {
                    return it;
                } 
            }
        }
        return UNKNOWN;
    }
    
    /**
     * 转换为ODItem
     * @param name 配置项的全写, 如:START
     * @return
     */
    public static ODItem valueOfFullName(String name) {
        if(name != null) {
            return valueOf(name.replace("_", ".").toLowerCase());
        }
        return UNKNOWN;
    }
    
    /**
     * 获取section所属的普通配置项列表<br>
     * 不包括特殊的配置项: name, close, hidden
     * @param sectionName
     * @return 不会为null
     */
    public static List<ODItem> getItem(String sectionName) {
        List<ODItem> list = new ArrayList<>();
        for(ODItem it: ODItem.values) {
            if(it.getSectionName().equals(sectionName) 
                    && !it.isHidden() && !it.isName() && !it.isClose()) {
                list.add(it);
            } 
        }
        return list;
    }
    
    /**
     * 配置项是否可为空
     * @return boolean
     */
    public boolean isNullable() {
        return attribute.nullable;
    }
    
    /**
     * 赋值格式
     * @return Pattern 不会为null
     */
    public ODItemValuePattern getValuePattern() {
        return ODItemValuePattern.toValuePattern(this, attribute.pattern);
    }
    
    /**
     * 配置的类型
     * @return COMMON, ACTION
     */
    public ODItemType getType() {
        ODItemType type = ODItemType.COMMON;
        if(attribute.executor != ODActionExecutor.class) {
            type = ODItemType.ACTION;
        }
        return type;
    }
    
    public Class<? extends ODActionExecutor> getExecutor() {
        return attribute.executor;
    }
    
    /**
     * 是否是common类型的配置项
     * @return
     */
    public boolean isCommon() {
        return attribute.executor == ODActionExecutor.class;
    }
    
    /**
     * 是否是action类型的配置项
     * @return
     */
    public boolean isAction() {
        return attribute.executor != ODActionExecutor.class;
    }

    /**
     * 赋值的数目
     * 如: item = a,b 其值数目为2
     * @return 可能为0
     */
    public int getValueCount() {
        String pattern = attribute.pattern;
        if (pattern != null) {
            return pattern.split(",").length;
        }
        return 0;
    }

    /**
     * 默认值
     * @return 对配置原样返回
     */
    public String getDefaultValue() {
        return attribute.defaultValue;
    }

    /**
     * 说明
     * @return 对配置原样返回
     */
    public String getDescription() {
        String desc = attribute.description;
        if(this == ODItem.SERVER_VERSION) { // 对OB版本号特殊处理
            desc += "registered version: ";
            List<String> versions = ODDeployer.getInstance().getParameterCeneratorList();
            if(versions != null) {
                for(String v: versions) {
                    desc += v + " ";
                }
            }
        }
        return desc;
    }
    
    /**
     * 是否是"*.close"类型，表示section结束符
     * @return boolean
     */
    public boolean isClose() {
        return ODUtil.isCloseItem(name);
    }
    
    /**
     * 是否是"*.name"类型，表示section开始符
     * @return boolean
     */
    public boolean isName() {
        return ODUtil.isNameItem(name);
    }

    /**
     * 获取某一类配置项的默认值, 用于生成配置文件
     * @param sectionName 配置项类别，Secion的名称
     * @return first = item name, second = default value
     */
    public static List<Pair<String, String>> getDefaultValueList(String sectionName) {
        List<Pair<String, String>> ret = new ArrayList<>();
        if(sectionName != null) {
            for(ODItem item: values) { // 将section.name提前
                if(item.toString().startsWith(sectionName)) {
                    if(item.isName()) {
                        ret.add(new Pair<>(item.toString(), "null"));
                        break;
                    }
                }
            }
            for(ODItem item: values) {
                if(item.toString().startsWith(sectionName)) {
                    if(item != ODItem.SERVER_USER && item != ODItem.SERVER_NETWORK // 对server.user和server.network特殊处理
                            && !item.isName()) { // section.name已提前添加 
                        ret.add(new Pair<>(item.toString(), item.getDefaultValue()));
                        if(item == ODItem.SERVER_IP) {
                            ret.add(new Pair<>(item.toString(), "1.1.1.1"));
                            ret.add(new Pair<>(item.toString(), "2.2.2.2"));
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * 获取某一类配置项的赋值格式和说明, 用于生成配置文件
     * @param sectionName 配置项类别，Secion的名称
     * @return first = item name, second = value pattern, description
     */
    public static List<Pair<String, Pair<String, String>>> getValuePatternAndDescriptionList(String sectionName) {
        List<Pair<String, Pair<String, String>>> ret = new ArrayList<>();
        if(sectionName != null) {
            for(ODItem item: values) { // 将section.name提前
                if(item.toString().startsWith(sectionName)) {
                    if(item.isName()) {
                        ret.add(new Pair<>(item.toString(),
                                new Pair<>(item.getValuePattern().toString(), item.getDescription())));
                        break;
                    }
                }
            }
            for(ODItem item: values) {
                if(item.toString().startsWith(sectionName) && !item.isName()) {
                    ret.add(new Pair<>(item.toString(),
                            new Pair<>(item.getValuePattern().toString(), item.getDescription())));
                }
            }
        }
        return ret;
    }
    
    /**
     * 设置属性值
     * @param nullable
     * @param pattern
     * @param defaultValue
     * @param description
     */
    public void setAttribute(boolean nullable, String pattern, Class<? extends ODActionExecutor> executor,
            String defaultValue, String description) {
        attribute.nullable = nullable;
        attribute.pattern = pattern;
        attribute.executor = executor;
        attribute.defaultValue = defaultValue;
        attribute.description = description;
    }

    /** 配置项属性 */
    private class Attribute {
        
        /** 是否可为空 */
        private boolean nullable = false;
        
        /** 赋值的格式, 多个值时用","号分隔 */
        private String pattern = "string";
        
        /** 配置项的类型 */
        Class<? extends ODActionExecutor> executor = ODActionExecutor.class;
        
        /** 默认值 */
        private String defaultValue = "";
        
        /** 说明 */
        private String description = "";
        
    }
}
