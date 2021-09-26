package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BaseKeyPrefix;

public class SeckillOrderKeyPrefix extends BaseKeyPrefix {

    public SeckillOrderKeyPrefix(String prefix) {
        super(prefix);
    }

    public static SeckillOrderKeyPrefix getSeckillOrderByUidGid = new SeckillOrderKeyPrefix("seckill");
}
