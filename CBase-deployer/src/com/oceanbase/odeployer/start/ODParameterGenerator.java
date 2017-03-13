package com.oceanbase.odeployer.start;

import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODError;

import java.util.List;

/**
 * 启动命令参数生成器
 * @author lbz@lbzhong.com 2016/4/4
 * @since OD1.0
 */
public abstract class ODParameterGenerator {
    
    protected ODStartParameter startParameter = new ODStartParameter();

    /**
     * 初始化操作
     * 自定初始化某些全局变量
     * @param actionList 动作列表
     * @return 只可初始化一次
     */
    public abstract ODError init(List<ODAction> actionList);

    /**
     * 生成启动命令参数
     * @param index 当前Action在列表中的位置
     * @return 参数列表
     */
    public abstract ODStartParameter generateStartParameter(int index);
    
    /**
     * 在BootStrap前执行
     * @return
     */
    public abstract ODError beforeBootstrap();
    
    public ODStartParameter getStartParameter() {
        return startParameter;
    }

}
