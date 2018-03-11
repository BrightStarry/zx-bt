package com.zx.bt.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.enums.CacheMethodEnum;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.store.CommonCache;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * author:ZhengXing
 * datetime:2018-03-11 1:29
 * 注入bean
 */
@Configuration
public class BeanConfig {

    /**
     * es 客户端
     */
    @Bean
    public TransportClient transportClient(Config config) throws UnknownHostException {
        //此处可构造并传入多个es地址,也就是一整个集群的所有节点
        //构造一个地址对象,查看源码得知,ip使用4个字节的字节数组传入

        TransportAddress node = new TransportAddress(
                InetAddress.getByName(config.getEs().getIp()),config.getEs().getPort()
        );

        Settings settings = Settings.builder()
                .put("cluster.name",config.getEs().getClusterName())
//				.put("client.transport.sniff", true)
                .build();
        //如果settings为空,可以使用Settings.EMPTY
        //但是不传入settings,会无法访问
        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(node);
        return client;
    }

    /**
     * metadataService
     */
    @Bean
    public MetadataService metadataService(TransportClient transportClient, ObjectMapper objectMapper) {
        return new MetadataService(transportClient,objectMapper);
    }

    /**
     * 热度缓存器
     * 防止单个metadata热度上涨过快
     * 存入的key为edId, 值为空串
     */
    @Bean
    public CommonCache<String> hotCache(Config config) {
        return new CommonCache<>(
                CacheMethodEnum.AFTER_WRITE,
                config.getService().getHotCacheExpireSecond(),
                config.getService().getHotCacheSize());
    }

    /**
     * 404异常处理路径
     */
    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer(){
        return container -> {
            container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"));
        };
    }

}
