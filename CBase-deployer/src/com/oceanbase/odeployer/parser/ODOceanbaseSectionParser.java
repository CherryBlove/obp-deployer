package com.oceanbase.odeployer.parser;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.common.ODConfiguration;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.common.ODSectionType;
import com.oceanbase.odeployer.task.ODDeployTask;

/**
 * "oceanbase.*"类型的配置项
 * <p>继承通用的解析器</p>
 * @author lbz@lbzhong.com 2016/06/12
 * @since OD1.0
 */
public class ODOceanbaseSectionParser extends ODCommonSectionParser {

    @Override
    public String getSectionName() {
        return "oceanbase";
    }
    
    @Override
    public String getDescription() {
        return "Copy the directory of oceanbase to every remote node of the cluster.";
    }

    // ----------------------------------------------------------- custom
    
    /**
     * 增加对oceanbase版本进行检查
     */
    @Override
    public void parseConfigureItems(ODCommand cmd, ODItem item, String value) throws Exception {
    	super.parseConfigureItems(cmd, item, value);
    	if(item.isName()) {
    		if(!ODDeployer.getInstance().isParameterGeneratorRegistered(value)) {
                ODConfiguration.printError(ODError.ERROR_EXCEPTION, item, 
                        "oceanbase.version(" + value + ") is undefined");
            }
    	}
    }

    public void init() {

        // Section的类型为 1
        // 初始化通用解析器
        init(ODSectionType.SECTION1, ODDeployTask.class);
    }

}
