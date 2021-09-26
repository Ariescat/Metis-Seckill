package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BaseKeyPrefix;

public class SeckillKeyPrefix extends BaseKeyPrefix {

    private SeckillKeyPrefix(String prefix) {
        super(prefix);
    }

    private SeckillKeyPrefix(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static SeckillKeyPrefix isGoodsOver = new SeckillKeyPrefix("isGoodsOver");
    public static SeckillKeyPrefix seckillPath = new SeckillKeyPrefix(60, "seckillPath");
    // 验证码5分钟有效
    public static SeckillKeyPrefix seckillVerifyCode = new SeckillKeyPrefix(300, "seckillVerifyCode");
}
