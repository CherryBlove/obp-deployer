package com.oceanbase.odeployer.action;

import java.util.List;

import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODActionExecutor;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.start.ODPaxosParameterGenerator;
import com.oceanbase.odeployer.start.ODStartParameter;
import com.oceanbase.odeployer.task.ODStartTask;
import com.oceanbase.odeployer.task.ODTask;
import com.oceanbase.odeployer.util.ODLogger;

/**
* @author yanfeizhang68@gmail.com 2016/12/30
* @since OD2.0
*/

public class ODStartActionExecutor extends ODActionExecutor {
    
    @Override
    public void execute(int index, List<String> argv, ODTask task) {
        ODStartTask startTask = (ODStartTask) task;
        ODAction action = startTask.getAction(index);
        ODServer server = action.getServer();
        List<ODServerName> servernames = action.getServernames();
        double serverWait = 0;
        // 获取附加的启动参数
        ODStartParameter parameters = startTask.getParameterGenerator().generateStartParameter(index);
        if(parameters == null) {
           ODLogger.debug("parameters=null", (new Throwable()).getStackTrace());
        }
        //add zhangyf [paxos] 170522
        try{
        	serverWait = Double.parseDouble((String)ODPaxosParameterGenerator.getActionList().get(index).getParameters().get(2));         	
        }catch(Exception e)
        {
        	serverWait = 0.5;
        }
        //add end
        //mod zhangyf [paxos] 170109
//        System.out.println("index "+index+ODServer.allServerList+""+startTask.getMrs().ip+" "+ startTask.getMups().ip);
        server.start(serverWait,servernames, startTask.getMrs().ip, startTask.getMups().ip,parameters, 
                startTask.isWipeData(), startTask.isWipeLog(), startTask.isWipeEtc());
    }
    
}