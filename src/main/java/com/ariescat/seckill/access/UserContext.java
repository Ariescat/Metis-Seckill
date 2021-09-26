package com.ariescat.seckill.access;

import com.ariescat.seckill.bean.User;

/**
 * 用于保存用户
 * 使用ThreadLocal保存用户，因为ThreadLocal是线程安全的，使用ThreadLocal可以保存当前线程持有的对象
 * 每个用户的请求对应一个线程，所以使用ThreadLocal以线程为键保存用户是合适的
 */
public class UserContext {

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user) {
        userHolder.set(user);
    }

    public static User getUser() {
        return userHolder.get();
    }

    public static void removeUser() {
        userHolder.remove();
    }

}
