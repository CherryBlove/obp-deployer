package com.oceanbase.odeployer.task;

import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODError;

/**
 * 部署任务
 * @author lbz@lbzhong.com 2016/04/21
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODDeployTask extends ODTask {
    
    private String sourceIp;
    private String sourceUsername;
    private String sourcePassword;
    private String sourceDir;
    private String targetDir;
    private int afterstartWait;
    private int bootstrapWait;

    public ODDeployTask(String name) {
        super(name);
    }

    @Override
    public ODError execute(List<String> argv, String sectionName) throws Exception {
//ODLogger.log("execute deploy task");
        ODError ret = ODError.SUCCESS;
        ODDeployer deployer = ODDeployer.getInstance();
        if(deployer.getOceanbase() != null) {
            ret = deployer.getOceanbase().deploy(sourceIp, sourceUsername, 
            		sourcePassword, sourceDir);
        } else {
            ret = ODError.ERROR;
        }
        return ret;
    }
    
    /**
     * 目标安装目录
     * @return
     */
    public String getTargetDir() {
    	return targetDir;
    }
    
    public int getAfterstartWait() {
    	return afterstartWait;
    }
    
    public int getBootstrapWait() {
    	return bootstrapWait;
    }

}
