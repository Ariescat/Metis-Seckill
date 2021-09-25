package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BasePrefix;

public class UserKey extends BasePrefix {

    public static final int TOKEN_EXPIRE = 3600 * 24 * 2; // 默认两天

    /**
     * 防止被外面实例化
     */
    private UserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 需要缓存的字段
     */
    public static UserKey getUserByToken = new UserKey(TOKEN_EXPIRE, "token");
    public static UserKey getUserById = new UserKey(0, "id");

}
