package com.oceanbase.odeployer.common;

import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.command.ODICommandHandler;
import com.oceanbase.odeployer.parser.ODISectionParser;

import java.util.List;

/**
 * 自定义的注册类的集合
 * <p>用于初始化ODDeploy, 若无相应自定义类, 则无需设值</p>
 * <p>可自定义的类：</p>
 * <blockquote><pre>
 * 1. {@code ODCommand} 最多自定义一个
 * 2. {@code ODItem} 最多自定义一个
 * 3. {@code ODICommandler} 最多自定义一个
 * 4. {@code ODISectionParser} 多个
 * 5. {@code ODServer} 最多自定义一个
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/4/6
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODRegisterParameter {

    /** 新增子命令类型 */
    public Class<? extends ODCommand> customCommandClass;

    /** 新增配置项类型 */
    public Class<? extends ODItem> customItemClass;

    /** 新增子命令处理器 */
    public Class<? extends ODICommandHandler> customCommandHandlerClass;

    /** 新增配置项解析器, 可新增多个解析器 */
    public List<Class<? extends ODISectionParser>> customSectionParserList;

    /** 扩展的Server */
    public Class<? extends ODServer> customServerClass;

}
