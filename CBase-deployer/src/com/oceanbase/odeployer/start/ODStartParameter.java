package com.oceanbase.odeployer.start;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.util.ODLogger;

/**
 * 各Server的附加启动参数的集合
 * @author lbz@lbzhong.com 2016/4/4
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODStartParameter {

    /** 各Server的附加启动参数 */
    private List<ODParameter> rsParameter = new ArrayList<>();
    private List<ODParameter> upsParameter = new ArrayList<>();
    private List<ODParameter> msParameter = new ArrayList<>();
    private List<ODParameter> csParameter = new ArrayList<>();
    
    /** 启动UPS,MS,CS是否使用主集群RS的IP  */
    private boolean useMasterRsIP = true;
    private String localRsIp;

    /** 空构造函数，无任何附加启动参数 */
    public ODStartParameter() {}
        
    // ----------------------------------------------------------- setter
    
    public ODError setParameterValues(List<String> rsParameterValues, List<String> upsParameterValues, 
            List<String> msParameterValues, List<String> csParameterValues) {
        ODError ret = ODError.SUCCESS;
        if(ret.isSuccess() && rsParameterValues != null) {
            ret = setParameterValues(rsParameter, rsParameterValues);
            if(ret.isError()) {
                ODLogger.error("fail to set RS parameter value", 
                        (new Throwable()).getStackTrace());
            }
        }
        if(ret.isSuccess() && upsParameterValues != null) {
            ret = setParameterValues(upsParameter, upsParameterValues);
            if(ret.isError()) {
                ODLogger.error("fail to set UPS parameter value", 
                        (new Throwable()).getStackTrace());
            }
        }
        if(ret.isSuccess() && msParameterValues != null) {
            ret = setParameterValues(msParameter, msParameterValues);
            if(ret.isError()) {
                ODLogger.error("fail to set MS parameter value", 
                        (new Throwable()).getStackTrace());
            }
        }
        if(ret.isSuccess() && csParameterValues != null) {
            ret = setParameterValues(csParameter, csParameterValues);
            if(ret.isError()) {
                ODLogger.error("fail to set CS parameter value", 
                        (new Throwable()).getStackTrace());
            }
        }
        return ret;
    }
    
    public ODError addRsParameterName(String parameterName) { 	
        ODError ret = addParameterName(rsParameter, parameterName);
        if(ret.isError()) {
            ODLogger.error("RS start parameter '" + parameterName + "' is duplicate", 
                    (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    public ODError addUpsParameterName(String parameterName) {
        ODError ret = addParameterName(upsParameter, parameterName);
        if(ret.isError()) {
            ODLogger.error("UPS start parameter '" + parameterName + "' is duplicate", 
                    (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    public ODError addMsParameterName(String parameterName) {
        ODError ret = addParameterName(msParameter, parameterName);
        if(ret.isError()) {
            ODLogger.error("MS start parameter '" + parameterName + "' is duplicate", 
                    (new Throwable()).getStackTrace());
        }
        return ret;
    }
    
    public ODError addCsParameterName(String parameterName) {
        ODError ret = addParameterName(csParameter, parameterName);
        if(ret.isError()) {
            ODLogger.error("CS start parameter '" + parameterName + "' is duplicate", 
                    (new Throwable()).getStackTrace());
        }
        return ret;
    }

    public void setLocalRsIp(String localRsIp){
    	this.localRsIp = localRsIp;
    	this.useMasterRsIP = false;
    }
    
    public void resetParameter() {
    	this.useMasterRsIP = true;
        resetParameter(rsParameter);
        resetParameter(upsParameter);
        resetParameter(msParameter);
        resetParameter(csParameter);
    }
    
    // ----------------------------------------------------------- getter

    public boolean isUseMasterRsIp(){
    	return useMasterRsIP;
    }
    
    public String getLocalRsIp(){
    	return localRsIp;
    }
    
    public List<ODParameter> getRsParameter() {
        return rsParameter;
    }
    
    public List<ODParameter> getUpsParameter() {
        return upsParameter;
    }
    
    public List<ODParameter> getMsParameter() {
        return msParameter;
    }
    
    public List<ODParameter> getCsParameter() {
        return csParameter;
    }
    
    // ----------------------------------------------------------- private
    
    private ODError addParameterName(List<ODParameter> parameter, String parameterName) {
        ODError ret = ODError.SUCCESS;
        for(ODParameter p: parameter) {
            if(p.name.equals(parameterName)) {
                ret = ODError.ERROR;
                break;
            }
        }
        if(ret.isSuccess()) {
            parameter.add(new ODParameter(parameterName));           
        }
        return ret;
    }
    
    private void resetParameter(List<ODParameter> parameter) {
        for(ODParameter p: parameter) {     	
            p.value = "";            
        }
    }

    private ODError setParameterValues(List<ODParameter> parameter, List<String> parameterValues) {
        ODError ret = ODError.SUCCESS;
        if(parameterValues.size() == parameter.size()) {
            for(int i = 0; i < parameter.size(); i++) {
                ODParameter p = parameter.get(i);
                String value = parameterValues.get(i);
                if(value != null && value.length() > 0) {
                    p.value = value;                    
                } else {
                    ODLogger.error("start value '" + value + "' for parameter '" + p.name + "' is empty", 
                            (new Throwable()).getStackTrace());
                }
            }
        } else {
            ret = ODError.ERROR;
            ODLogger.error("parameterValues.size() != parameter.size()", 
                    (new Throwable()).getStackTrace());
        }
        return ret;
    }
}
