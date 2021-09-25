package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BasePrefix;

public class SeckillKey extends BasePrefix {

    private SeckillKey(String prefix) {
        super(prefix);
    }

    public static SeckillKey isGoodsOver = new SeckillKey("go");
}
