package com.zx.bt.config;

import com.zx.bt.enums.CacheMethodEnum;
import com.zx.bt.socket.processor.UDPProcessor;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.store.CommonCache;
import com.zx.bt.util.Bencode;
import io.netty.util.CharsetUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
        return new CommonCache<>(
                CacheMethodEnum.AFTER_WRITE,
                config.getPerformance().getGetPeersTaskExpireSecond(),
                config.getPerformance().getDefaultCacheLen());
    }

    /**
     * udp 处理器管理器
     * 可通过See{@link org.springframework.core.annotation.Order}改变处理器顺序
     */
    @Bean
    public UDPProcessorManager udpProcessorManager(List<UDPProcessor> udpProcessors) {
        UDPProcessorManager udpProcessorManager = new UDPProcessorManager();
        udpProcessors.forEach(udpProcessorManager::register);
        return udpProcessorManager;
    }
}
