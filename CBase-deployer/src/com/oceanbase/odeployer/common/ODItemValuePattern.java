package com.oceanbase.odeployer.common;

import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置项的值格式
 * @author lbz@lbzhong.com 2016/4/6
 * @since OD1.0
 */
public class ODItemValuePattern {

    /** first:引用的配置项, second: 取值类型 */
    public List<Pair<ODItem, ODValue>> valuePatterns = new ArrayList<>();

    public String toString() {
        String str = "<";
        for(Pair<ODItem, ODValue> p: valuePatterns) {
            ODItem it = p.first;
            if(it != null) {
                String name = it.toString();
                int pos = name.lastIndexOf(".");
                if(pos > 0) {
                    str += name.substring(pos + 1, name.length()) + "(" + p.second.getPattern() + "),";
                }
            } else {
                str += p.second.getPattern() + ",";
            }
        }
        str = ODUtil.removeLastChar(str);
        str += ">";
        return str;
    }

    /**
     * 将字符串转换为ValuePattern
     * @param item 配置项
     * @param pattern 赋值格式字符串
     * @return Pattern
     */
    public static ODItemValuePattern toValuePattern(ODItem item, String pattern) {
        ODItemValuePattern valuePattern = new ODItemValuePattern();
        pattern = pattern.replace("<", "").replace(">", "");
        String[] patterns = ODUtil.split(pattern, ",");
        for(String p: patterns) {
            if(p.startsWith("$")) { // 若引用了其他配置项
                if(item.isHidden()) { // 不允许H_类型的配置项的Pattern再引用其他配置项
                    ODLogger.error(item.toString() + " should not use $H_ITEM in pattern!", (new Throwable()).getStackTrace());
                } else {
                    p = p.replace("$", "");
                    ODItem it = ODItem.valueOfWithH(p);
                    if(it != ODItem.UNKNOWN) {
                        if(!p.startsWith("H_")) {
                            ODLogger.error(p + " should not be used in pattern!", (new Throwable()).getStackTrace());
                        } else {
                            if(it.getValueCount() == 1) {
                                String vp = it.getValuePattern().toString();
                                valuePattern.valuePatterns.add(new Pair<>(it, ODValue.patternToValue(vp)));
                            } else {
                                ODLogger.error("the value count of '" +it + "' used in the pattern of '"+ item.toString() +"' should be 1!",
                                        (new Throwable()).getStackTrace());
                            }
                        }
                    } else {
                        ODLogger.error(p + " is undefined!", (new Throwable()).getStackTrace());
                    }
                }
            } else {
                valuePattern.valuePatterns.add(new Pair<ODItem, ODValue>(null, ODValue.patternToValue(p)));
            }
        }
        return valuePattern;
    }

}
