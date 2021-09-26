package com.ariescat.seckill.redis.key;

import com.ariescat.seckill.redis.key.base.BaseKeyPrefix;

public class GoodsKeyPrefix extends BaseKeyPrefix {

    private GoodsKeyPrefix(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static GoodsKeyPrefix getGoodsList = new GoodsKeyPrefix(60, "gl");
    public static GoodsKeyPrefix getGoodsDetail = new GoodsKeyPrefix(60, "gd");
    public static GoodsKeyPrefix getGoodsStock = new GoodsKeyPrefix(0, "gs");
}
