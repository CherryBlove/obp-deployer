package com.oceanbase.odeployer.common;

import java.util.List;

import com.oceanbase.odeployer.task.ODTask;

/**
 * 动作执行器
 * @author lbz@lbzhong.com 2016/05/01
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public abstract class ODActionExecutor {
    
    /**
     * 执行动作
     * @param index 动作在列表中的序号, 即第index个动作
     * @param argv 命令行参数
     * @param task action所属的task
     */
    public abstract void execute(int index, List<String> argv, ODTask task);
    
}
