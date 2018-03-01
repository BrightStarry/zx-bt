package com.zx.bt.config;

import com.zx.bt.enums.CacheMethodEnum;
import com.zx.bt.socket.ProcessQueue;
import com.zx.bt.socket.UDPServerHandler;
import com.zx.bt.socket.processor.UDPProcessor;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.Bencode;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-17 13:57
 * 注入bean
 */
@Configuration
public class BeanConfig {

    /**
     * 初始化config的nodeIds
     */
    @Autowired
    public void initConfigNodeIds(Config config) {
        config.getMain().initNodeIds();
    }
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
    public List<CommonCache<CommonCache.GetPeersSendInfo>> getPeersCache(Config config) {
        int size = config.getMain().getPorts().size();
        List<CommonCache<CommonCache.GetPeersSendInfo>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new CommonCache<>(
                    CacheMethodEnum.AFTER_WRITE,
                    config.getPerformance().getGetPeersTaskExpireSecond(),
                    config.getPerformance().getDefaultCacheLen()));
        }
        return result;
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

    /**
     * 创建多个路由表
     */
    @Bean
    public List<RoutingTable> routingTables(Config config) {
        List<Integer> ports = config.getMain().getPorts();
        List<RoutingTable> result = new ArrayList<>(ports.size());
        List<String> nodeIds = config.getMain().getNodeIds();
        for (int i = 0; i < ports.size(); i++) {
            result.add(new RoutingTable(config, nodeIds.get(i).getBytes(CharsetUtil.ISO_8859_1), ports.get(i)));
        }
        return result;
    }

    /**
     * udp handler类
     */
    @Bean
    public List<UDPServerHandler> udpServerHandlers(Bencode bencode, Config config,
                                                    UDPProcessorManager udpProcessorManager, ProcessQueue processQueue) {
        int size = config.getMain().getNodeIds().size();
        List<UDPServerHandler> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new UDPServerHandler(i, bencode, config, udpProcessorManager, processQueue));
        }
        return result;
    }
}
