package com.zx.bt.socket;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.zx.bt.config.Config;
import com.zx.bt.dto.FindNode;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.dto.Node;
import com.zx.bt.enums.YEnum;
import com.zx.bt.store.Table;
import com.zx.bt.util.BTUtil;
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

import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht服务端处理类
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class DHTServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final String LOG = "[DHT服务端处理类]-";

    private final Bencode bencode;
    private final Config config;
    private final Table table;

    public DHTServerHandler(Bencode bencode, Config config, Table table) {
        this.bencode = bencode;
        this.config = config;
        this.table = table;
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

        //解码为map
        Map<String, Object> map = null;
        try {
            map = this.bencode.decode(bytes, Type.DICTIONARY);
        } catch (Exception e) {
            log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, packet.sender(), e.getMessage(), e);
            return;
        }
        log.info("{}2-消息解码成功.发送者:{},解码消息内容:{}", LOG, packet.sender(), map);

        //解析出MessageInfo
        MessageInfo messageInfo = null;
        try {
            messageInfo = BTUtil.getMessageInfo(map);
        } catch (Exception e) {
            log.error("{}解析MessageInfo异常.发送者:{}.异常:{}", LOG, packet.sender(), e.getMessage(), e);
            return;
        }
        log.info("{}3-解析MessageInfo成功.发送者:{},MessageInfo:{}", LOG, packet.sender(), messageInfo);


        switch (messageInfo.getMethod()) {
            case PING:
                //如果是请求,进行回复
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    SendUtil.pingReceive(packet.sender(), config.getMain().getNodeId(), messageInfo.getMessageId());
                } else {
                    //如果是回复

                }
                break;
            case FIND_NODE:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    new FindNode.Response(config.getMain().getNodeId(), "");
                }else{
                    //如果是回复
                    Object r = map.get("r");
                    if(r == null){
                        log.error("{}FIND_NODE,找不到r参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    Map<String, Object> rMap = (Map<String, Object>) r;
                    Object nodes = rMap.get("nodes");
                    if(nodes == null){
                        log.error("{}FIND_NODE,找不到nodes参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    byte[] bytes1 = ((String) nodes).getBytes(CharsetUtil.ISO_8859_1);
                    for (int i = 0; i < bytes1.length / 26; i++) {
                        //nodeId
                        byte[] nodeIdBytes = ArrayUtils.subarray(bytes1, i*26, i*26 + 20);
                        String nodeId = new String(nodeIdBytes,CharsetUtil.ISO_8859_1);

                        //ip
                        byte[] ipBytes = ArrayUtils.subarray(bytes1, i * 26 + 20, i * 26 + 24);
                        String ip = String.join(".", Integer.toString(ipBytes[0] & 0xFF) ,Integer.toString(ipBytes[1] & 0xFF)
                                ,Integer.toString(ipBytes[2] & 0xFF) ,Integer.toString(ipBytes[3] & 0xFF));

                        //port
                        byte[] portBytes = ArrayUtils.subarray(bytes1, i * 26 + 24, i * 26 + 26);
                        Integer port = portBytes[1] & 0xFF | (portBytes[0] & 0xFF) << 8;

                        Node node = new Node(nodeId, ip, port);
                        log.info("第{}个node信息:{}",i,node);
                        //存入节点
                        table.put(node);
                    }

                }
                break;

            case ANNOUNCE_PEER:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    Object aObj = map.get("a");
                    if(aObj == null){
                        log.error("{}ANNOUNCE_PEER,找不到a参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    Map<String, Object> aMap = (Map<String, Object>) aObj;
                    Object iObj = aMap.get("info_hash");
                    if (iObj == null) {
                        log.error("{}ANNOUNCE_PEER,找不到info_hash参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    String info_hash = (String) iObj;
                    log.info("{}ANNOUNCE_PEER获取到info_hash:{}",LOG,info_hash);
                    //回复
                    SendUtil.announcePeerReceive(packet.sender(),config.getMain().getNodeId());

                }else {
                    //如果是回复
                }
                break;

            case GET_PEERS:
                //如果是请求
                if (messageInfo.getStatus().equals(YEnum.QUERY)) {
                    Object aObj = map.get("a");
                    if(aObj == null){
                        log.error("{}GET_PEERS,找不到a参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    Map<String, Object> aMap = (Map<String, Object>) aObj;
                    Object iObj = aMap.get("info_hash");
                    if (iObj == null) {
                        log.error("{}ANNOUNCE_PEER,找不到info_hash参数.发送者:{}.", LOG, packet.sender());
                        return;
                    }
                    String info_hash = (String) iObj;
                    log.info("{}ANNOUNCE_PEER获取到info_hash:{}",LOG,info_hash);
                    SendUtil.getPeersReceive(packet.sender(),config.getMain().getNodeId(),
                            config.getMain().getToken(),"");

                }else {
                    //如果是回复
                }
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

        log.info("{}1-收到消息,发送者:{},未解码消息内容:{}", LOG, packet.sender(), new String(bytes,CharsetUtil.ISO_8859_1));
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
