package com.oceanbase.odeployer.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.command.ODCommandHandler;
import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODConfiguration;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.common.ODItemAdder;
import com.oceanbase.odeployer.common.ODItemValuePattern;
import com.oceanbase.odeployer.common.ODSectionType;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.common.ODValue;
import com.oceanbase.odeployer.task.ODTask;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 通用的解析器
 * <p>自定义的section的类型应符合下面3类之一:</p>
 * <blockquote><pre>
 * 1. type = SECTION0  
 *              a.commonItem1 = value
 *              a.commonItem2 = value
 *              a.commonItem3 = value
 * -----------------------------------
 * 2. type = SECTION1  
 *              a.nameItem   = value
 *              a.commonItem1 = value
 *              a.commonItem2 = value
 *              a.closeItem
 * -----------------------------------
 * 3. type = SECTION2
 *              a.nameItem   = value
 *              [a.commonItem1 = value](可选项)
 *              [a.commonItem2 = value](可选项)
 *              a.actionItem1 = value
 *              a.actionItem2 = value
 *              a.closeItem
 * 其中:
 * (1) nameItem 配置项命名如"*.name", 表示开始符, 由解析器自动生成
 * (2) closeItem 配置项命名如"*.close",表示结束符, 由解析器自动生成
 * (3) actionItem格式必须为: &lt;ip[, ..., number]&gt, 
 *                      如: &lt;ip&gt, &lt;ip,number&gt, &lt;ip,[rs|ups],number&gt;, &lt;ip,string,string,number&gt;
 *                      且ip表示绑定的主机, number表示时间间隔
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/4/6
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public abstract class ODCommonSectionParser extends ODItemAdder implements ODISectionParser {

    /** 类型, 0, 1, 2 */
    private ODSectionType type;

    /** 标记开始的配置项 */
    private ODItem nameItem;

    /** 标记结束的配置项 */
    private ODItem closeItem;
    
    /** 用于保存任务集合的配置项 */
    private ODItem tasksItem;

    /** 映射为任务通用属性的配置项 */
    protected List<ODItemValue> commonItems;

    /** 映射为任务中动作的配置项 */
    protected List<ODItemValue> actionItems;

    /** 自定义任务类 */
    private Class<? extends ODTask> taskClass;

    /** 解析后的数据 */
    protected CommonSection commonSection = new CommonSection();
    
    private boolean hasActionItem = false;

    /** 是否初始化 */
    private boolean isInit;
    
    private ODDeployer deployer = ODDeployer.getInstance();
    
    /* 本次执行命令绑定的section */
    private String commandSection;
    
    /**
     * @param type        SECTION0, SECTION1, SECTION2
     * @param taskClass   ODTask
     */
    protected void init(ODSectionType type, Class<? extends ODTask> taskClass) {
        if(!isInit) {       	
            List<ODItem> itemList = ODItem.getItem(getSectionName());
            boolean hasCommonItem = false;
            for(ODItem item: itemList) { 
                if(!hasCommonItem && item.isCommon()) {
                    hasCommonItem = true;
                }
                if(!hasActionItem && item.isAction()) {
                    hasActionItem = true;
                }
            }
            if((type == ODSectionType.SECTION0 || type == ODSectionType.SECTION1) && !hasCommonItem) {
                ODLogger.error("'" + getSectionName() + "'(type=SECTION0 or SECTION1) need common items!", 
                		(new Throwable()).getStackTrace());
            } else if(type == ODSectionType.SECTION2 && !hasActionItem) {
                ODLogger.error("'" + getSectionName() + "'(type=SECTION2) need action items!", (new Throwable()).getStackTrace());
            } else {
                this.type = type;
                this.taskClass = taskClass;
                String sectionName = getSectionName().toUpperCase();
                if(type == ODSectionType.SECTION1 || type == ODSectionType.SECTION2) { // 只有type=1或2的section才有这三个特殊的配置项
                    this.nameItem = new ODItem(sectionName + "_NAME");
                    this.closeItem = new ODItem(sectionName + "_CLOSE");
                }
                this.tasksItem = new ODItem("H_" + sectionName);
                    
                // 以section为中介，绑定command和task
                ODCommandHandler.addSectionBindingTask(getSectionName(), tasksItem);
                
                this.commonItems = new ArrayList<>();
                for(ODItem item: itemList) {
                    if(item.isCommon()) {
                        this.commonItems.add(new ODItemValue(item));
                    }
                }
                if(type == ODSectionType.SECTION2) { // 只有SECTION2类型的section才有action配置项
                	if(this.actionItems == null) {
                		this.actionItems = new ArrayList<>();
                	}
                    for(ODItem item: itemList) {
                        if(item.isAction()) {
                            this.actionItems.add(new ODItemValue(item));
                        }
                    }
                }
                isInit = true;
            }
        } else {
            ODLogger.error("ODCommonSectionParser(" + getSectionName() + ") is initialized!", (new Throwable()).getStackTrace());
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "ODCommonSectionParser is initialized", false);
        }
    }

    /**
     * 不建议重载该方法
     * 默认不对配置项值进行切分或转换类型
     * 在addToConfigurations()方法中对配置项进行值的切分与转换
     * @param cmd 子命令
     * @param item 配置项
     * @param value 值
     * @throws Exception
     */
    @Override
    public void parseConfigureItems(ODCommand cmd, ODItem item, String value) throws Exception {
        if(isInit) {
            // 强制检查参数正确性
            if(type == ODSectionType.SECTION0 && (taskClass == null ||commonItems == null)) {
                ODLogger.error("section type = 0, but task class or commonItems is null!",
                        (new Throwable()).getStackTrace());
            } else if(type == ODSectionType.SECTION1 && (taskClass == null ||
                    nameItem == null || closeItem == null || tasksItem == null || commonItems == null)) {
                ODLogger.error("section type = 1, but task class" +
                        " or commonItems is null!", (new Throwable()).getStackTrace());
            } else if(type == ODSectionType.SECTION2 && (taskClass == null || actionItems == null)) {
                ODLogger.error("section type = 2, but task class" +
                                " or actionItems is null!", (new Throwable()).getStackTrace());
            } else {
            	if(commandSection == null && cmd.isBindingSection()) {
            		commandSection = deployer.getSectionParser(cmd.getBindingSection()).getSectionName();
            	}
                // ----------------------------------------------------------- type = 0
                if(type == ODSectionType.SECTION0) {
                    if(commonSection.task == null) {
                        Constructor<?> constructor = taskClass.getConstructor(String.class);
                        // 若type=0, 只有一个task, 则以section的名称命名task
                        commonSection.task = (ODTask) constructor.newInstance(getSectionName());
                        commonSection.beginLineNumber = ODConfiguration.getLineNumber();
                    }
                    // common类型的配置项
                    parserCommonItems(item, value);
                    // 用于输出日志
                    ODConfiguration.addItem(getSectionName(), getSectionName(), item, value);
                // ----------------------------------------------------------- type = 1,2
                } else if(type == ODSectionType.SECTION1 || type == ODSectionType.SECTION2) {
                    if(item == nameItem) {
                        if(commonSection.task == null) {
                            itemSet.clear(); // 清除上一个task
                            
                            Constructor<?> constructor = taskClass.getConstructor(String.class);
                            commonSection.task = (ODTask) constructor.newInstance(value);
                            commonSection.beginLineNumber = ODConfiguration.getLineNumber();
                        } else {
                            ODConfiguration.printError(ODError.ERROR_MISS_ITEM, closeItem, nameItem.toString(), 
                            		commonSection.beginLineNumber);
                        }
                    } else {
                        if(commonSection.task != null) {
                            if(item == closeItem) {
                            	// 每个task结束时，检查common类型的配置项是否为空
                            	if(commonSection.task != null) {
                            		checkCommonItems();
                            	}
                                commonSection.taskMap.put(commonSection.task.getTaskName(), commonSection.task);
                                commonSection.task = null;
                            } else {
                                parserCommonItems(item, value);
                                if(type == ODSectionType.SECTION2) {
                                	parserActionItems(item, value);                                   
                                }
                            }
                        } else {
                            ODConfiguration.printError(ODError.ERROR_MISS_ITEM, nameItem, item.toString());
                        }
                    }
                    if(commonSection.task != null) {
                        // 用于输出日志
                        ODConfiguration.addItem(getSectionName(), commonSection.task.getTaskName(), item, value);
                    }
                } else {
                    ODLogger.error("ODCommonSectionParser type '" + type + "' is undefined!",
                            (new Throwable()).getStackTrace());
                }
            }
        } else {
            ODLogger.error("ODCommonSectionParser(" + getSectionName() + ") has not been initialized!", 
            		(new Throwable()).getStackTrace());
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "ODCommonSectionParser has not been initialized", false);
        }
    }

    @Override
    public void before() {
        //do nothing
    }

    /**
     * 默认配置项的添加方式
     * 默认不对配置项值进行切分或转换类型
     * 根据需要子类可重载该方法，并对异常单独处理
     * 若需要重载该方法，可从commonItems和actionItems获取解析后的配置项值
     */
    @Override
    public void after() {
        if(isInit) {
            if(type == ODSectionType.SECTION0) {
            	if((commandSection != null && commandSection.equals(getSectionName()))
            			|| (commonSection.task != null)) {
            		checkCommonItems();
            	}
                if(commonSection.task == null) {
                	if(commandSection != null && commandSection.equals(getSectionName())) {
                		ODConfiguration.printError(ODError.ERROR_EXCEPTION, "No item is defined!", false);
                	}
                } else {
                    // 把惟一的task添加到map
                    commonSection.taskMap.put(commonSection.task.getTaskName(), commonSection.task);
                }
            } else if(type == ODSectionType.SECTION1 || type == ODSectionType.SECTION2) {
                if(commonSection.task != null) { // 最后一个task没有close
                    ODConfiguration.printError(ODError.ERROR_MISS_ITEM, closeItem, nameItem.toString(), commonSection.beginLineNumber);
                }
            }
            // 保存任务集合Map
            ODConfiguration.addConfigureItem(tasksItem, commonSection.taskMap);
        } else {
            ODLogger.error("ODCommonSectionParser(" + getSectionName() + ") has not been initialized!", (new Throwable()).getStackTrace());
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "ODCommonSectionParser has not been initialized", false);
        }
    }
    
    public ODItem getItem() {
        return tasksItem;
    }
    
    protected void addActionItem(ODItem actionItem) {
    	if(this.actionItems == null) {
    		this.actionItems = new ArrayList<>();
    	}
    	if(!hasActionItem) {
    		hasActionItem = true;
    	}
    	this.actionItems.add(new ODItemValue(actionItem));
    }

    // ----------------------------------------------------------- private

    /**
     * 处理CommonItem
     * @param item 配置项
     * @param value 值
     * @throws Exception
     */
    private void parserCommonItems(ODItem item, String value) throws Exception {
        if(commonItems != null) {
            for(ODItemValue iv: commonItems) {
                if(iv.item == item) {
                    if(!isExist(item)) { // Common不可重复
                        iv.lineNumber = ODConfiguration.getLineNumber();
                        processValue(iv, value, false);
                        break;
                    }
                }
            }
        } else {
            if(type != ODSectionType.SECTION2) { // type = 2时, 可无common类型的配置项
                ODLogger.error("commonItems is null!", (new Throwable()).getStackTrace());
            }
        }
    }

    /**
     * 处理ActionItem
     * @param item 配置顶
     * @param value 值
     * @throws Exception
     */
    private void parserActionItems(ODItem item, String value) throws Exception {
        if(actionItems != null) {
            for(ODItemValue iv: actionItems) {
                if(iv.item == item) {
                    iv.lineNumber = ODConfiguration.getLineNumber();                   
                    processValue(iv, value, true);
                    break;
                }
            }
        } else {
            ODLogger.error("actionItems is null!", (new Throwable()).getStackTrace());
        }
    }

    /**
     * 处理配置项值，进行切分和类型转换等操作
     * @param itemValue 配置项值对
     * @param value 输入值
     * @param isActionItem 是否是action类的配置项
     */
    private void processValue(ODItemValue itemValue, String value, boolean isActionItem) throws Exception {
        ODItemValuePattern valuePattern = itemValue.item.getValuePattern();
        String[] values = value.split(",");
        if(valuePattern.valuePatterns.size() == values.length) { // 检查值数目是否一致
            int i = 0;
            List<Object> actionValue = new ArrayList<>();
            for(Pair<ODItem, ODValue> pattern: valuePattern.valuePatterns) {
                Object objValue = castValue(values[i], pattern.second);
                if(pattern.first == null) { // 若未引用其他配置项
                    if(isActionItem) { // action类型的配置项不需要保存到task
                        itemValue.values.add(objValue);
                        actionValue.add(objValue);
                    } else {
                        itemValue.values.add(objValue);
                        addCommonValue(itemValue.item, objValue);
                    }
                } else {
                    if(isActionItem) {
                        ODLogger.error("$H_ITEM is not allowed in the pattern of ACTION item!",
                                (new Throwable()).getStackTrace());
                    } else { // 保存引用的配置项
                        itemValue.hideValues.add(new Pair<>(pattern.first, objValue));
                        addCommonValue(pattern.first, objValue);
                    }
                }
                i++;
            }
            if(isActionItem) {
                // 以配置项名称命令Action
                ODAction action = new ODAction(itemValue.item, actionValue);               
                // 将Item映射到Action
                commonSection.task.addAction(action);
            }
        } else {
            ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, itemValue.item);
        }
    }

    /**
     * 增加对IP, SECTION, TASK类型的值单独处理
     * @param value 输入值
     * @param pattern 值类型格式
     * @return 值
     */
    @SuppressWarnings("unchecked")
    private Object castValue(String value, ODValue pattern) throws Exception {
        Object v = ODValue.castValue(value, pattern).getValue();
        String currentSection = getSectionName();
        String currentTask = commonSection.task.getTaskName();
        if(pattern.getValueType() == ODValue.ODValueType.IP) {
            String ip = v.toString();
            ODServer server = deployer.getConfigueServer(ip);
            if(server != null) {
                ODConfiguration.addIp(currentSection, currentTask, ip);
                return server;
            }
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "Server [ip=" + ip + "] is undefined");
            return new ODServer(null, null, null, null); // NULL server
        } else if(pattern.getValueType() == ODValue.ODValueType.SECTION) {
            String sectionName = v.toString();
            Map<String, ODTask> section = ODConfiguration.getSection(sectionName);
            if(section != null) {
                // 添加已使用IP列表
                ODConfiguration.addIpList(currentSection, currentTask, sectionName);
                // 用于输出日志
                ODConfiguration.addItemList(currentSection, currentTask, sectionName);
                return section;
            }
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "Section [name=" + sectionName + "] is undefined");
            return null;
        } else if(pattern.getValueType() == ODValue.ODValueType.TASK) {
            Pair<String, String> task = (Pair<String, String>) v;
            String sectionName = task.first;
            String taskName = task.second;
            Object obj = ODConfiguration.getTaskFromSection(sectionName, taskName);
            if(obj != null) {
                // 添加已使用IP列表
                ODConfiguration.addIpList(currentSection, currentTask, sectionName, taskName);
                // 用于输出日志
                ODConfiguration.addItemList(currentSection, currentTask, sectionName, taskName);
                return obj;
            } 
            ODConfiguration.printError(ODError.ERROR_EXCEPTION, "Task [section=" + sectionName + ", name=" + taskName + "] is undefined");
            return null;
        }
        return v;
    }

    /**
     * 保存common类型的配置项值到Task
     * @param item 配置项
     * @param value 值
     */
    private void addCommonValue(ODItem item, Object value) {
        if(commonSection.task != null) {
            String property = item.getPropertyName();
            if(property != null) {
                try {
                    Field f = taskClass.getDeclaredField(property);
                    f.setAccessible(true);
                    f.set(commonSection.task, value);
                } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
                    // do nothing
                } catch (IllegalArgumentException e) {
                    if(ODDeployer.DEBUG) {
                        e.printStackTrace();
                    }
                    ODLogger.error("[ERROR] Fail to set value, property='" + property + "', "
                            + "item='" + item + "', value='" + value + "'!", (new Throwable()).getStackTrace());
                }
            }
            commonSection.task.setValue(item, value);
        } else {
            ODLogger.error("commonSection.task is null!", (new Throwable()).getStackTrace());
        }
    }
    
    /**
     * 检查common类型的配置项是否为空
     */
    private void checkCommonItems() {
    	for(ODItemValue iv: commonItems) {
            if(!iv.item.isNullable() && !itemSet.contains(iv.item)) { // 若item不可为空
                if(type == ODSectionType.SECTION0 || commonSection.beginLineNumber < 0) {
                    ODConfiguration.printError(ODError.ERROR_MISS_ITEM,  iv.item, false);
                } else {
                    ODConfiguration.printError(ODError.ERROR_MISS_ITEM,  iv.item, nameItem.toString(), commonSection.beginLineNumber);
                }
                break;
            }
        }
    }

    protected class CommonSection {

        /** 当前处理的nameItem所在的行号 */
        private int beginLineNumber = -1;

        /** 任务的集合 */
        protected Map<String, ODTask> taskMap = new HashMap<>();

        /** 用于解析配置项组的临时变量 */
        private  ODTask task = null;

        public ODTask getTask() {
            return task;
        }

        public Map<String, ODTask> getTaskMap() {
            return taskMap;
        }
    }

    protected class ODItemValue {

        private ODItem item;

        /** 若引用了其他配置项, 以键值对ODItem, Object的形式保存到配置项集合中 */
        public List<Pair<ODItem, Object>> hideValues = new ArrayList<>();

        public List<Object> values = new ArrayList<>();

        /**
         * 配置项所在的行号
         * 用于自定义的错误提示
         */
        private int lineNumber;

        private ODItemValue(ODItem item) {
            this.item = item;
        }

        public ODItem getItem() {
            return item;
        }

        public int getLineNumber() {
            return lineNumber;
        }
        
        /** 用于调试 */
        @Override
        public String toString() {
            String str = "ODItemValue={item=" + item.toString() + ",hideValues=[";
            for(Pair<ODItem, Object> h: hideValues) {
                str += h.first.toString() + ":" + h.second.toString() + ",";
            }
            str = ODUtil.removeLastChar(str);
            str += "], values=[";
            for(Object v: values) {
                str += v.toString() + ",";
            }
            str = ODUtil.removeLastChar(str);
            str += "]}";
            return str;
        }
    }
}
