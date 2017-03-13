package com.oceanbase.odeployer.common;

/**
 * 通用section的类型
 * @author lbz@lbzhong.com 2016/06/03
 * @since OD1.0
 */
public enum ODSectionType {

    /*
     * a.commonItem1 = value
     * a.commonItem2 = value
     * a.commonItem3 = value
     */
    SECTION0,
    /*
     * a.nameItem   = value
     * a.commonItem1 = value
     * a.commonItem2 = value
     * a.closeItem
     */
    SECTION1,
    
    /*
     * a.nameItem   = value
     * [a.commonItem1 = value](可选项)
     * [a.commonItem2 = value](可选项)
     * a.actionItem1 = value
     * a.actionItem2 = value
     * a.closeItem
     */
    SECTION2
}
