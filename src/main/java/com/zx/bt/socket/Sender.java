package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.dto.method.AnnouncePeer;
import com.zx.bt.dto.method.FindNode;
import com.zx.bt.dto.method.GetPeers;
import com.zx.bt.dto.method.Ping;
import com.zx.bt.entity.Node;
import com.zx.bt.exception.BTException;
import com.zx.bt.function.Pauseable;
import com.zx.bt.util.BeanUtil;
import com.zx.bt.util.Bencode;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * author:ZhengXing
 * datetime:2018-02-14 13:56
 * 发送工具类
 */
@Slf4j
@Component
public class Sender implements Pauseable {

    private  List<Channel> channels;
    private Bencode bencode;

    //get_peers请求发送锁
    private  final ReentrantLock getPeersLock = new ReentrantLock();
    private  Condition getPeersCondition;
    private  int getPeersPauseMS;
    /**
     * 使用channel发送消息
     */
    public  void writeAndFlush(byte[] bytes, InetSocketAddress address,int index) {
        if (!channels.get(index).isWritable()) {
            channels.get(index).close();
            throw new BTException("发送消息异常");
        }
        channels.get(index).writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes), address));
    }

    /**
     * 发送ping请求
     */
    public  void ping(InetSocketAddress address, String nodeID,int index) {
        Ping.Request request = new Ping.Request(nodeID);

        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }



    /**
     * 回复ping请求
     */
    public  void pingReceive(InetSocketAddress address, String nodeID,String messageId,int index) {
        Ping.Response response = new Ping.Response(nodeID, messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)), address,index);
    }

    /**
     * 发送find_node请求
     */
    @SneakyThrows
    public  void findNode(InetSocketAddress address, String nodeId,String targetNodeId,int index) {
        FindNode.Request request = new FindNode.Request(nodeId, targetNodeId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(request)), address,index);
    }



    /**
     * 回复find_node回复
     */
    public  void findNodeReceive(String messageId,InetSocketAddress address, String nodeId, List<Node> nodeList,int index) {
        FindNode.Response response = new FindNode.Response(nodeId, new String(Node.toBytes(nodeList), CharsetUtil.ISO_8859_1),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }



    /**
     * 回复announce_peer
     */
    public  void announcePeerReceive(String messageId,InetSocketAddress address, String nodeId,int index) {
        AnnouncePeer.Response response = new AnnouncePeer.Response(nodeId,messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 回复get_peers
     */
    public  void getPeersReceive(String messageId,InetSocketAddress address, String nodeId, String token, List<Node> nodeList,int index) {
        GetPeers.Response response = new GetPeers.Response(nodeId, token, new String(Node.toBytes(nodeList), CharsetUtil.ISO_8859_1),messageId);
        writeAndFlush(bencode.encode(BeanUtil.beanToMap(response)),address,index);
    }

    /**
     * 批量发送get_peers
     */
    public  void getPeersBatch(List<InetSocketAddress> addresses, String nodeId,String infoHash,String messageId,int index) {
        GetPeers.Request request = new GetPeers.Request(nodeId, infoHash,messageId);
        byte[] encode = bencode.encode(BeanUtil.beanToMap(request));
        for (InetSocketAddress address : addresses) {
            try {
                writeAndFlush(encode,address,index);
                pause(getPeersLock,getPeersCondition,getPeersPauseMS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("发送GET_PEERS,失败.e:{}",e.getMessage());
            }
        }
    }





    //初始化-setter等--------------------------------

    @Autowired
    public void init(Bencode bencode,Config config) {
        this.bencode = bencode;
        this.channels = new ArrayList<>(config.getMain().getPorts().size());
        //增加size到指定数量.以在setChannel方法中不越界
        for (int i = 0; i < config.getMain().getPorts().size(); i++) {
            this.channels.add(null);
        }
        this.getPeersCondition = this.getPeersLock.newCondition();
        this.getPeersPauseMS = config.getPerformance().getGetPeersRequestSendIntervalMs();
    }

    public  void setChannel(Channel channel,int index) {
        this.channels.set(index,channel);
    }
}
