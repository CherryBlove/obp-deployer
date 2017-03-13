package com.oceanbase.odeployer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.oceanbase.odeployer.common.ODActionExecutor;

/**
 * 配置项的注解
 * @author lbz@lbzhong.com 2016/05/13
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ODItemAttribute {
    
    /** 是否可为空 */
    boolean nullable() default false;

    /** 
     * 赋值的格式, 多个值时用","号分隔 
     * <pre>
     * 1. number: int, 类型数字;
     * 2. float: float, 小数;
     * 3. boolean: boolean, 布尔, true|false;
     * 4. string及其他: String, 字符串
     * 5. $H_ITEM: 表示取值以H_ITEM为键保存到配置项集合,
     *             (1) 只允许引用前缀为"H_"的配置项,且该配置项为单值类型
     *             (2) Action类型的配置项不允许引用其它配置项
     * 6. [a|b]: List<String>, 表示取值集合为'a'和'b'
     * 7. [a/b]: String, 表示取值为'a'和'b'之一
     * 8. ip: ODServer, 将检查ip所指的主机节点是否已定义
     * 9. section: Map<ODTask>
     * 10. task: ODTask
     * </pre>
     */
    String pattern() default "string";
   
    /** 配置项的类型 */
    Class<? extends ODActionExecutor> executor() default ODActionExecutor.class;
    
    /** 默认值 */
    String defaultValue() default "";
    
    /** 说明 */
    String description() default "";
    
}
