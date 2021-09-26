package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BaseKeyPrefix;

public class UserKeyPrefix extends BaseKeyPrefix {

    public static final int TOKEN_EXPIRE = 3600 * 24 * 2; // 默认两天

    /**
     * 防止被外面实例化
     */
    private UserKeyPrefix(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 需要缓存的字段
     */
    public static UserKeyPrefix getUserByToken = new UserKeyPrefix(TOKEN_EXPIRE, "token");
    public static UserKeyPrefix getUserById = new UserKeyPrefix(0, "id");

}
