package com.oceanbase.odeployer.parser;

import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.common.ODItem;

/**
 * 配置项模块解析器接口
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public interface ODISectionParser {
    
    /**
     * 初始化配置单元类型
     */
    public abstract void init();

    /**
     * section的名称<br>
     * 识别配置文件中的配置项，只解析特定前缀的配置顶
     * @return 配置项前缀
     */
    String getSectionName();
    
    /**
     * Section的描述信息, 用于生成配置文件
     * @return 描述文本
     */
    String getDescription();
    
    /** 解析配置文件开始前自定义操作 */
    void before();
    
    /**
     * 执行解析
     * @param cmd 子命令
     * @param item 配置项
     * @param value 输入值
     * @throws Exception
     */
    void parseConfigureItems(ODCommand cmd, ODItem item, String value) throws Exception;

    /**
     * 解析配置结束后自定义操作
     * 如: 添加重要信息到程序输出的头部
     *     添加特殊配置项到键值对集合
     *     检查特殊配置项的完整性
     * */
    void after();

}
