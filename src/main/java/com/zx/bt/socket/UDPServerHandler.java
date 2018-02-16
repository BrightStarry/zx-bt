package com.zx.bt.socket;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.zx.bt.config.Config;
import com.zx.bt.dto.AnnouncePeer;
import com.zx.bt.dto.FindNode;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.entity.Node;
import com.zx.bt.entity.InfoHash;
import com.zx.bt.enums.YEnum;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.repository.NodeRepository;
import com.zx.bt.store.Table;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.SendUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht服务端处理类
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final String LOG = "[DHT服务端处理类]-";

    private final Bencode bencode;
    private final Config config;
    private final Table table;
    private final InfoHashRepository infoHashRepository;
    private final NodeRepository nodeRepository;

    public UDPServerHandler(Bencode bencode, Config config, Table table, InfoHashRepository infoHashRepository, NodeRepository nodeRepository) {
        this.bencode = bencode;
        this.config = config;
        this.table = table;
        this.infoHashRepository = infoHashRepository;
        this.nodeRepository = nodeRepository;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("{}通道激活", LOG);
        //给发送器工具类的channel赋值
        SendUtil.setChannel(ctx.channel());
    }


    /**
     * 接收到消息
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        byte[] bytes = getBytes(packet);
        InetSocketAddress sender = packet.sender();
        //解码为map
        Map<String, Object> map;
        try {
            map = this.bencode.decode(bytes, Type.DICTIONARY);
            log.info("{}消息解码成功.发送者:{},解码消息内容:{}", LOG, sender, map);
        } catch (Exception e) {
            log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage(), e);
            return;
        }

        //解析出MessageInfo
        MessageInfo messageInfo = BTUtil.getMessageInfo(map);
//        log.info("{}解析MessageInfo成功.发送者:{},MessageInfo:{}", LOG, sender, messageInfo);


        switch (messageInfo.getMethod()) {
            case PING:
                //如果是请求,进行回复
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    log.info("{}PING.发送者:{}", LOG, sender);
                    SendUtil.pingReceive(sender, config.getMain().getNodeId(), messageInfo.getMessageId());
                    break;
                }
                //如果是回复

                break;
            case FIND_NODE:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    FindNode.Response response = new FindNode.Response(config.getMain().getNodeId(), "");
//                    SendUtil.findNode();
                    break;
                }
                //如果是回复
                log.info("{}FIND_NODE-RECEIVE.发送者:{}", LOG, sender);
                //回复主体
                Map<String, Object> rMap = BTUtil.getParamMap(map, "r", "FIND_NODE,找不到r参数.map:" + map);
                byte[] nodesBytes = BTUtil.getParamString(rMap, "nodes", "FIND_NODE,找不到nodes参数.map:" + map).getBytes(CharsetUtil.ISO_8859_1);
                List<Node> nodeList = new LinkedList<>();
                for (int i = 0; i + Config.NODE_BYTES_LEN < nodesBytes.length; i += Config.NODE_BYTES_LEN) {
                    //byte[26] 转 Node
                    Node node = new Node(ArrayUtils.subarray(nodesBytes, i, i + Config.NODE_BYTES_LEN));
                    //存入节点
                    table.put(node);
                    nodeList.add(node);
                }
                //插入数据库
                nodeRepository.save(nodeList);
                break;

            case ANNOUNCE_PEER:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {

                    AnnouncePeer.RequestContent requestContent = new AnnouncePeer.RequestContent(map, sender.getPort());

                    log.info("{}ANNOUNCE_PEER.发送者:{},port:{},info_hash:{}", LOG, sender, requestContent.getPort(), requestContent.getInfo_hash());
                    //入库
                    infoHashRepository.save(new InfoHash("ANNOUNCE_PEER:" + sender.getHostName() + ":" + requestContent.getPort() + requestContent.getInfo_hash()));
                    //回复
                    SendUtil.announcePeerReceive(sender, config.getMain().getNodeId());
                    break;
                }
                //如果是回复

                break;

            case GET_PEERS:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    Map<String, Object> aMap = BTUtil.getParamMap(map, "a", "GET_PEERS,找不到a参数.map:" + map);
                    String info_hash = CodeUtil.bytes2HexStr(BTUtil.getParamString(aMap, "info_hash", "GET_PEERS,找不到info_hash参数.map:" + map).getBytes(CharsetUtil.ISO_8859_1));
                    log.info("{}GET_PEERS,获取到info_hash:{}", LOG, info_hash);
                    //入库
                    infoHashRepository.save(new InfoHash("GET_PEERS:" + "-" + info_hash));
                    SendUtil.getPeersReceive(sender, config.getMain().getNodeId(),
                            config.getMain().getToken(), "");

                    break;
                }
                //如果是回复
                break;
        }


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
        log.error("{}发生异常:{}", LOG, cause.getMessage(), cause);
        ctx.close();
    }
}
