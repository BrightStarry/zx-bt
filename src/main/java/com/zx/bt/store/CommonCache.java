package com.zx.bt.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.config.Config;
import com.zx.bt.enums.CacheMethodEnum;
import com.zx.bt.util.BTUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * author:ZhengXing
 * datetime:2018/2/27 0027 16:58
 * 通用缓存
 */
@Slf4j
public class CommonCache<T> {


	public CommonCache(CacheMethodEnum cacheMethodEnum, int expireTime, int capacity) {
		Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
				.initialCapacity(capacity / 2)
				.maximumSize(capacity);
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
	 * 长度
	 * ps:该长度不是强一致性的.
	 */
	public long size() {

		return cache.estimatedSize();
	}

	/**
	 * 判断值是否存在
	 */
	public boolean contain(T obj) {
		return cache.asMap().values().parallelStream().filter(item -> item.equals(obj)).count() > 0;
	}

	/**
	 * 判断某个属性是否存在, 自行传入方法
	 */
	public  boolean contain(Predicate<? super T> predicate) {
		return cache.asMap().values().parallelStream().filter(predicate).count() > 0;
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

		/**
		 * 已发送get_peers请求的nodeIds
		 */
		private List<byte[]> sentNodeIds = new LinkedList<>();

		/**
		 * 判断当前对象的sentNodeIds是否包含传入的nodeId
		 */
		public boolean contains(byte[] nodeId) {
			return sentNodeIds.contains(nodeId);
		}

		/**
		 * 将对象加入到sentNodeIds
		 */
		public GetPeersSendInfo put(List<byte[]> bytes) {
			if (CollectionUtils.isEmpty(bytes))
				return this;
			bytes.forEach(this::put);
			return this;
		}

		/**
		 * 将对象加入到sentNodeIds
		 */
		public GetPeersSendInfo put(byte[] bytes) {
			if(!sentNodeIds.contains(bytes))
				sentNodeIds.add(bytes);
			return this;
		}

		public GetPeersSendInfo(String infoHash) {
			this.infoHash = infoHash;
		}
	}



}
