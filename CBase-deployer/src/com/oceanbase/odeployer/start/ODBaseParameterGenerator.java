package com.oceanbase.odeployer.start;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODShell;
import com.oceanbase.odeployer.util.Pair;

/**
 * oceanbase 0.4.2基础版本
 *<blockquote><pre>
 * ./bin/rootserver -r 0.0.0.0:0 -R 0.0.0.0:0 -i bond0 -C 3
 * ./bin/updateserver -r 0.0.0.0:0 -p 1 -m 2 -i bond0 
 * ./bin/mergeserver -r 0.0.0.0:0:0 -p 3 -z 4 -i bond0
 * ./bin/chunkserver -r 0.0.0.0:0:0 -p 5 -n obtest -i bond0
 *</pre></blockquote>
 * @author lbz@lbzhong.com 2016/6/7
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODBaseParameterGenerator extends ODParameterGenerator {
    
    /** 启动序列Action列表 */
    private List<ODAction> actionList;

    private int rsCount; // 只能启动一个RS
    
    private boolean isInit;
    
    public ODBaseParameterGenerator() {
        startParameter.addRsParameterName("C");
    }

    @Override
    public ODError init(List<ODAction> actionList) {
        if(!isInit) {
            this.actionList = new ArrayList<>();
            this.actionList.addAll(actionList);

            rsCount = 0; //初始化
            for(ODAction ac: actionList) {
            	for(ODServerName sn: ac.getServernames()) {
                    if(sn == ODServerName.RS) {
                    	rsCount++;
                    }
                }
            }
            if(rsCount == 1) {
            	isInit = true;
                return ODError.SUCCESS;
            } else {
            	System.out.println("the version of 'OB_BASE' is support ONE rootserver only!");
            	return ODError.ERROR;
            }
        } else {
            ODLogger.error((new Throwable().getStackTrace()));
            return ODError.ERROR;
        }
    }

    @Override
    public ODStartParameter generateStartParameter(int index) {
        if(isInit) {
            List<String> rsParameterValues = new ArrayList<>();
        	// 必须保持value和name的数目和顺序完全一致
            rsParameterValues.add("1"); // 只能启动单集群，集群号一直为1
            startParameter.setParameterValues(rsParameterValues, null, null, null);
            return startParameter;
        } else {
            ODLogger.error((new Throwable().getStackTrace()));
        }
        return null;
    }
    
    @Override
    public ODError beforeBootstrap() {
        ODError ret = ODError.SUCCESS;
        ODServer masterRS = getMasterRS();
        if(masterRS != null) {
        	Pair<ODError, String> retExe = masterRS.exec("cd " + ODShell.getOceanbaseDir() + 
        			";bin/rs_admin -r " + masterRS.ip + " -p " + ODShell.getRsPort() + " set_obi_role -o OBI_MASTER");
        	ODLogger.log(retExe.second);
        }
        return ret;
    }
    
    /**
     * 默认启动序列中第一个出现的RS为master
     * @return 可能为空
     */
    public ODServer getMasterRS() {
    	if(actionList != null) {
    		for(ODAction ac: actionList) {
        		for(ODServerName sn: ac.getServernames()) {
        			if(sn == ODServerName.RS) {
        				return ac.getServer();
        			}
        		}
        	}
    	}
        return null;
    }
}
