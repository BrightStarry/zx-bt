package com.zx.bt.config;

import com.zx.bt.enums.CacheMethodEnum;
import com.zx.bt.store.CommonCache;
import com.zx.bt.util.Bencode;
import io.netty.util.CharsetUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author:ZhengXing
 * datetime:2018-02-17 13:57
 * 注入bean
 */
@Configuration
public class BeanConfig {
    /**
     * Bencode编解码工具类
     */
    @Bean
    public Bencode bencode() {
        return new Bencode();
    }

    /**
     * get_peers请求消息缓存
     */
    @Bean
    public CommonCache<CommonCache.GetPeersSendInfo> getPeersCache(Config config) {
        return new CommonCache<>(config, CacheMethodEnum.AFTER_WRITE,config.getMain().getGetPeersTaskExpireSecond());
    }
}
