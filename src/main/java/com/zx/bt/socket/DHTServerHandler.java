package com.zx.bt.socket;

import com.zx.bt.util.BTUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht服务端处理类
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class DHTServerHandler extends SimpleChannelInboundHandler<DatagramPacket>{
    private static final String LOG = "[DHT服务端处理类]-";

    public static Channel channel;

    /**
     *
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("{}通道激活",LOG);

        if (channel == null) {
            channel = ctx.channel();
        }
    }


    /**
     * 接收到消息
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        //消息信息
        String message = packet.content().toString(CharsetUtil.UTF_8);
        log.info("{}收到消息,发送者:{},消息内容:{}",LOG,packet.sender(),message);
//        BTUtil.writeAndFlush(ctx.channel(), "服务端发送的消息", packet.sender());
    }

    /**
     * 异常捕获
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{}发生异常:{}",LOG,cause.getMessage(),cause);
        ctx.close();
    }
}
