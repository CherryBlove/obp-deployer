package com.oceanbase.odeployer.common;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odeployer.util.ODPrinter;
import com.oceanbase.odeployer.util.ODUtil;

/**
 * 动作类
 * <p>必须绑定一个主机</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODAction {
    
    /** 绑定的配置项，表示动作的类型 */
    private ODItem item;

    /** 执行该动作的主机节点 */
    private ODServer server;
    
    private List<Object> parameters = new ArrayList<>(); 

    /** 
     * 执行等待间隔时间 
     * -1: 与上一个action同时执行
     * 0 : 前面所有action完成后, 立即执行
     * >0: 前面所有action完成后, 等待interval秒后执行
     * 默认为0
     */
    private int interval;
    
    public ODAction(ODItem item, List<Object> actionValue) {
        if(actionValue != null) {
            this.item = item;
            int size = actionValue.size();
            // 第一个值必须是ip
            if(size > 0) {
                server = (ODServer) actionValue.get(0);
            }
            if(size > 1) {
                if(size == 2) {
                    Object obj = actionValue.get(1);
                    if(obj instanceof Integer) { // 最后一个值可能是interval
                        interval = (int)obj;
                    } else {
                        parameters.add(obj);
                    }
                } else {
                    for(int i = 1; i < size; i++) { // 第0值是ip, 故从第1个值开始
                        Object obj = actionValue.get(i);
                        if(i == size - 1 && obj instanceof Integer) { // 最后一个值可能是interval
                            interval = (int)obj;
                        } else {
                            parameters.add(obj);
                        }
                    }
                }
            }
        }
    }
    
    public ODAction(ODItem item, ODServer server, List<String> servernames) {
        this(item, server, servernames, 0);
    }
    
    public ODAction(ODItem item, ODServer server, List<String> servernames, int interval) {
        this.item = item;
        this.server = server;
        if(servernames != null) {
            this.parameters.add(servernames); // servernames做为第0个参数
        }
        this.interval = interval;
    }

    /**
     * 用于调试
     * @return 格式化类型
     */
    public String toString() {
        String str = "ODAction={item=" + item + ",server=" + server;
        str += ",parameters="  + parameters + "";
        str += ",interval=" + interval + "}";
        return str;
    }
    
    public ODItem getItem() {
        return item;
    }

    public ODServer getServer() {
        return server;
    }

    public List<Object> getParameters() {
        return parameters;
    }
    
    public int getInterval() {
        return interval;
    }
    
    /**
     * 等待interval间隔时间
     */
    public void waitInterval() {
        if(interval > 0) {
            System.out.print("Wait: ");
            ODPrinter.printClocker(interval);
            System.out.println();
        } else {
            ODUtil.sleep(1); // 等待1ms, 避免跳过
        }
    }
    
    /**
     * Server名称列表 rs,ups,ms,lms,cs
     * @return 可能为null
     */
    @SuppressWarnings("unchecked")
    public List<ODServerName> getServernames() {
        if(parameters.size() > 0) {
            Object sn = parameters.get(0);
            List<String> servernames;
            if(sn instanceof List) { // 对应option类型
                servernames = (List<String>) sn;
            } else { //对应select类型
                servernames = new ArrayList<>();
                servernames.add((String) sn);
            }
            return ODUtil.stringToServerNameList(servernames);
        }
        return null;
    }

}
