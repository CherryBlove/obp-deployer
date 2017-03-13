package com.oceanbase.odeployer.common;

/**
 * Server的类型
 * <blockquote><pre>
 * 1. RS
 * 2. UPS
 * 3. MS
 * 4. LMS
 * 5. CS
 * </pre></blockquote>
 * @author lbz@lbzhong.com 2016/4/8
 * @since OD1.0
 */
public enum ODServerName {
    UNKNOWN,

    RS,
    UPS,
    MS,
    LMS, // listener ms
    CS;

    /** 转换类型 */
    public static ODServerName toServerType(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    /** Server缩写 */
    public String toShortName() {
        switch (this) {
            case RS:
                return "rs";
            case UPS:
                return "ups";
            case MS:
                return "ms";
            case LMS:
                return "lms";
            case CS:
                return "cs";
            default:
                return null;
        }
    }

    /** 默认全写名称 */
    public String toString() {
        switch (this) {
            case RS:
                return "rootserver";
            case UPS:
                return "updateserver";
            case MS:
                return "mergeserver";
            case LMS:
                return "mergeserver";
            case CS:
                return "chunkserver";
            default:
                return null;
        }
    }
}
