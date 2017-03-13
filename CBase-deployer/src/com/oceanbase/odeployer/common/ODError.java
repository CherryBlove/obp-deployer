package com.oceanbase.odeployer.common;

/**
 * 自定义错误
 * <blockquote><pre>
 * 函数返回值: 
 * 1. SUCCESS
 * 2. ERROR
 * </pre></blockquote>
 * @since OD1.0
 * @author lbz@lbzhong.com 2016/03/30
 */
public enum ODError {
    //通用函数返回值
    /** 函数执行成功 */
    SUCCESS,
    
    /** 函数执行失败 */
    ERROR,
    
    //解析配置错误
    /** 自定义异常 */
    ERROR_EXCEPTION,
    
    /** 不符合“配置项 = 值”的模式 */
    ERROR_PATTERN,
    
    /** 未识别的配置项 */
    ERROR_UNKNOWN_ITEM,
    
    /** 配置项值为空 */
    ERROR_EMPTY_VALUE,
    
    /** 配置项值格式错误 */
    ERROR_WRONG_VALUE,
    
    /**  缺失配置项 */
    ERROR_MISS_ITEM,
    
    /** 配置项重复 */
    ERROR_CONFLICT,
    ;
    
    /**
     * 判断返回值是否成功
     * @return boolean
     */
    public boolean isSuccess() {
        return (this == SUCCESS);
    }
    
    /**
     * 判断返回值是否非成功
     * @return boolean
     */
    public boolean isError() {
        return !isSuccess();
    }
}
