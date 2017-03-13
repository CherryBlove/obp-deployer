package com.oceanbase.odeployer.start;

/**
 * 启动命令参数
 * @author lbz@lbzhong.com 2016/4/4
 * @since OD1.0
 */
public class ODParameter {

    public String name;
    public String value;
    
    public ODParameter(String name) {
        this.name = name;
        this.value = "";
    }
    
}
