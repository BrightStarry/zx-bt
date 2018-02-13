package com.zx.bt.util;

import com.zx.bt.socket.LogChannelFutureListener;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.http2.ByteUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author:ZhengXing
 * datetime:2018-02-13 13:42
 * 通用工具类
 */
@Slf4j
@Component
public class BTUtil {
    private static LogChannelFutureListener logChannelFutureListener;
    private static ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

    @Autowired
    public void init(LogChannelFutureListener logChannelFutureListener) {
        BTUtil.logChannelFutureListener = logChannelFutureListener;
    }

    /**
     * 使用channel发送消息
     */
    public static boolean writeAndFlush(Channel channel, String msg, InetSocketAddress address) {
        return writeAndFlush(channel, msg.getBytes(CharsetUtil.UTF_8), address);
    }

    /**
     * 使用channel发送消息
     */
    public static boolean writeAndFlush(Channel channel, byte[] bytes, InetSocketAddress address) {
        if(!channel.isActive()){
            channel.close();
            return false;
        }
        log.info("通道id:{},正在写入数据:{}",getChannelId(channel),new String(bytes));
        channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes),address)).addListener(logChannelFutureListener);
        return true;
    }
    /**
     * 从channel中获取到当前通道的id
     */
    public static String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

    /**
     * 生成一个随机的nodeID
     */
    public static byte[] generateNodeID() {
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            bytes[i] = (byte) threadLocalRandom.nextInt(256);
        }
        return CodeUtil.sha1(bytes);
    }

    public static void main(String[] args) {
        System.out.println(new String(generateNodeID()));

    }
}
