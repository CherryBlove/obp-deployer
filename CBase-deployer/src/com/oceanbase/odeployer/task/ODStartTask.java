package com.oceanbase.odeployer.task;

import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.start.ODParameterGenerator;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.common.ODConfiguration;

/**
 * 启动任务
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODStartTask extends ODTask {

	/** 判断所所启动的RS、UPS数目与所设定的RS、UPS数目是否一致 */
	private int rsCount;
	private int upsCount;
	ODConfiguration conf;
	
    /** 清空数据 */
    private List<String> wipe;   
    private boolean isWipeData = false;
    private boolean isWipeLog = false;
    private boolean isWipeEtc = false;
    private ODServer masterRS;
    private ODServer masterUPS;
    private ODServer serverIp;
    private ODParameterGenerator parameterGenerator;
    public static ODServer mRs;
    
    /**
     * 子类必须实现Task(String)类型的构造函数
     * @param name 任务类型
     */
    public ODStartTask(String name) {
        super(name);
    }

    @Override
    public ODError execute(List<String> argv, String sectionName) throws Exception {
        ODError ret = ODError.SUCCESS;
        if(actionList != null && actionList.size() > 0) {
            ODDeployer deployer = ODDeployer.getInstance();
            // 从配置项中获取Oceanbase的版本名称, 调用对应的启动命令参数生成器
            ODDeployTask deployTask = deployer.getDeployTask();
            int startWait = deployTask.getAfterstartWait();
            int bootstrapWait = deployTask.getBootstrapWait();
            int bootstrapTimeout = 600;//bootstrap延迟时间
            
            parameterGenerator = deployer.getParameterGenerator();
            if(parameterGenerator != null) {
                // ----------------------------------------------------------- 1. start
                if(wipe != null) {
                     if(!wipe.contains("none")) {
                    	 for(String w: wipe) { // 是否三清
                             if(w.equals("data")) {
                                 isWipeData = true;
                             } else 
                             if(w.equals("log")) {
                                 isWipeLog = true;                              
                             }else 
                             if(w.equals("etc")) {
                                 isWipeEtc = true;
                             }
                         }
                     }
                } else {
                    // 默认全部清空
                    isWipeData = true;
                    isWipeLog = true;
                    isWipeEtc = true;
                }
                // 初始化附加的启动参数生成器
                ret = parameterGenerator.init(actionList);                                
                if(ret.isError()) {
                	ODLogger.debug("fail to init parameterGenerator", (new Throwable()).getStackTrace());
                } else {
                	ret = putMasterRS();
                	ret = putMasterUPS();
                	if(masterRS == null || masterUPS == null) {
                		ODLogger.log("start one rootserver and updateserver at least!");
                	} else {
                		// 执行action
                        executeActions(argv, sectionName);
                        // ----------------------------------------------------------- 2. setRole
                        if(ret.isSuccess()) {
                            if(startWait > 0) {
                                System.out.println("Wait " + startWait + "s to do something before bootstrap...");
                                ODUtil.sleep(startWait * 1000);
                            }
                            ret = parameterGenerator.beforeBootstrap();
                            if(ret == null || ret.isError()) {
                                ODLogger.log("Fail to do beforeBootstrap");
                                ret = ODError.ERROR;
                            }
                        }
                        // ----------------------------------------------------------- 3. bootstrap
                        if(isWipeData && ret.isSuccess()) {
//                        if(ret.isSuccess()) {
                            if(bootstrapWait >= 0) {
                                System.out.println("Wait " + bootstrapWait + "s to bootstrap...");
                                ODUtil.sleep(bootstrapWait * 1000);
                            } else {
                                ODUtil.sleep(3 * 1000);
                            }
                            // 执行bootstrap
                            ret = masterRS.bootstrap(bootstrapTimeout);
                        }
                	}
                }
            } else {
                ODLogger.error("parameterGenerator is null!", (new Throwable()).getStackTrace());
                ret = ODError.ERROR;
            }
        } else {
            System.out.println("Action list is empty!");
            ret = ODError.ERROR;
        }
        return ret;
    }
    
    public ODParameterGenerator getParameterGenerator() {
        return parameterGenerator;
    }

    public List<String> getWipe() {
        return wipe;
    }

    public boolean isWipeData() {
        return isWipeData;
    }

    public boolean isWipeLog() {
        return isWipeLog;
    }

    public boolean isWipeEtc() {
        return isWipeEtc;
    }
    
    //add zhangyf [paxos] Ds:U/u for paxos
    public int getRsCount() {
    	return rsCount;
    }
    
    public int getUpsCount() {
    	return upsCount;
    }
    //
    /**
     * 默认启动所指定的RS为master，所指定的UPS为master
     * @return 可能为空
     */  
    public ODError putMasterRS() {
    	ODError ret = ODError.SUCCESS;
    	if(masterRS == null && actionList != null) 
    	{
    		for(ODAction ac: actionList) {
        		for(ODServerName sn: ac.getServernames()) {
        			if(sn == ODServerName.RS) {//判断一个action里的server是否是RS，若是RS判断是否是主RS 
        				//mod zhangyf [paxos] 170522
        				serverIp = ac.getServer();
        				if((serverIp.ip).equals(ODDeployer.getStValueMrs()))
        				{
        					masterRS = ac.getServer();
        					mRs = masterRS;
        					return ret;        					
        				}
        				//mod end
        			}
        		}
        	}
    	}
    	if(masterRS == null || actionList == null)
    	{
    		ret = ODError.ERROR;
    		ODLogger.error("the action don`t have master Rootserver or not have server", new Throwable().getStackTrace());
    	}
        return ret;
    } 
    
    public ODError putMasterUPS(){
    	ODError ret = ODError.SUCCESS;
    	if(masterUPS == null && actionList != null)
    	{
    		for(ODAction ac : actionList)
    		{
    			for(ODServerName sn : ac.getServernames())
    			{
    				if(sn == ODServerName.UPS)//判断一个action里的server是否是UPS，若是RS判断是否是主UPS  
    				{
    					//mod zhangyf[paxos] 170522
    					serverIp = ac.getServer();
    					if((serverIp.ip).equals(ODDeployer.getStValueMups()))
    					{
    						masterUPS = ac.getServer();
    						return ret;
    					}
    					//mod end
    				}
    			}
    		}
    	}
    	if(masterUPS == null || actionList == null)
    	{
    		ret = ODError.ERROR;
    	}
    	return ret;
    }
    
    public ODServer getMrs(){
    	return masterRS;
    }
    
    public ODServer getMups(){
    	return masterUPS;
    }
}
