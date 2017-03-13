package com.oceanbase.odeployer.parser;

import com.oceanbase.odeployer.common.ODSectionType;
import com.oceanbase.odeployer.task.ODStartTask;

/**
 * "start.*"类型的配置项
 * <p>继承通用的解析器</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public class ODStartSectionParser extends ODCommonSectionParser {

    @Override
    public String getSectionName() {
        return "start";
    }
    
    @Override
    public String getDescription() {
        return "Custom start sequence of server.";
    }

    // ----------------------------------------------------------- custom

    public void init() {
        // 初始化通用解析器, Section类型为 2
        init(ODSectionType.SECTION2, ODStartTask.class);
    }

}
