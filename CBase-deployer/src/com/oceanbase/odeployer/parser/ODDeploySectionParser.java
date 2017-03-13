package com.oceanbase.odeployer.parser;

import com.oceanbase.odeployer.common.ODSectionType;
import com.oceanbase.odeployer.task.ODDeployTask;

/**
 * "deploy.*"类型的配置项
 * <p>继承通用的解析器</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public class ODDeploySectionParser extends ODCommonSectionParser {

    @Override
    public String getSectionName() {
        return "deploy";
    }
    
    @Override
    public String getDescription() {
        return "Copy the directory of oceanbase to every remote node of the cluster.";
    }

    // ----------------------------------------------------------- custom

    public void init() {

        // Section的类型为 0
        // 初始化通用解析器
        init(ODSectionType.SECTION0, ODDeployTask.class);
    }

}
