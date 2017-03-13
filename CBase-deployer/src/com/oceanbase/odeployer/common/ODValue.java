package com.oceanbase.odeployer.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 配置项的输入值
 * <p>值类型有:</p>
 * <blockquote><pre>
 * 1. STRING 字符串, 默认类型
 * 2. NUMBER 整数
 * 3. FLOAT 小数
 * 4. BOOLEAN 布尔
 * 5. OPTION 多选列表, 如[rs|ups]
 * 6. SELECT 单先列表,如[rs/ups]
 * 7. IP IP地址, 会检验主机是否已配置
 * 8. SECTION 配置单元所属任务的集合
 * 9. TASK 单个任务
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/4/11
 * @since OD1.0
 */
public class ODValue {

    public enum ODValueType {
        STRING, // String
        NUMBER, // int
        FLOAT,  // float
        BOOLEAN,// boolean
        OPTION, // 以"[]"为标记, 多个值以"|"分隔, 如:[rs|ups], 表示可选其中0或多个
        SELECT, // 以"[]"为标记, 多个值以空格"/"分隔, 如:[rs/ups], 表示只可且必须选其中1个, 若只有一个选项, 默认为OPTION类型
        IP,     // ODServer
        SECTION,// Map<ODTask>
        TASK,   // ODTask, <sectionName, taskName>
    }

    private ODValueType valueType = ODValueType.STRING;

    private Value value = new Value();
    
    private String pattern;

    /** 获取实际类型 */
    public Object getValue() {
        switch (valueType) {
            case NUMBER:
                return value.numberValue;
            case FLOAT:
                return value.floatValue;
            case BOOLEAN:
                return value.booleanValue;
            case OPTION:
                return value.optionValue;
            case SELECT:
                if(value.selectValue == null || value.selectValue.size() > 1) {
                    return value.selectValue; // 表示pattern的值, 返回数组
                }
                return value.selectValue.get(0); // 表示输入值或只有一个值的pattern, 返回单个字符串
            case IP:
                return value.ipValue;
            case SECTION:
                return value.sectionValue;
            case TASK:
                return value.taskValue;
            default:
                return value.stringValue;
        }
    }

    /**
     * 配置项值类型
     * @param pattern String
     * @return 值类型
     */
    public static ODValueType toValueType(String pattern) {
        pattern = pattern.replace("<","").replace(">","").trim();
        if(pattern.equalsIgnoreCase(ODValueType.NUMBER.toString())) {
            return ODValueType.NUMBER;
        } else if(pattern.equalsIgnoreCase(ODValueType.FLOAT.toString())) {
            return ODValueType.FLOAT;
        } else if(pattern.equalsIgnoreCase(ODValueType.BOOLEAN.toString())) {
            return ODValueType.BOOLEAN;
        } else if(pattern.startsWith("[") && pattern.endsWith("]")) {
            if(pattern.contains("/")) {
                return ODValueType.SELECT;
            }
            return ODValueType.OPTION; // 列表默认为OPTION类型
        } else if(pattern.equalsIgnoreCase(ODValueType.IP.toString())) {
            return ODValueType.IP;
        } else if(pattern.equalsIgnoreCase(ODValueType.SECTION.toString())) {
            return ODValueType.SECTION;
        } else if(pattern.equalsIgnoreCase(ODValueType.TASK.toString())) {
            return ODValueType.TASK;
        }
        // 默认为String类型
        return ODValueType.STRING;
    }

    /** 格式化 */
    public String toString() {
        switch (valueType) {
            case NUMBER:
                return String.valueOf(value.numberValue);
            case FLOAT:
                return String.valueOf(value.floatValue);
            case BOOLEAN:
                return String.valueOf(value.booleanValue);
            case OPTION:
                if(value != null && value.optionValue !=null) {
                    String str = "";
                    for (String s : value.optionValue) {
                        str += s + "|";
                    }
                    str = ODUtil.removeLastChar(str);
                    return str;
                }
                return null;
            case SELECT:
                if(value != null && value.selectValue !=null) {
                    String str = "";
                    for (String s : value.selectValue) {
                        str += s + "/";
                    }
                    str = ODUtil.removeLastChar(str);
                    return str;
                }
                return null;
            case IP:
                return value.ipValue;
            case SECTION:
                return value.sectionValue;
            case TASK:
                return value.taskValue.first + " " + value.taskValue.second;
            default:
                return value.stringValue;
        }
    }

    /**
     * 识别类型模式
     * @param pattern String
     * @return 值
     */
    public static ODValue patternToValue(String pattern) {
        ODValue value = new ODValue();
        value.pattern = pattern;
        value.valueType = toValueType(pattern);
        if(value.valueType == ODValueType.OPTION) {
            value.value.optionValue = new ArrayList<>();
            pattern = pattern.replace("[", "").replace("]", "");
            String[] types = ODUtil.split(pattern, "\\|");
            Collections.addAll(value.value.optionValue, types);
        } else if(value.valueType == ODValueType.SELECT) {
            value.value.selectValue = new ArrayList<>();
            pattern = pattern.replace("[", "").replace("]", "");
            String[] types = ODUtil.split(pattern, "/");
            Collections.addAll(value.value.selectValue, types);
        }
        //ODLogger.debug("pattern=" + pattern + ",type=" + value.getValueType(), (new Throwable()).getStackTrace());
        return value;
    }

    /**
     * 转换值类型
     * @param value 配置项输入值
     * @param pattern 要求的类型
     * @return 转换类型后的值
     * @throws Exception 若为List类型，但值不符合要求，抛出自定义的异常
     */
    @SuppressWarnings("unchecked")
    public static ODValue castValue(String value, ODValue pattern) throws Exception {
        value = value.trim();
        ODValue odValue = new ODValue();
        odValue.valueType = pattern.valueType;
        switch (odValue.valueType) {
            case NUMBER:
                odValue.value.numberValue = Integer.parseInt(value);
                break;
            case FLOAT:
                odValue.value.floatValue = Float.parseFloat(value);
                break;
            case BOOLEAN:
            	if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            		odValue.value.booleanValue = Boolean.parseBoolean(value);
            	} else {
            		throw new Exception("The value of '" + value + "' is undefined, expect 'true' or 'false'");
            	}
                break;
            case OPTION:
                List<String> types = (List<String>)pattern.getValue();
                odValue.value.optionValue = new ArrayList<>();
                String[] values = ODUtil.split(value, "\\|");
                for(String v: values) {
                    if(ODUtil.isExist(types, v)) { // 是否符合可选集合中的一项
                        odValue.value.optionValue.add(v.toLowerCase());
                    } else {
                        throw new Exception("The value of '" + v + "' is undefined, expect '<" + pattern.toString() + ">'");
                    }
                }
                break;
            case SELECT:
                List<String> selectTypes;
                Object selectValue = pattern.getValue();
                if(selectValue instanceof List) { // List
                    selectTypes = (List<String>) selectValue;
                } else { // String
                    selectTypes = new ArrayList<>();
                    selectTypes.add((String) selectValue);
                }
                odValue.value.selectValue = new ArrayList<>();
                if(value.length() == 0 || value.contains("/")) {
                    throw new Exception("expect ONE option from '<" + pattern.toString() + ">'");
                } else if (!ODUtil.isExist(selectTypes, value)) {
                    throw new Exception("The value of '" + value + "' is undefined, expect one of '<" + pattern.toString() + ">'");
                } else { // 是否符合可选集合中的一项
                    odValue.value.selectValue.add(value.toLowerCase());
                }
                break;
            case IP:
                odValue.value.ipValue = value;
                break;
            case SECTION:
                odValue.value.sectionValue = value;
                break;
            case TASK:
                String[] tasks = ODUtil.split(value, " ");
                if(tasks.length == 2) {
                    odValue.value.taskValue = new Pair<String, String>(tasks[0], tasks[1]);
                } else {
                    throw new Exception("The value of '" + value + "' is undefined, expect '<task(sectionName taskName)>'");
                }
                break;
            default:
                odValue.value.stringValue = value;
                break;
        }
        return odValue;
    }

    public String getPattern() {
        return pattern;
    }
    
    public ODValueType getValueType() {
        return valueType;
    }

    /** 实际值 */
    private class Value {

        private String stringValue;

        private int numberValue;

        private float floatValue;

        private boolean booleanValue;

        private List<String> optionValue;
        
        private List<String> selectValue; // 模式可能有多个值, 但输入值只能一个, 保存在[0]位置

        private String ipValue;
        
        private String sectionValue;
        
        private Pair<String, String> taskValue; // <sectionName, taskName>
    }
}
