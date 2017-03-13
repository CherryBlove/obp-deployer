package com.oceanbase.odeployer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.oceanbase.odeployer.parser.ODISectionParser;

/**
 * 子命令的注解
 * @author lbz@lbzhong.com 2016/05/13
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ODCommandAttribute {

    /** 子命令接受参数, 用','号分隔 */
    String argument() default "";
    
    /** 子命令绑定的配置单元 */
    Class<? extends ODISectionParser> section() default ODISectionParser.class;
    
    /** 是否需要预连接所有IP节点 */
    boolean connectAll() default false;
    
    /** 是否需要连接MS */
    boolean connectMS() default false;
    
    /** 说明 */
    String description() default "";
    
}
