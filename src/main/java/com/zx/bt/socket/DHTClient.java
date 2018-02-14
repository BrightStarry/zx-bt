package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.util.BootstrapFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * author:ZhengXing
 * datetime:2018-02-13 13:08
 * DHT客户端
 * 用于发送DHT协议对应请求
 */
@Component
@Slf4j
@Deprecated
public class DHTClient {
    private static final String LOG = "[DHT客户端]-";
    private final BootstrapFactory bootstrapFactory;
    private final Config config;

    public DHTClient(BootstrapFactory bootstrapFactory, Config config) {
        this.bootstrapFactory = bootstrapFactory;
        this.config = config;
    }


    /**
     * 获取连接到目标服务器的通道
     */
    public Optional<Channel> getChannel(InetSocketAddress address, ChannelHandler handler) {
        Channel channel = null;
        try {
            //绑定自己的一个随机端口,向目标的UDP服务器发送消息
            channel = bootstrapFactory.build().handler(handler).bind(config.getMain().getPort()).sync().channel();
        } catch (Exception e) {
            log.error("{}连接到目标:{}失败.异常:{}",LOG,address,e.getMessage(),e);
        }
        return Optional.ofNullable(channel);
    }

}
