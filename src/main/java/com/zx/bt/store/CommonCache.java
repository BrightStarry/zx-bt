package com.zx.bt.store;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.config.Config;
import com.zx.bt.enums.CacheMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018/2/27 0027 16:58
 * 通用缓存
 */
@Slf4j
public class CommonCache<T> {

	public CommonCache(Config config, CacheMethodEnum cacheMethodEnum,int expireTime) {
		Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
				.initialCapacity(config.getMain().getSendCacheLen())
				.maximumSize(config.getMain().getSendCacheLen());
		if (CacheMethodEnum.AFTER_ACCESS.equals(cacheMethodEnum))
			caffeine.expireAfterAccess(expireTime, TimeUnit.SECONDS);
		else if (CacheMethodEnum.AFTER_WRITE.equals(cacheMethodEnum))
			caffeine.expireAfterWrite(expireTime, TimeUnit.SECONDS);
		//传入缓存加载策略,key不存在时调用该方法返回一个value回去
		this.cache = caffeine.build(key -> null);
	}

	//创建缓存
	private final LoadingCache<String, T> cache;


	/**
	 * 获取数据
	 */
	public T get(String key) {
		return cache.getIfPresent(key);
	}

	/**
	 * 获取并删除
	 */
	public T getAndRemove(String key) {
		T result = cache.getIfPresent(key);
		if (result != null) {
			cache.invalidate(key);
		}
		return result;
	}

	/**
	 * 存入数据
	 */
	public void put(String key, T obj) {
		cache.put(key, obj);
	}

	/**
	 * 删除数据
	 */
	public void remove(String key) {
		cache.invalidate(key);
	}


	/**
	 * get_peers发送信息
	 * 该类被保存在该缓存中
	 */
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	@Data
	public static class GetPeersSendInfo {
		private String infoHash;
	}
}
