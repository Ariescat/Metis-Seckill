package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BasePrefix;

public class SeckillOrderKey extends BasePrefix {

    public SeckillOrderKey(String prefix) {
        super(prefix);
    }

    public static SeckillOrderKey getSeckillOrderByUidGid = new SeckillOrderKey("seckill");
}
