package com.zx.bt.socket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht客户端处理类
 */
@ChannelHandler.Sharable
@Slf4j
@Component
@Deprecated
public class DHTClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final String LOG = "[DHT客户端处理类]-";


    /**
     * 接收到消息
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        //消息信息
        String message = packet.content().toString(CharsetUtil.UTF_8);
        log.info("{}收到消息,发送者:{},消息内容:{}", LOG, packet.sender(), message);
    }

    /**
     * 异常捕获
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{}发生异常:{}", LOG, cause.getMessage(), cause);
        ctx.close();
    }
}
