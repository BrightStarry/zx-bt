package com.zx.bt.socket;

import com.zx.bt.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:03
 * UDP服务器
 */
@Slf4j
@Component
public class UDPServer {
    private static final String LOG = "[DHT服务端]-";

    private final Config config;
    private final UDPServerHandler UDPServerHandler;

    public UDPServer(Config config, UDPServerHandler UDPServerHandler) {
        this.config = config;
        this.UDPServerHandler = UDPServerHandler;
    }

    /**
     * 异步开启
     */
    @SneakyThrows
    public void start() {
        new Thread(this::run).start();
        //等待连接成功,获取到发送用的channel,再进行下一步
        Thread.sleep(4000);
    }

    /**
     * 保证UDP服务端开启,即使运行出错
     */
    private void run() {
        while (true){
            try {
                run1(config.getMain().getPort());
            } catch (Exception e) {
                log.error("{},发生未知异常,准备重新启动.异常:{}",LOG,e.getMessage(),e);
            }
        }
    }


    /**
     * 启动UDP服务端,监听
     */
    private void run1(int port) throws Exception {
        log.info("{}服务端启动...",LOG);
        EventLoopGroup eventLoopGroup = null;
        try {
            //创建线程组 - 手动设置线程数,默认为cpu核心数2倍
            eventLoopGroup =  new NioEventLoopGroup(config.getPerformance().getUdpServerMainThreadNum());
            //创建引导程序
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioDatagramChannel.class)//通道类型也为UDP
//                    .option(ChannelOption.SO_BROADCAST, true)//是广播,也就是UDP连接
                    .option(ChannelOption.SO_RCVBUF, 10000 * 1024)// 设置UDP读缓冲区为3M
                    .option(ChannelOption.SO_SNDBUF, 10000 * 1024)// 设置UDP写缓冲区为3M
                    .handler(UDPServerHandler);//配置的业务处理类
            bootstrap.bind(port).sync().channel().closeFuture().await();
        }finally {
            if(eventLoopGroup != null)
                eventLoopGroup.shutdownGracefully();
        }
    }
}
