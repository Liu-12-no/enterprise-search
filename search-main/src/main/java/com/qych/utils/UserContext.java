package com.qych.utils;

/**
 * 用户上下文工具：利用 ThreadLocal 实现请求线程内的数据共享与隔离
 */
public class UserContext {

    //利用 ThreadLocal 为每个线程提供独立的变量副本
    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();
    

    //将用户id存入当前线程
    public static void setUserId(Long userId){
        THREAD_LOCAL.set(userId);
    }

    //获取用户id
    public static Long getUserId(){
        return THREAD_LOCAL.get();
    }

    // 请求结束必须清理，防止内存泄露
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
