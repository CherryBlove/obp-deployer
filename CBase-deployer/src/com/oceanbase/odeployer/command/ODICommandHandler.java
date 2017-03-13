package com.oceanbase.odeployer.command;

import com.oceanbase.odeployer.common.ODError;

/**
 * 子命令处理器接口
 * <p>自定义的处理器必须实现该接口</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public interface ODICommandHandler {

    /**
     * 处理子命令
     * @param cmd 子命令
     * @return 执行是否成功
     * @throws Exception
     */
    ODError handleCommand(ODCommand cmd) throws Exception;
    

}
