package com.zx.bt.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018-02-14 16:05
 * 缓存工具类
 */
@Component
public class CacheUtil {
    private static Config config;

    @Autowired
    public void init(Config config) {
        CacheUtil.config = config;
        cache = Caffeine.newBuilder()
                .initialCapacity(config.getMain().getSendCacheLen())
                .maximumSize(config.getMain().getSendCacheLen())
                .expireAfterAccess(config.getMain().getSendCacheExpireSecond(), TimeUnit.SECONDS)
                //传入缓存加载策略,key不存在时调用该方法返回一个value回去
                //此处直接返回空
                .build(key -> null);

    }

    //创建缓存
    private static LoadingCache<String, MessageInfo> cache;


    /**
     * 获取数据
     */
    public static MessageInfo get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 获取并删除
     */
    public static MessageInfo getAndRemove(String key) {
        MessageInfo result = cache.getIfPresent(key);
        if(result != null){
            cache.invalidate(key);
        }
        return result;
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
