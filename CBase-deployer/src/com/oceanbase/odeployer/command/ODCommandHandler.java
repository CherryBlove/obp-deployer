package com.oceanbase.odeployer.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.parser.ODISectionParser;
import com.oceanbase.odeployer.task.ODTask;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;

/**
 * 子命令处理器
 * <p>实现接口{@code ODICommandHandler}, 处理基本的子命令.</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODCommandHandler implements ODICommandHandler {

    private ODDeployer deployer = ODDeployer.getInstance();

    private Map<ODItem, Object> configurations;
    
    /** 自动处理的Task, key是section的名称 */
    private static Map<String, ODItem> taskToItemMap = new HashMap<>();

    /** 可用的MS */
    protected String availableMSIp;

    @Override
    public ODError handleCommand(ODCommand cmd) throws Exception {
        ODError ret = ODError.SUCCESS;
        configurations = deployer.getConfigurations();
//ODLogger.log("configurations "+cmd+" "+configurations.toString());        
        if(cmd != ODCommand.CONFIGURATION //忽略生成配置文件模板的命令
                && (configurations == null || configurations.isEmpty())) {
            ret = ODError.ERROR;
            ODLogger.error("Configuration map is empty!", (new Throwable()).getStackTrace());
        }
        // 若子命令需要连接数据库
        if(cmd.getNeedAliveMS()) {
            availableMSIp = deployer.getOceanbase().getAliveMsIp();
            if(availableMSIp == null) {
                ODLogger.log("[ERROR] No alive mergeserver is found!");
                ret = ODError.ERROR;
            }
        }
        if (ret.isSuccess()) {
            if(cmd == ODCommand.ALL_START) {
                ret = handleAllStart();
            } else if(cmd == ODCommand.ALL_STOP) {
                ret = handleAllStop(cmd.getArgumentList());
//   ODLogger.log("all_stop");             
            } else if(cmd == ODCommand.CONFIGURATION) {
                ret = handleCreateConfiguration();
            } else {
                // 处理映射为Task的命令
//ODLogger.log("cmd  deployer");              	
                Class<? extends ODISectionParser> clazz = cmd.getBindingSection();
                if(clazz != null && clazz != ODISectionParser.class) {
                    ODISectionParser section = deployer.getSectionParser(clazz);
                    if(section != null) {
                        ODItem taskItem = taskToItemMap.get(section.getSectionName());
                        if(taskItem != null) {    
//ODLogger.log("handleTask "+cmd.getArgumentList()+" "+taskItem+" "+section.getSectionName());                          	
                            ret = handleTaskCommand(cmd.getArgumentList(), taskItem, section.getSectionName());
                        } else {
                            ODLogger.log("[ERROR] Command '" + cmd + "' is unhandled!");
                            ret = ODError.ERROR;
                        }
                    } else {
                        ODLogger.log("[ERROR] Command '" + cmd + "' binding to undefined section '" + clazz + "'!");
                        ret = ODError.ERROR;
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * 添加需要处理的task
     * @param section 与Task绑定的section
     * @param item 保存Task的配置项
     */
    public static void addSectionBindingTask(String section, ODItem item) {
        taskToItemMap.put(section, item);
    }

    // ----------------------------------------------------------- private

    /** all-start */
    private ODError handleAllStart() throws Exception {
        ODError ret = ODError.SUCCESS;
        if(deployer.getOceanbase() != null) {
            ret = deployer.getOceanbase().start();
        }
        return ret;
    }

    /** all-stop */
    private ODError handleAllStop(List<String> argumentList) throws Exception {
        ODError ret = ODError.SUCCESS;
        if(deployer.getOceanbase() != null) {
            if(argumentList.size() == 1) {
                boolean force = false;
                String argv = argumentList.get(0);
                if(argv.equalsIgnoreCase("f") || argv.equalsIgnoreCase("force")) {
                    force = true;
                } else if(argv.equalsIgnoreCase("nf") || argv.equalsIgnoreCase("nonforce")) {
                    force = false;
                } else {
                    ODLogger.log("[ERROR] the argument of '" + argv + "' is undefined, it should be 'f|force' or 'nf|nonforce'!");
                    ret = ODError.ERROR;
                }
                if(ret.isSuccess()) {
                    ret = deployer.getOceanbase().stop(force);
                }
            }
        }
        return ret;
    }

    /**
     * 生成配置文件模板
     * @return 必须配置了解析器,否则出错
     */
    private ODError handleCreateConfiguration() throws Exception {
        ODError ret = ODError.SUCCESS;
        String filename = "config/odeployer.cfg.template";
        String filetext = null;
        // 从解析器中分类获取所有配置项信息
        List<ODISectionParser> sectionParserList = deployer.getSectionParserList();
        if(sectionParserList != null && sectionParserList.size() > 0) {
            filetext = ODUtil.buildConfigurationTemplate(deployer.getVersion(), sectionParserList, deployer.getParameterCeneratorList());
        } else {
            ODLogger.error("get SectionParser list fail!", (new Throwable()).getStackTrace());
            ret = ODError.ERROR;
        }
        if(ret.isSuccess()) {
            try {
                ODUtil.writeFile(filename, filetext);
            } catch (Exception e) {
                ret = ODError.ERROR;
            }
        }
        if(ret.isSuccess()) {
            ODLogger.log("Create configuration template file '" + filename + "' success!");
        } else {
            ODLogger.log("Create configuration template file '" + filename + "' fail!");
        }
        return ret;
    }
    
    @SuppressWarnings("unchecked")
    private ODError handleTaskCommand(List<String> argv, ODItem item, String sectionName) throws Exception {
        ODError ret = ODError.SUCCESS;
        Object temp = configurations.get(item);
        if(temp == null) {
            ret = ODError.ERROR;
            System.out.println("No task is defined!");
        } else {
            if(argv == null || argv.size() == 0) {
                ret = ODError.ERROR;
                ODLogger.error("The argmuents of com`mand for task should >= 1!", (new Throwable()).getStackTrace());
            } else {
                String taskname = argv.get(0);
                Map<String, ODTask> taskMap = (Map<String, ODTask>) temp;
                ODTask task = taskMap.get(taskname);              
                if(task == null) {
                    System.out.println("Task of '" + taskname + "' is undefined!");
                    ret = ODError.ERROR;
                } else {
                    ret = task.execute(argv, sectionName);
                }
            }
        }
        return ret;
    }
}

