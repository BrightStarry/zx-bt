package com.zx.bt.util;

import com.dampcake.bencode.Bencode;
import com.zx.bt.config.Config;
import com.zx.bt.dto.*;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.exception.BTException;
import com.zx.bt.socket.LogChannelFutureListener;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * author:ZhengXing
 * datetime:2018-02-14 13:56
 * 发送工具类
 */
@Slf4j
@Component
public class SendUtil {

    private static LogChannelFutureListener logChannelFutureListener;
    private static Channel channel;
    private static Bencode bencode;

    /**
     * 使用channel发送消息
     */
    public static void writeAndFlush(byte[] bytes, InetSocketAddress address) {
        if (!channel.isActive()) {
            channel.close();
            throw new BTException("发送消息异常");
        }
        channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes), address)).addListener(logChannelFutureListener);
    }

    /**
     * 发送ping请求
     */
    public static void ping(InetSocketAddress address, String nodeID) {
        Ping.Request request = new Ping.Request(nodeID);
        //存入缓存
        CacheUtil.put(request.getT(),new MessageInfo(MethodEnum.PING, YEnum.QUERY,request.getT()));
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address);
    }

    /**
     * 回复ping请求
     */
    public static void pingReceive(InetSocketAddress address, String nodeID,String messageId) {
        Ping.Response response = new Ping.Response(nodeID, messageId);
        log.info("发送PING-RECEIVE,对方地址:{}",address);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)), address);
    }

    /**
     * 发送find_node请求
     */
    public static void findNode(InetSocketAddress address, String nodeId,String targetNodeId) {
        FindNode.Request request = new FindNode.Request(nodeId, targetNodeId);
        //存入缓存
        CacheUtil.put(request.getT(),new MessageInfo(MethodEnum.FIND_NODE, YEnum.QUERY,request.getT()));
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address);
    }

    /**
     * 回复announce_peer
     */
    public static void announcePeerReceive(InetSocketAddress address,String nodeId) {
        AnnouncePeer.Response response = new AnnouncePeer.Response(nodeId);
        log.info("发送ANNOUNCE_PEER-RECEIVE,对方地址:{}",address);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address);
    }

    /**
     * 回复get_peers
     */
    public static void getPeersReceive(InetSocketAddress address, String nodeId, String token, String nodes) {
        GetPeers.Response response = new GetPeers.Response(nodeId, token, nodes);
        log.info("发送GET_PEERS-RECEIVE,对方地址:{}",address);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address);
    }






    //初始化-setter等--------------------------------

    @Autowired
    public void init(LogChannelFutureListener logChannelFutureListener, Bencode bencode) {
        SendUtil.logChannelFutureListener = logChannelFutureListener;
        SendUtil.bencode = bencode;
    }

    public static void setChannel(Channel channel) {
        SendUtil.channel = channel;
    }
}
