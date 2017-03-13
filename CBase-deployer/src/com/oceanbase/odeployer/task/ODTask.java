package com.oceanbase.odeployer.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODAction;
import com.oceanbase.odeployer.common.ODActionExecutor;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;

/**
 * 任务类
 * <p>自定义的任务必须继承该类, 并实现execute和setValue两个方法</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public abstract class ODTask {

    /** 任务名称 */
    protected String name;

    /** 动作列表 */
    protected List<ODAction> actionList = new ArrayList<>();
    
    /** 动作执行器 */
    private Map<ODItem, ODActionExecutor> actionExecutorMap;
    
    /** 正在执行action的线程计数 */
    private AtomicInteger executingCount = new AtomicInteger(0);

    /**
     * 运行任务, 可以执行多次
     * @param argv 命令行传入的参数, 第[0]个参数是task的名字
     * @return 执行结果
     * @throws Exception 
     */
    public abstract ODError execute(List<String> argv, String sectionName) throws Exception;
    
    /**
     * 执行动作
     * @param argv 命令行参数
     * @return
     */
    public ODError executeActions(final List<String> argv, String sectionName) {
        ODError ret = ODError.SUCCESS;
        // 添加执行器
        List<ODItem> items = ODItem.getItem(sectionName);
        for(ODItem it: items) {       	
            if(it.isAction()) {
                try {
                    addActionExecutor(it, it.getExecutor().newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
//        for(int j = 0; j < actionList.size(); j++){
//           final ODAction actionMrs = actionList.get(j);
//           final ODActionExecutor executorMrs = actionExecutorMap.get(actionMrs.getItem());
//           if(actionMrs.getServer().ip == ODStartTask.mRs.ip )//主RS所在主机优先执行
//           {
//ODLogger.log("action "+actionMrs.getServer().ip+" "+argv+" "+this);         	   
//           	executorMrs.execute(0, argv, this); 
//           }else if(ODStartTask.mRs.ip == null || actionMrs.getServer().ip == null){
//        	   ret = ODError.ERROR;
//               ODLogger.error("[ERROR] Master ip is not alive!", (new Throwable()).getStackTrace());
//           }
//        }
        
        for(int i = 0; i < actionList.size(); i++) {
            final ODAction action = actionList.get(i);
            if(actionExecutorMap != null) {
                final ODActionExecutor executor = actionExecutorMap.get(action.getItem());
//ODLogger.log("action2 "+action.getServer().ip+" "+argv+" "+this);                
                if(executor != null) { //调用对应的actionExecutor
                    action.waitInterval();
                    //add 
//                    if(action.getServer().ip != ODStartTask.mRs.ip)//主RS所在主机优先执行
//                    {
                    //end add
                      if(i < actionList.size() - 1 && // 若不是最后一个action
                        actionList.get(i + 1).getInterval() < 0) { // 若后一个action的需要同时执行
                         final int index = i; // action的序号
                         final ODTask self = this;
                         ODDeployer.getInstance().executeAction(new Runnable() { // 启动新线程
                            @Override
                            public void run() {
                                executingCount.incrementAndGet(); // 增加计数
                                executor.execute(index, argv, self); // 在新线程中执行, 不阻塞, 使得后面的action可同时执行
                                executingCount.decrementAndGet(); // 减少计数
                            }
                        });
                      } else {
                          if(action.getInterval() >= 0) { // 若interval >= 0, 说明需要等待前面所有线程结束
                            while(executingCount.get() > 0); // 等待
                         }
                         executor.execute(i, argv, this); // 在主线程中执行
                      }
//                   }   
                } else {
                    ret = ODError.ERROR;
                    ODLogger.error("[ERROR] action executor for '" + action.getItem() + "' is undefined!", 
                            (new Throwable()).getStackTrace());
                }
            } else {
                ret = ODError.ERROR;
                ODLogger.error("[ERROR] action executor map is null!", (new Throwable()).getStackTrace());
            }
        }
        ODUtil.sleep(1);
        while(executingCount.get() > 0); // 等待所有action结束
        return ret;
    }
    
    /**
     * 将配置项与Task属性绑定
     * @param item common类型的配置项
     * @param value 值
     */
    public void setValue(ODItem item, Object value) {
        //do nothing
    };

    /**
     * 任务名称
     * @return name
     */
    public String getTaskName() {
        return name;
    }

    /**
     * 添加动作
     * @param action 不应为null
     */
    public void addAction(ODAction action) {
        if(action != null) {
            actionList.add(action);
        }
    }
    
    /**
     * 获取action
     * @param index
     * @return 可能为null
     */
    public ODAction getAction(int index) {
        if(index >=0 && index < actionList.size()) {
            return actionList.get(index);
        }
        return null;
    }

    /** 子类必须实现Task(String)类型的构造函数 */
    public ODTask(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        String str = "ODTask={name=" + name + ",actionList=[";
        for(ODAction action: actionList) {
            str += action.toString() + ",";
        }
        str = ODUtil.removeLastChar(str);
        str += "]}";
        return str;
    }
    
    // ----------------------------------------------------------- private
    
    /**
     * 添加动作处理器
     * @param item action映射的item
     * @param executor
     */
    private void addActionExecutor(ODItem item, ODActionExecutor executor) {
        if(actionExecutorMap == null) {
            actionExecutorMap = new HashMap<>();
        }
        actionExecutorMap.put(item, executor);
    }
}
