package com.ariescat.seckill.access;

import com.ariescat.seckill.redis.key.base.BaseKeyPrefix;

public class AccessKey extends BaseKeyPrefix {

    private AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    // 可灵活设置过期时间
    public static AccessKey withExpire(int expireSeconds) {
        return new AccessKey(expireSeconds, "access");
    }

}
