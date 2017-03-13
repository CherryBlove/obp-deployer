package com.oceanbase.odeployer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oceanbase.odeployer.annotation.ODCommandAttribute;
import com.oceanbase.odeployer.annotation.ODItemAttribute;
import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.command.ODCommandHandler;
import com.oceanbase.odeployer.command.ODICommandHandler;
import com.oceanbase.odeployer.common.ODConfiguration;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.common.ODOceanbase;
import com.oceanbase.odeployer.common.ODRegisterParameter;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.parser.ODISectionParser;
import com.oceanbase.odeployer.parser.ODOceanbaseSectionParser;
import com.oceanbase.odeployer.parser.ODServerSectionParser;
import com.oceanbase.odeployer.parser.ODStartSectionParser;
import com.oceanbase.odeployer.start.ODBaseParameterGenerator;
import com.oceanbase.odeployer.start.ODParameterGenerator;
import com.oceanbase.odeployer.start.ODPaxosParameterGenerator;
import com.oceanbase.odeployer.task.ODDeployTask;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODPrinter;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * Oceanbase Deployer
 * <p>单例模式</p>
 * <blockquote>ODDeployer deployer = ODDeployer.getInstance()</blockquote>
 * <p>使用方法：</p>
 * <blockquote><pre>
 * 1. init() 初始化，参数ODRegisterParameter可为<tt>null</tt>
 * 2. registerParameterGenerator() 注册自定义的OB附加参数启动器
 * 3. start() 直接传入main接收的参数数组
 * 4. destroy() 释放资源
 * </pre></blockquote>
 * <p>公开静态变量：</p>
 * <pre>
 * EBUG:        调试模式，输出异常详细信息
 * PRINT_SHELL: 输出远程命令及其执行返回结果详细信息
 * CONNECT:     是否连接远程主机, 用于调试
 * </pre>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public final class ODDeployer {

    /** debug 模式 */
    public static boolean DEBUG = false;

    /** 是否输出远程命令及其执行返回结果, 用于调试 */
    public static boolean PRINT_SHELL = false;
    
    /** 是否连接远程主机, 用于调试 */
    public static boolean CONNECT = true;

    /** 程序版本号 */
    private String version;

    /** 配置文件路径和名称, 默认: config/odeployer.cfg" */
    private static String CONFIGURE_FILE_NAME = "config/odeployer.cfg";

    /** 日志文件路径和名称, 默认: log/odeployer.log" */
    private static String LOG_FILE_NAME = "log/odeployer.log";

    /** 默认的子命令处理器 */
    private ODICommandHandler defaultCommandHandler;

    /** 自定义的子命令处理器 */
    private ODICommandHandler customCommandHandler;

    /** 扩展的Server */
    private Class<? extends ODServer> customServerClass;

    /** 配置文件解析器的集合 */
    private List<ODISectionParser> sectionParserList = new ArrayList<>();
    
    /** 启动命令参数生成器, 默认不实例化 */
    private Map<String, Class<? extends ODParameterGenerator>> parameterGenerators = new HashMap<>();
    
    /** 实例化当前使用的参数生成器 */
    private ODParameterGenerator parameterGenerator;
    
    /** 用于执行action */
    private ExecutorService actionExecutor = Executors.newFixedThreadPool(5);
    
    /** 配置文件加载器 */
    private ODConfiguration configuration;

    /** 管理各主机节点 */
    private ODOceanbase oceanbase;

    /** 是否初始化,保证仅初始化一次 */
    private boolean isInit;

    /** 是否开始运行,保证只start一次 */
    private boolean isStart;

    /** 单例 */
    private static ODDeployer DEPLOYER = new ODDeployer();

    /** 获取ODDeployer的惟一实例 */
    public static ODDeployer getInstance() {
        return DEPLOYER;
    }

    /** 禁止在外部实例化 */
    private ODDeployer() {}

    /**
     * 初始化
     * @param version 程序的名称或版本号，自定义
     * @param registerParameter 自定义类测试集合
     * @return 只能初始化一次
     */
    public ODError init(String version, ODRegisterParameter registerParameter) {
        ODError ret = ODError.SUCCESS;
        if(!isInit) {
            initLogger();
            if(registerParameter != null) {          	
                // 对返回错误不处理, 一次性检查所有注册类的正确性
                // ------------------------------------------- 1. 注册自定义的子命令
                if(registerParameter.customCommandClass != null) {
                    instantiate(registerParameter.customCommandClass);
                    loadCommandAttribute(registerParameter.customCommandClass);
                } else {
                    loadCommandAttribute(ODCommand.class);
                }
                // ------------------------------------------- 2. 注册自定义的配置项
                if(registerParameter.customItemClass != null) {
                    instantiate(registerParameter.customItemClass);
                    loadItemAttribute(registerParameter.customItemClass);
                } else {
                    loadItemAttribute(ODItem.class);//加载配置项的属性注解
                }
                // 注册默认的解析器
                //registerSectionParser(new ODDeploySectionParser());
                registerSectionParser(new ODOceanbaseSectionParser());
                registerSectionParser(new ODServerSectionParser());
                registerSectionParser(new ODStartSectionParser());
                // ------------------------------------------- 3. 注册自定义的子命令处理器
                if(registerParameter.customCommandHandlerClass != null) {
                    try {
                        customCommandHandler = registerParameter.customCommandHandlerClass.newInstance();
                    } catch (Exception e) {
                        ODLogger.error("instantiate '" + registerParameter.customCommandClass + "' fail!", (new Throwable()).getStackTrace());
                        ret = ODError.ERROR;
                    }
                }
                // ------------------------------------------- 4. 注册扩展的Server类
                customServerClass = registerParameter.customServerClass;              

                // ------------------------------------------- 5. 注册自定义的解析器
                if(registerParameter.customSectionParserList != null) {                 	
                    for(Class<? extends ODISectionParser> clazz: registerParameter.customSectionParserList) {
                        try {
                            registerSectionParser(clazz.newInstance());
                        } catch (Exception e) {
                            ODLogger.error("instantiate '" + clazz + "' fail!", (new Throwable()).getStackTrace());
                            ret = ODError.ERROR;
                        }
                    }
                }
            }
            if(ret.isSuccess()) {
                isInit = true; // 确保初始化成功
                this.version = version;
                defaultCommandHandler = new ODCommandHandler();

                //注册默认版本的启动命令参数生成器
                registerParameterGenerator("OB_PAXOS", ODPaxosParameterGenerator.class);
                registerParameterGenerator("OB_BASE", ODBaseParameterGenerator.class);
                configuration = new ODConfiguration();
            }
        } else {
            ODLogger.error("ODDeployer is initialized already!", (new Throwable()).getStackTrace());
            ret = ODError.ERROR;
        }
        return ret;
    }
    
    /**
     * 初始化系统日志类
     */
    public static void initLogger() {
        ODLogger.init(LOG_FILE_NAME);
    }

    /**
     * 用于调试, 模拟控制台输入
     * @param argvStr 输入命令
     */
    public ODError start(String argvStr) {
        if(argvStr != null && argvStr.trim().length() > 0) {
            String argvs[] = ODUtil.getArgumentList(argvStr);
            return start(argvs);
        } else {
            printUsage();
        }
        return ODError.ERROR;
    }

    /**
     * 运行部署器，只能调用一次
     * 命令行输入的参数进行识别, 参数输入错误仅在控制台输出
     * @param argvs 控制台输入
     */
    public ODError start(String argvs[]) {
        ODError ret = ODError.SUCCESS;
        if(isInit) {
            if(!isStart) { // 只能启动一次
                isStart = true;
                long startTime = System.currentTimeMillis(); // 用于计算总运行时间
                if(argvs.length > 0) { // 若有命令行参数
                    String command = argvs[0].trim(); // 第0个参数识别为子命令
                    if(ret.isSuccess()) {
                        ODCommand cmd = ODCommand.valueOf(command);
                        if(cmd != ODCommand.UNKNOWN) {
                            int num = cmd.getArgumentsNum();
                            List<String> argvsList = new ArrayList<>(); // 子命令的参数
                            if(num > 0) {
                                if(argvs.length > num) {
                                    for(int i = 0; i < num; i++) {
                                        argvsList.add(argvs[i + 1].trim()); // 从第1个参数开始识别为子命令的参数
                                    }
                                } else {
                                    ret = ODError.ERROR;
                                    System.out.println("[ERROR] miss argument '" + cmd.getArgumentNames() + "' for '" + command + "'!");
                                }
                            }
                            if(ret.isSuccess()) {
                                if(argvs.length == num + 2) { // 若指了定配置文件名称
                                    CONFIGURE_FILE_NAME = argvs[num + 1].trim();
                                }
                                if(argvs.length == num + 1 || argvs.length == num + 2) {
                                    cmd.setArgumentList(argvsList);                                   
                                    ret = handleCommand(cmd, ODUtil.toString(argvs)); // 处理子命令                           
                                    long useTime = System.currentTimeMillis() - startTime;
                                    if(ret != null && ret.isSuccess()) {
                                        ODLogger.log("Over! (" + ODUtil.parseTime(useTime) + ")");
                                    } else if(argvs.length > 0) {
                                        ODLogger.log("Exit with errors! (" + ODUtil.parseTime(useTime) + ")");
                                    }
                                } else {
                                    ret = ODError.ERROR;
                                    System.out.println("[ERROR] wrong command!");
                                }
                            }
                        } else {
                            ret = ODError.ERROR;
                            printUsage();
                            System.out.println("[ERROR] unknown command: " + command);
                        }
                    }
                } else {
                    printUsage();
                }
            } else {
                ret = ODError.ERROR;
                ODLogger.error("ODDeployer has started!", (new Throwable()).getStackTrace());
            }
        } else {
            ret = ODError.ERROR;
            ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        }
        return ret;
    }

    /**
     * 注册启动命令参数生成器, 应在start前注册
     * @param parameterGeneratorClass 附加启动参数生成器
     * @return 注册前需要初始化ODDeployer, 且不可重复注册
     */
    public ODError registerParameterGenerator(String oceanbaseVersion, Class<? extends ODParameterGenerator> parameterGeneratorClass) {
        ODError ret = ODError.SUCCESS;
        if(isInit) {
            if(!parameterGenerators.containsKey(oceanbaseVersion)) { // 检查重复
                parameterGenerators.put(oceanbaseVersion, parameterGeneratorClass);
            } else {
                ret = ODError.ERROR;
                ODLogger.error("ODIParameterGenerator regist fail! '" + oceanbaseVersion
                        + "' is existed", (new Throwable()).getStackTrace());
            }
        } else {
            ret = ODError.ERROR;
            ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    // ----------------------------------------------------------- getter

    
    /**
     * 获取启动命令参数生成器类
     * @param oceanbaseVersion Oceanbase的启动版本
     * @return 附加启动参数生成器, 可能为null
     */
    public Class<? extends ODParameterGenerator> getParameterGenerator(String oceanbaseVersion) {
        if(isInit) {
            return parameterGenerators.get(oceanbaseVersion);
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }
    
    /**
     * 参数生成器的名称列表
     * @return 可能为null
     */
    public List<String> getParameterCeneratorList() {
        if(isInit) {
            List<String> tmp = new ArrayList<>();
            Iterator<String> iter = parameterGenerators.keySet().iterator();
            while(iter.hasNext()) {
                tmp.add(iter.next());
            }
            return tmp;
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }
    
    /**
     * 根据类名获取解析器
     * @param sectionName section名称
     * @return 可能返回null
     */
    public ODISectionParser getSectionParser(Class<? extends ODISectionParser> clazz) {
        for(ODISectionParser parser: sectionParserList) {
            if(clazz == parser.getClass()) {
                return parser;
            }
        }
        return null;
    }
    
    /**
     * 根据名称获取解析器
     * @param sectionName section名称
     * @return 可能返回null
     */
    public ODISectionParser getSectionParser(String sectionName) {
        if(sectionName != null) {
            for(ODISectionParser parser: sectionParserList) {
                if(sectionName.equals(parser.getSectionName())) {
                    return parser;
                }
            }
        }
        return null;
    }

    /**
     * 解析器列表
     * @return 若未初始化,返回null
     */
    public List<ODISectionParser> getSectionParserList() {
        if(isInit) {
            return sectionParserList;
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }

    
    /**
     * 判断解析器是否已注册
     * @param sectionName section的名称
     * @return boolean
     */
    public boolean isSectionParserRegistered(String sectionName) {
        return (getSectionParser(sectionName) != null);
    }

    /**
     * 获取配置项集合
     * @return 确保在loadConf()后调用该方法，否则返回集合为null
     */
    public Map<ODItem, Object> getConfigurations() {
        if(isInit) {
            return ODConfiguration.getConfigurations();
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }

    /**
     * 获取Oceanbase集群
     * @return 确保在loadConf()后调用该方法，否则返回null
     */
    public ODOceanbase getOceanbase() {
        if(isInit) {
            return oceanbase;
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }
    
    /**
     * 是否已注册
     * @param oceanbaseVersion
     * @return
     */
    public boolean isParameterGeneratorRegistered(String oceanbaseVersion) {
        return parameterGenerators.containsKey(oceanbaseVersion);
    }
    
    /**
     * Oceanbase版本
     * @return
     */
    public String getOceanbaseVersion() {
        return (String) getConfigurations().get(ODItem.SERVER_VERSION);
    }
    
    /**
     * 当前使用的启动参数生成器
     * @return
     */
    public ODParameterGenerator getParameterGenerator() {
        if(parameterGenerator == null) {
            Class<? extends ODParameterGenerator> clazz = getParameterGenerator(getOceanbaseVersion());
            if(clazz != null) {
                try {
                    parameterGenerator = clazz.newInstance();
                } catch (Exception e) {
                    ODLogger.error("fail to create new instance of '" + clazz + "'!", (new Throwable()).getStackTrace());
                }
            }
        }
        return parameterGenerator;
    }

    /**
     * 在加载配置文件完成前获取主机节点信息
     * @param ip 主机IP
     * @return ODServer
     */
    public ODServer getConfigueServer(String ip) {
        if(sectionParserList != null) {
            for(ODISectionParser parser: sectionParserList) {
                if(parser instanceof ODServerSectionParser) {
                    List<ODServer> serverList = ((ODServerSectionParser)parser).getServerList();
                    if(serverList != null) {
                        for(ODServer s: serverList) {
                            if(s.ip.equals(ip)) {
                                return s;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 版本号
     * @return 若未初始化,返回null
     */
    public String  getVersion() {
        if(isInit) {
            return version;
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }

    /**
     * 扩展的Server类
     * @return 可能null
     */
    public Class<? extends ODServer> getCustomServerClass() {
        if(isInit) {
            return customServerClass;
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }
    
    /**
     * 主机列表
     * @return 可能null
     */
    @SuppressWarnings("unchecked")
    public List<Class<? extends ODServer>> getServerList() {
        if(isInit) {
            return (List<Class<? extends ODServer>>) getConfigurations().get(ODItem.H_SERVERS);
        }
        ODLogger.error("ODDeployer has not been initialized!", (new Throwable()).getStackTrace());
        return null;
    }
    
    /**
     * 获取oceanbase版本对应的task
     * @return
     */
    public ODDeployTask getDeployTask() {
    	return (ODDeployTask) ODConfiguration.getTaskFromSection("oceanbase", getOceanbaseVersion());
    }
    
    /**
     * 提交执行动作的任务
     * @param action
     */
    public void executeAction(Runnable action) {
        actionExecutor.submit(action);
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        if(oceanbase != null) {
            oceanbase.close();
        }
        actionExecutor.shutdownNow();
        ODLogger.destroy();
    }

    // ----------------------------------------------------------- private

    /**
     * 处理子命令
     * 错误信息输出到控制台和日志文件
     * @param cmd 子命令
     * @param argsStr 控制台输入
     * @return 捕获所有异常
     */
    @SuppressWarnings("unchecked")
    private ODError handleCommand(ODCommand cmd, String argsStr) {
        ODError ret = ODError.SUCCESS;
        try {
            if(cmd != ODCommand.CONFIGURATION) { // 生成配置模板文件是可不用加载配置文件，因为此时可能没有配置文件
                if(ret.isSuccess()) {              	
                    Pair<ODError, Map<ODItem, Object>> configRet = configuration.loadConf(CONFIGURE_FILE_NAME, cmd, sectionParserList);
                    ret = configRet.first;
                    Map<ODItem, Object> configurations = configRet.second;
                    if(ret.isError()) { // 解析配置文件出错	
                        String errorMessage = (String)configurations.get(ODItem.H_ERROR_MESSAGE);
                        ODLogger.log(errorMessage);
                    // System.out.print(errorMessage);
                    } else {
                        String sectionName = null;
                        if(cmd.getBindingSection() != null 
                                && cmd.getBindingSection() != ODISectionParser.class) {
                            sectionName = cmd.getBindingSection().newInstance().getSectionName();
                        }
                        String taskName = sectionName; // 对于0类section, task name即为section name
                        if(cmd.getArgumentsNum() > 0) { // 若子命令参数大于0, 说明指定了task name
                            taskName = cmd.getArgumentList().get(0);
                        } else {
                        	if(cmd == ODCommand.DEPLOY) { // 对deploy命令特殊处理
                        		// 把oceanbase的版本名称添加到命令行参数的最前面
                        		taskName = getOceanbaseVersion();
                        	}
                            cmd.getArgumentList().add(0, taskName); // 把默认的task name添加到命令行参数的最前面
                        }
                        List<ODServer> serverList = (List<ODServer>) configurations.get(ODItem.H_SERVERS);
                        if(!cmd.getNeedPreconnectAll() && !cmd.getNeedAliveMS()) {
                            // 实际使用的ip,避免不必要的连接 
                            Set<String> usedIpSet = new HashSet<>();
                            List<String> ipList = ODConfiguration.getIpList(sectionName, taskName);
                            if(ipList != null) {
                                usedIpSet.addAll(ipList);                             
                            }
                            for(int i = serverList.size() - 1; i >= 0; i--) {
                                if(!usedIpSet.contains(serverList.get(i).ip)) { // 若未真正使用
                                    serverList.remove(i);
                                }
                            }
                        }
                        // 程序输出头部信息 
                        List<Pair<String, String>> headMessageList = ODConfiguration.getItemList(sectionName, taskName);
                        //打印程序头
                        printHeader(argsStr, serverList, headMessageList);        
                        oceanbase = ODOceanbase.getInstance();
                        // 获取对应oceabanse版本的安装目录
                        ODDeployTask deployTask = getDeployTask();
                        if(deployTask == null) {
                        	ODLogger.log("oceanbase version '" + getOceanbaseVersion() + "' is undefined in the section of 'oceanbase'!");
                        	ret = ODError.ERROR;
                        } else {
                        	String oceanbaseDir = deployTask.getTargetDir();
                            String rsPort = String.valueOf(configurations.get(ODItem.SERVER_RS_PORT));
                            //upsport/msport/csport
                            String upsPortp = String.valueOf(configurations.get(ODItem.H_SERVER_UPS_PORT_P));
                            String upsPortm = String.valueOf(configurations.get(ODItem.H_SERVER_UPS_PORT_M));
                            String msPortp = String.valueOf(configurations.get(ODItem.H_SERVER_MS_PORT_P));
                            String msPortz = String.valueOf(configurations.get(ODItem.H_SERVER_MS_PORT_Z));
//                            String lmsPortz = String.valueOf(configurations.get(ODItem.H_SERVER_LMS_PORT_Z));
                            String csPortp = String.valueOf(configurations.get(ODItem.H_SERVER_CS_PORT_P));
                            String csPortn = String.valueOf(configurations.get(ODItem.H_SERVER_CS_PORT_N));
                            //end
                            
                            boolean noPreConnect = false;
                            //需要使用MS,但不预连接
                            if(cmd.getNeedAliveMS() && !cmd.getNeedPreconnectAll()) {
                            	noPreConnect = true;
                            }
                            // 初始化Oceanbase
                            ret = oceanbase.init(serverList, oceanbaseDir, rsPort, upsPortp, upsPortm, msPortp, msPortz, csPortp, csPortn, noPreConnect);
                        }
                    }
                }
            }
            if(ret.isSuccess()) {
                // 依次调用子命令处理器处理同一命令
                ret = defaultCommandHandler.handleCommand(cmd);
                if(ret != null && ret.isSuccess() && customCommandHandler != null) {
                    ret = customCommandHandler.handleCommand(cmd);
                }
            }
        } catch (Exception e) {
            if(DEBUG) {
                e.printStackTrace();
            }
            ODLogger.error(e.toString(), (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    /**
     * 注册解析器
     * @param parser 解析器
     * @return section的名称不可重复
     */
    private ODError registerSectionParser(ODISectionParser parser) {
        ODError ret = ODError.SUCCESS;
        if(parser != null) {
            boolean isDuplicate = false;
            for(ODISectionParser p: sectionParserList) {
                if(parser.getSectionName().equals(p.getSectionName())) {
                    isDuplicate = true;
                    ODLogger.error("the name of section '" + parser.getSectionName() + "' is duplicate!", (new Throwable()).getStackTrace());
                    break;
                }
            }
            if(!isDuplicate) {
                parser.init();
                sectionParserList.add(parser);
            }
        } else {
            ret = ODError.ERROR;
            ODLogger.error("parser is null!", (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    /**
     * 加载子命令的属性注解
     * @param clazz
     */
    private void loadCommandAttribute(Class<? extends ODCommand> clazz) {
        Field[] fields = clazz.getFields();
        for(Field f: fields) {
            if(f.isAnnotationPresent(ODCommandAttribute.class)) {
                ODCommandAttribute attribute = (ODCommandAttribute)f.getAnnotation(ODCommandAttribute.class);
                ODCommand cmd = ODCommand.valueOfFullName(f.getName());
                if(cmd != ODCommand.UNKNOWN) {
                    cmd.setAttribute(attribute.argument(), attribute.section(), 
                            attribute.connectAll(), attribute.connectMS(), attribute.description());
                }
            }
        }
    }
    
    /**
     * 加载配置项的属性注解
     * @param clazz
     */
    private void loadItemAttribute(Class<? extends ODItem> clazz) {
        Field[] fields = clazz.getFields();
        for(Field f: fields) {
            if(f.isAnnotationPresent(ODItemAttribute.class)) {
                ODItemAttribute attribute = (ODItemAttribute)f.getAnnotation(ODItemAttribute.class);
                ODItem item = ODItem.valueOfFullName(f.getName());
                if(item != ODItem.UNKNOWN) {
                    item.setAttribute(attribute.nullable(), attribute.pattern(), attribute.executor(),
                            attribute.defaultValue(), attribute.description());
                }
            }
        }
    }
    
    /**
     * 实例化组件类
     * @param clazz 待实例化的类
     * @return 需要实现指定的构造函数
     */
    private ODError instantiate(Class<?> clazz) {
        ODError ret = ODError.SUCCESS;
        if(clazz != null) {
            try {
                Constructor<?> constructor = clazz.getConstructor(String.class);
                constructor.newInstance("");
            } catch (Exception e) {
                ODLogger.error("'" + clazz.getName() + "'必须定义构造函数: public " +
                        clazz.getSimpleName() + "(String name){super(name);}", (new Throwable()).getStackTrace());
                ret = ODError.ERROR;
            }
        }
        return ret;
    }

    /**
     * 打印程序头部输出信息
     * @param argvStr 命令行输入
     * @param serverList 主机列表
     * @param headMessageList 使用到的配置项等信息
     */
    private void printHeader(String argvStr, List<ODServer> serverList, List<Pair<String, String>> headMessageList) {
        String border = "-";
        String lineBorder = "+";
        String version = getOceanbaseVersion();
        int leftWidth = 18;
        ODPrinter.printSingleLine(lineBorder);
        ODPrinter.printMessageCenter("OceanbaseDeployer V-" + getVersion(), border);
        ODPrinter.printMessageCenter(ODUtil.getSystemTime(), border);
        ODPrinter.printMessageCenter(argvStr, border);
        ODPrinter.printSingleLine(lineBorder);
        ODPrinter.printMessageLeft(ODUtil.formatString("oceanbase.version", leftWidth) + ": " + version, border);
        for(int i = 0; i < serverList.size(); i++) {
            ODPrinter.printMessageLeft(ODUtil.formatString("server[" + i + "]", leftWidth) + ": " + serverList.get(i), border);
        }
        if(headMessageList != null && headMessageList.size() > 0) {
            ODPrinter.printSingleLine(lineBorder);
            for(Pair<String, String> message: headMessageList) {
                if(message != null) {
                    ODPrinter.printMessageLeft(ODUtil.formatString(message.first, leftWidth) + ": " + message.second, border);
                } else { // 添加空行
                    ODPrinter.printSingleLine(lineBorder);
                }
            }
        }
        ODPrinter.printSingleLine(lineBorder);
    }

    /**
     * 打印程序版本和使用说明
     */
    private void printUsage() {
        StringBuilder sb = new StringBuilder();
        // -------------------------------------------- 1. version
        sb.append("Oceanbase Deployer").append(ODUtil.SEPARATOR);
        sb.append("Version: ").append(version).append(ODUtil.SEPARATOR);
        sb.append("Usage: java -jar program.jar <command> [arguments] [config file]").append(ODUtil.SEPARATOR);
        sb.append(ODUtil.charToString("-", 65)).append(ODUtil.SEPARATOR);
        // -------------------------------------------- 2. commands
        sb.append("<command>:").append(ODUtil.SEPARATOR);
        List<Pair<String, String>> cmdUsages = ODCommand.getUsage();
        int maxLen = 0; // 用于对齐
        for(Pair<String, String> usage: cmdUsages) {
            if(usage != null) {
                int len = usage.first.length();
                if(len > maxLen && len < 30) {
                    maxLen = len;
                }
            }
        }
        for(Pair<String, String> usage: cmdUsages) {
            if(usage != null) {
                sb.append("  ").append(ODUtil.formatString(usage.first, maxLen)).append(" : ").append(usage.second).append(ODUtil.SEPARATOR);
            } else { // 分隔线
                sb.append("  ").append(ODUtil.charToString("-", maxLen)).append(ODUtil.SEPARATOR);
            }
            
        }
        //sb.append("  ").append(ODUtil.charToString("-", maxLen)).append(ODUtil.SEPARATOR);
        // -------------------------------------------- 3. notes
        sb.append("<config file>: path/filename, '").append(CONFIGURE_FILE_NAME).append("' in default.").append(ODUtil.SEPARATOR);
        sb.append("See '").append(LOG_FILE_NAME).append("' for the details of execute log.").append(ODUtil.SEPARATOR);
        sb.append(ODUtil.charToString("-", 65));
        System.out.println(sb.toString());
    }

}
