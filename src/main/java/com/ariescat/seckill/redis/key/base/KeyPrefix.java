package com.ariescat.seckill.redis.key.base;

/**
 * 缓冲key前缀
 */
public interface KeyPrefix {

    /**
     * 有效期
     */
    int expireSeconds();

    /**
     * 前缀
     */
    String getPrefix();
}
