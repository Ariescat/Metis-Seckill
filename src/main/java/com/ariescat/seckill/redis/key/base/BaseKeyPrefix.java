package com.ariescat.seckill.redis.key.base;

public abstract class BaseKeyPrefix implements KeyPrefix {

    private final int expireSeconds;

    private final String prefix;

    public BaseKeyPrefix(String prefix) {
        this(0, prefix);//默认0代表永不过期
    }

    public BaseKeyPrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }

    @Override
    public int expireSeconds() {
        return expireSeconds;
    }

    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();//拿到参数类类名
        return className + ":" + prefix + ":";
    }
}
