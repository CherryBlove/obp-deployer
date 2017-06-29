package com.oceanbase.odeployer.start;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.util.ODLogger;

/**
 * MultiUps-Paxos版本
 *<blockquote><pre>
 * ./bin/rootserver -r 0.0.0.0:0 -R 0.0.0.0:0 -i bond0 -U 3 -u 3 -C 3 -G 2 -K 2 -F true
 * ./bin/updateserver -r 0.0.0.0:0 -p 1 -m 2 -i bond0 -C 3 -g 2
 * ./bin/mergeserver -r 0.0.0.0:0:0 -p 3 -z 4 -i bond0 -C 3
 * ./bin/chunkserver -r 0.0.0.0:0:0 -p 5 -n obtest -i bond0 -C 3
 *</pre></blockquote>
 * U/u: 分别表示RS和UPS的数目,动态计算<br>
 * G:Group组个数
 * K:小集群个数
 * F:选择是否使用paxos
 * g:ups所在group组的组号
 * C: 集群号, 启动一个RS,集群号自增1,后面的各Server与该RS的集群号保持一致, 从1开始
 * @author yanfeizhang68@gmail.com 2017/6/20
 * @since OD2.0
 */

public class ODMultiUpsParameterGenerator extends ODParameterGenerator {
	
    /** 启动序列Action列表 */
    private static List<ODAction> actionList;

    /** RS数目 */
    private int RSCount;

    /** UPS数目 */
    private int UPSCount;

    private int clusterId;

    private boolean isInit;
    
    public ODMultiUpsParameterGenerator() {
        startParameter.addRsParameterName("U");
        startParameter.addRsParameterName("u");
        startParameter.addRsParameterName("C");
        startParameter.addRsParameterName("G");
        startParameter.addRsParameterName("K");
        startParameter.addRsParameterName("F");
        
        startParameter.addUpsParameterName("C");
        startParameter.addUpsParameterName("g");
        startParameter.addMsParameterName("C");
        startParameter.addCsParameterName("C");
    }

	@Override
	public ODError init(List<ODAction> actionList) {
		// TODO Auto-generated method stub
		 ODError ret = ODError.SUCCESS;
	    	if(!isInit) {
	            RSCount = 0;
	            UPSCount = 0;
	            clusterId = 0;
	            ODMultiUpsParameterGenerator.actionList = new ArrayList<>();
	            ODMultiUpsParameterGenerator.actionList.addAll(actionList);

	            // 计算启动RS和UPS的数目
	            for(ODAction act: actionList) {
	                for(ODServerName sn: act.getServernames()) {
	                    if(sn == ODServerName.RS) {
	                        RSCount++;
	                    } else if(sn == ODServerName.UPS) {
	                        UPSCount++;
	                    }
	                }
	            }         
	            //mod zhangyf [paxos] 170522
	            if(RSCount == Integer.parseInt(ODDeployer.getStValueRsCount()) && UPSCount == Integer.parseInt(ODDeployer.getStValueUpsCount())){           	
	            	isInit = true;          	
	            }else{
	            	ODLogger.log(RSCount+" "+Integer.parseInt(ODDeployer.getStValueRsCount())+" "+UPSCount+" "+Integer.parseInt(ODDeployer.getStValueUpsCount()));
	            	ODLogger.log("real RS and UPS is "+RSCount+" "+UPSCount);
	            	isInit = false;
	                ret = ODError.ERROR;
	                ODLogger.error("the rscount or upscount is not fit with that have defined", new Throwable().getStackTrace());
	            }
	            //mod end
	        } else {
	            ODLogger.error((new Throwable().getStackTrace()));
	            ret = ODError.ERROR;
	        }        
	        return ret;
	}

	@Override
	public ODStartParameter generateStartParameter(int index) {
		// TODO Auto-generated method stub
		int thisClusterId = -1;
        if(isInit) {
            List<String> rsParameterValues = new ArrayList<>();
            List<String> upsParameterValues = new ArrayList<>();
            List<String> msParameterValues = new ArrayList<>();
            List<String> csParameterValues = new ArrayList<>();
            for(ODServerName sn: actionList.get(index).getServernames()) {
                if(sn == ODServerName.RS) {
                    clusterId++;
                }
            }
            Integer CustomClusterId =Integer.parseInt((String)actionList.get(index).getParameters().get(1));
            if(CustomClusterId != null && CustomClusterId > -1 && CustomClusterId < 6){
            	thisClusterId = CustomClusterId;
            }else{
            	ODLogger.log("please set correct clusterid between[0,5],if not the tools will recommand a random clusterID in [0,5] for you");
            	thisClusterId = (int)Math.random()*clusterId;
            }
            // 必须保持value和name的数目和顺序完全一致
            String paraG = (String)actionList.get(index).getParameters().get(2);
            String paraK = (String)actionList.get(index).getParameters().get(3);
            String paraF = (String)actionList.get(index).getParameters().get(4);
            String parag = (String)actionList.get(index).getParameters().get(5);
            int ucount = (UPSCount/Integer.parseInt(paraG))+1;
            rsParameterValues.add(String.valueOf(RSCount)); // RS的数目
            rsParameterValues.add(String.valueOf(ucount)); // UPS的数目
            rsParameterValues.add(String.valueOf(thisClusterId)); // 集群号
            rsParameterValues.add(paraG);
            rsParameterValues.add(paraK);
            rsParameterValues.add(paraF);
            upsParameterValues.add(String.valueOf(thisClusterId)); // 集群号
            upsParameterValues.add(parag);
            msParameterValues.add(String.valueOf(thisClusterId)); // 集群号
            csParameterValues.add(String.valueOf(thisClusterId)); // 集群号
            startParameter.setParameterValues(rsParameterValues, upsParameterValues, 
                    msParameterValues, csParameterValues);
            return startParameter;
        } else {
            ODLogger.error((new Throwable().getStackTrace()));
        }
        return null;
	}

	@Override
	public ODError beforeBootstrap() {
		// TODO Auto-generated method stub
		return ODError.SUCCESS;
	}
	
    public static List<ODAction> getActionList(){
    	return actionList;
    }

}
