package com.oceanbase.odeployer.util;

/**
 * 一对组合
 * @author lbz@lbzhong.com 2016/03/30
 * @param <T1>
 * @param <T2>
 * @since OD1.0
 */
public class Pair<T1, T2> {

    public final T1 first;
    
    public final T2 second;
    
    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public boolean equals(Object first, Object second) {
        return (this.first.equals(first) && this.second.equals(second));
    }

    @Override
    public String toString() {
        return "Pair[first=" + first.toString() + ",second=" + second.toString() + "]";
    }

}
