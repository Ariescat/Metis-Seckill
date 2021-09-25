package com.ariescat.seckill.access;

import com.ariescat.seckill.redis.key.base.BasePrefix;

public class AccessKey extends BasePrefix {

	private AccessKey(int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}
	
	public static AccessKey withExpire(int expireSeconds) {
		return new AccessKey(expireSeconds, "access");
	}
	
}
