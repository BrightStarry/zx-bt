package com.zx.bt.util;

import com.zx.bt.config.Config;
import com.zx.bt.dto.method.AnnouncePeer;
import com.zx.bt.dto.method.FindNode;
import com.zx.bt.dto.method.GetPeers;
import com.zx.bt.dto.method.Ping;
import com.zx.bt.entity.Node;
import com.zx.bt.exception.BTException;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * author:ZhengXing
 * datetime:2018-02-14 13:56
 * 发送工具类
 */
@Slf4j
@Component
public class SendUtil {

    private static List<Channel> channels;
    private static Bencode bencode;

    /**
     * 使用channel发送消息
     */
    public static void writeAndFlush(byte[] bytes, InetSocketAddress address,int index) {
        if (!channels.get(index).isWritable()) {
            channels.get(index).close();
            throw new BTException("发送消息异常");
        }
        channels.get(index).writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes), address));
    }

    /**
     * 发送ping请求
     */
    public static void ping(InetSocketAddress address, String nodeID,int index) {
        Ping.Request request = new Ping.Request(nodeID);

        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }



    /**
     * 回复ping请求
     */
    public static void pingReceive(InetSocketAddress address, String nodeID,String messageId,int index) {
        Ping.Response response = new Ping.Response(nodeID, messageId);
//        log.info("发送PING-RECEIVE,对方地址:{}",address);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)), address,index);
    }

    /**
     * 发送find_node请求
     */
    @SneakyThrows
    public static void findNode(InetSocketAddress address, String nodeId,String targetNodeId,int index) {
        FindNode.Request request = new FindNode.Request(nodeId, targetNodeId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }



    /**
     * 回复find_node回复
     */
    public static void findNodeReceive(String messageId,InetSocketAddress address, String nodeId, List<Node> nodeList,int index) {
        FindNode.Response response = new FindNode.Response(nodeId, new String(Node.toBytes(nodeList), CharsetUtil.ISO_8859_1),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }



    /**
     * 回复announce_peer
     */
    public static void announcePeerReceive(String messageId,InetSocketAddress address, String nodeId,int index) {
        AnnouncePeer.Response response = new AnnouncePeer.Response(nodeId,messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 回复get_peers
     */
    public static void getPeersReceive(String messageId,InetSocketAddress address, String nodeId, String token, List<Node> nodeList,int index) {
        GetPeers.Response response = new GetPeers.Response(nodeId, token, new String(Node.toBytes(nodeList), CharsetUtil.ISO_8859_1),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 批量发送get_peers
     */
    public static void getPeersBatch(List<InetSocketAddress> addresses, String nodeId,String infoHash,String messageId,int index) {
        GetPeers.Request request = new GetPeers.Request(nodeId, infoHash,messageId);
        byte[] encode = bencode.encode(BeanUtil.beanToMap(request));
        for (InetSocketAddress address : addresses) {
            try {
                writeAndFlush(encode,address,index);
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("发送GET_PEERS,失败.e:{}",e.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        Bencode bencode = new Bencode();
        //扩展消息头
        Map<String, Object> extendMetadataHeader = new LinkedHashMap<>();
        extendMetadataHeader.put("e", 0);
        extendMetadataHeader.put("complete_ago", 1);
        Map<String, Object> extendMetadataHeaderM = new LinkedHashMap<>();
        extendMetadataHeaderM.put("ut_metadata", 2);
        extendMetadataHeaderM.put("ut_pex", 1);

        extendMetadataHeader.put("m",extendMetadataHeaderM);
        extendMetadataHeader.put("reqq", 255);
        extendMetadataHeader.put("yourip", new String(CodeUtil.ip2Bytes("192.168.0.1"),CharsetUtil.ISO_8859_1));
        byte[] extendMetadataHeaderBytes = bencode.encode(extendMetadataHeader);
    }



    //初始化-setter等--------------------------------

    @Autowired
    public void init(Bencode bencode,Config config) {
        SendUtil.bencode = bencode;
        SendUtil.channels = new ArrayList<>(config.getMain().getPorts().size());
        //增加size到指定数量.以在setChannel方法中不越界
        for (int i = 0; i < config.getMain().getPorts().size(); i++) {
            channels.add(null);
        }
    }

    public static void setChannel(Channel channel,int index) {
        SendUtil.channels.set(index,channel);
    }
}
