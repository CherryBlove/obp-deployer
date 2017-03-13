package com.oceanbase.odeployer.common;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置项添加器
 * 具有重复和非空检查功能
 * @author lbz@lbzhong.com 2016/4/5
 * @since OD1.0
 */
public class ODItemAdder {

    /** 用于检查配置项重复冲突 */
    protected Set<ODItem> itemSet = new HashSet<>();

    /**
     * 配置项是否已存在
     * @param item 配置项
     * @return 若已存在，返回true
     */
    protected boolean isExist(ODItem item) {
        if(itemSet.contains(item)) {
            ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
            return true;
        }
        itemSet.add(item);
        return false;
    }

    /**
     * 添加配置项
     * @param item 配置项
     * @param value 配置项值
     */
    protected void addConfigureItem(ODItem item, Object value) {
        if(value == null) {
            ODConfiguration.printError(ODError.ERROR_MISS_ITEM, item, false);
        } else {
            ODConfiguration.addConfigureItem(item, value);
        }
    }
}
