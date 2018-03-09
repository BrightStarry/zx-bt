package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.exception.BTException;
import com.zx.bt.socket.processor.ProcessObject;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.Bencode;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

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
    private final List<UDPServerHandler> udpServerHandlers;

    public UDPServer(Config config,List<UDPServerHandler> udpServerHandlers) {
        this.config = config;
        this.udpServerHandlers = udpServerHandlers;
    }

    /**
     * 异步开启
     */
    @SneakyThrows
    public void start() {
        List<Integer> ports = config.getMain().getPorts();
        for (int i = 0; i < ports.size(); i++) {
            final int index = i;
            new Thread(()->run(ports.get(index),index)).start();
        }

        //等待连接成功,获取到发送用的channel,再进行下一步
        Thread.sleep(5000);
    }

    /**
     * 保证UDP服务端开启,即使运行出错
     */
    private void run(int port,int index) {
        while (true){
            try {
                run1(port,index);
            } catch (Exception e) {
                log.error("{},端口:{},发生未知异常,准备重新启动.异常:{}",LOG,port,e.getMessage(),e);
            }
        }
    }


    /**
     * 启动UDP服务端,监听
     */
    private void run1(int port,int index) throws Exception {
        log.info("{}服务端启动...当前端口:{}",LOG,port);
        EventLoopGroup eventLoopGroup = null;
        try {
            //创建线程组 - 手动设置线程数,默认为cpu核心数2倍
            eventLoopGroup =  new NioEventLoopGroup(config.getPerformance().getUdpServerMainThreadNum());
            //创建引导程序
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioDatagramChannel.class)//通道类型也为UDP
                    .option(ChannelOption.SO_BROADCAST, true)//是广播,也就是UDP连接
                    .option(ChannelOption.SO_RCVBUF, 10000 * 1024)// 设置UDP读缓冲区为3M
                    .option(ChannelOption.SO_SNDBUF, 10000 * 1024)// 设置UDP写缓冲区为3M
                    .handler(udpServerHandlers.get(index));//配置的业务处理类
            bootstrap.bind(port).sync().channel().closeFuture().await();
        }finally {
            if(eventLoopGroup != null)
                eventLoopGroup.shutdownGracefully();
        }
    }

    /**
     * dht服务端处理类
     */
    @Slf4j
    @ChannelHandler.Sharable//此处,该注解是为了重启时,不会报错,而非该对象可以被复用(因为重启时,上一个服务可能未完全停止,会报错)
    public static class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private static final String LOG = "[DHT服务端处理类]-";

        //当前处理器针对的nodeId索引
        private final int index;

        private final Bencode bencode;
        private final Config config;
        private final UDPProcessorManager udpProcessorManager;
        private final Sender sender;



        public UDPServerHandler(int index, Bencode bencode, Config config, UDPProcessorManager udpProcessorManager,
                                Sender sender) {
            this.index = index;
            this.bencode = bencode;
            this.config = config;
            this.udpProcessorManager = udpProcessorManager;
            this.sender = sender;
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//            log.info("{}通道激活", LOG);
            //给发送器工具类的channel赋值
            this.sender.setChannel(ctx.channel(),this.index);
        }


        /**
         * 接收到消息
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            byte[] bytes = getBytes(packet);
            InetSocketAddress sender = packet.sender();

            //解码为map
            Map<String, Object> map;
            try {
                map = bencode.decode(bytes, Map.class);
            } catch (BTException e) {
                log.debug("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage());
                return;
            } catch (Exception e) {
                log.debug("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage(), e);
                return;
            }

            //解析出MessageInfo
            MessageInfo messageInfo;
            try {
                messageInfo = BTUtil.getMessageInfo(map);
            } catch (BTException e) {
                log.debug("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage());
                return;
            } catch (Exception e) {
                log.debug("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage(), e);
                return;
            }

            udpProcessorManager.process(new ProcessObject(messageInfo, map, sender,this.index));

        }

        /**
         * ByteBuf -> byte[]
         */
        private byte[] getBytes(DatagramPacket packet) {
            //读取消息到byte[]
            ByteBuf byteBuf = packet.content();
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            return bytes;
        }

        /**
         * 异常捕获
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("{}索引:{},发生异常:{}", LOG, index,cause.getMessage());
            //这个巨坑..发生异常(包括我自己抛出来的)后,就关闭了连接,..
//        ctx.close();
        }
    }
}
