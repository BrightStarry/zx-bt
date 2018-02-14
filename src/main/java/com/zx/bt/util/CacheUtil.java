package com.zx.bt.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.dto.MessageInfo;

import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018-02-14 16:05
 * 缓存工具类
 */
public class CacheUtil {

    //创建缓存
    private static final LoadingCache<String, MessageInfo> cache = Caffeine.newBuilder()
            .initialCapacity(1024)
			.maximumSize(10240)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            //传入缓存加载策略,key不存在时调用该方法返回一个value回去
            //此处直接返回空
            .build(key -> null);


    /**
     * 获取数据
     */
    public static MessageInfo get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 存入数据
     */
    public static void put(String key, MessageInfo channelCache) {
        cache.put(key, channelCache);
    }

    /**
     * 删除数据
     */
    public static void remove(String key) {
        cache.invalidate(key);
    }
}
