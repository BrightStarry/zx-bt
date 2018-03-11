package com.zx.bt.spider.socket;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.factory.BootstrapFactory;
import com.zx.bt.spider.util.BTUtil;
import com.zx.bt.spider.util.Bencode;
import com.zx.bt.common.util.CodeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-17 10:19
 * tcp连接客户端
 * 废弃, 之前为了解析清楚 extend协议返回的消息. 逐个参数解析. 真正使用时,可取巧解析出 metadata info
 */
@Deprecated
@Slf4j
public class DeprecatedTCPClient {

    private final Config config;
    private final BootstrapFactory bootstrapFactory;

    public DeprecatedTCPClient(Config config, BootstrapFactory bootstrapFactory) {
        this.config = config;
        this.bootstrapFactory = bootstrapFactory;
    }

    public void connection(InetSocketAddress address, String infoHashHexStr, byte[] peerId) {
        bootstrapFactory.build()
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,Unpooled.copiedBuffer("ÿÿÿÿÿÿÿÿÿÿÿ".getBytes(CharsetUtil.ISO_8859_1))))
                                .addLast(new SimpleChannelInboundHandler<ByteBuf>() {

                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        super.channelRead(ctx, msg);
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                        log.info("收到:{}消息", infoHashHexStr, ctx.channel().id());
                                        byte[] bytes = new byte[msg.readableBytes()];
                                        msg.readBytes(bytes);

                                        //如果不是握手协议
                                        if (bytes[0] != (byte) 19) {
                                            //如果不包含msg_type
//                                            if (!new String(ArrayUtils.subarray(bytes, 0, 50), CharsetUtil.ISO_8859_1).contains("msg_type")) {
//                                                return;
//                                            }
                                            //收到metadata分片信息
                                            log.info("收到:{},metadata分片信息:{}", infoHashHexStr, new String(bytes, CharsetUtil.ISO_8859_1));
                                            return;
                                        }


                                        byte[] b = new byte[]{1,};
                                        StringBuilder sb = new StringBuilder("字节:");
                                        sb.append("{");
                                        for (int i = 0; i < bytes.length; i++) {
                                            sb.append(bytes[i]).append(",");
                                        }
                                        sb.append("}");
                                        System.out.println(sb);

                                        System.out.println("消息:" + new String(bytes, CharsetUtil.ISO_8859_1));
                                        //协议长度
                                        int protocolLen = bytes[0] & 0xff;
                                        //协议字节
                                        byte[] protocolBytes = ArrayUtils.subarray(bytes, 1, protocolLen + 1);
                                        //协议字符
                                        String protocol = new String(protocolBytes, CharsetUtil.ISO_8859_1);

                                        //版本号
                                        byte[] versionBytes = ArrayUtils.subarray(bytes, protocolLen + 1, protocolLen + 1 + 8);


                                        //info_hash字节
                                        byte[] infoHashBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8, protocolLen + 1 + 8 + 20);
                                        //info_hash字符
                                        String infoHashHexStr = CodeUtil.bytes2HexStr(infoHashBytes);

                                        //peerId字节
                                        byte[] peerIdBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8 + 20, protocolLen + 1 + 8 + 20 + 20);
                                        //peerId字符
                                        String peerId = new String(protocolBytes, CharsetUtil.ISO_8859_1);
                                        String peerId1 = new String(protocolBytes, CharsetUtil.UTF_8);


                                        byte[] otherBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8 + 20 + 20, bytes.length);
                                        //最多循环三次,尝试找到bencode编码的字符串消息
                                        String message = null;
                                        byte[] messageBytes = null;
                                        int index = 0;
                                        for (int i = 0; i < 3; i++) {
                                            //扩展消息长度(该长度包括 消息id和扩展消息id)
                                            byte[] messageLenBytes = ArrayUtils.subarray(otherBytes, index, index + 4);
                                            int messageLen = CodeUtil.bytes2Int(messageLenBytes);

                                            //消息id, 固定为20
                                            byte messageIdByte = otherBytes[index + 4];
                                            int messageId = messageIdByte & 0xff;

                                            //扩展消息id, 0:握手; >0:握手指定的扩展消息
                                            byte extendMessageIdByte = otherBytes[index + 4 + 1];
                                            int extendMessageId = extendMessageIdByte & 0xff;

                                            //消息主体
                                            messageBytes = ArrayUtils.subarray(otherBytes, index + 4 + 1 + 1, index + 4 + messageLen);
                                            message = new String(messageBytes, CharsetUtil.ISO_8859_1);
                                            //找了,退出循环
                                            if (message.substring(0, 1).equals("d") && message.substring(message.length() - 1).equals("e"))
                                                break;
                                            index += 4 + messageLen;
                                        }

                                        Bencode bencode = new Bencode();
                                        Map<String, Object> messageMap = bencode.decode(messageBytes, Map.class);
                                        log.info("消息:{},Map:{}", infoHashHexStr, messageMap);

//                                        if(!messageMap.containsKey("m") || !BTUtil.getParamMap(messageMap,"m","").containsKey("ut_metadata")){
                                        if (!messageMap.containsKey("metadata_size")) {
                                            log.info("该peer不支持bep-009协议.");
                                            return;
                                        }

                                        //总长度
                                        Integer metadataSize = BTUtil.getParamInteger(messageMap, "metadata_size", "metadata_size属性不存在");
                                        //总分块数
                                        int blockSum = (int) Math.ceil(metadataSize.doubleValue() / Config.METADATA_PIECE_SIZE);

                                        //发送若干次请求.请求metadata分块数据
                                        Map<String, Object> metadataRequest = new LinkedHashMap<>();
                                        metadataRequest.put("msg_type", 0);
                                        metadataRequest.put("piece", 0);
                                        //数据是 4字节的长度 + (byte)20(表示扩展消息) + (byte)2(表示ut_metadata)
                                        byte[] encodeBytes = bencode.encode(metadataRequest);
                                        byte[] metadataRequestBytes = new byte[encodeBytes.length + 4 + 1 + 1];
                                        System.arraycopy(encodeBytes, 0, metadataRequestBytes, 6, encodeBytes.length);
                                        System.arraycopy(CodeUtil.int2Bytes(encodeBytes.length + 1 + 1), 0, metadataRequestBytes, 0, 4);
                                        metadataRequestBytes[4] = 20;
                                        metadataRequestBytes[5] = 2;

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
                                        byte[] send = ArrayUtils.addAll(extendMetadataHeaderBytes, metadataRequestBytes);


                                        log.info("发送metadata请求:{}", new String(send, CharsetUtil.ISO_8859_1));
                                        ctx.channel().writeAndFlush(Unpooled.copiedBuffer(send));


                                    }



                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        log.error("异常:{}", cause.getMessage());
                                    }
                                });


                    }
                })
                .connect(address)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("连接成功");
                        byte[] infoHash = CodeUtil.hexStr2Bytes(infoHashHexStr);
                        byte[] sendBytes = new byte[68];
                        System.arraycopy(Config.GET_METADATA_HANDSHAKE_PRE_BYTES, 0, sendBytes, 0, 28);
                        System.arraycopy(infoHash, 0, sendBytes, 28, 20);
                        System.arraycopy(peerId, 0, sendBytes, 48, 20);
                        future.channel().writeAndFlush(Unpooled.copiedBuffer(sendBytes));
                    }
                });
    }


    public static void main(String[] args) {
//		byte[] bytes = new byte[]{19,66,105,116,84,111,114,114,101,110,116,32,112,114,111,116,111,99,111,108,0,0,0,0,0,16,0,5,-81,71,107,106,19,-121,-1,-51,-21,127,74,-82,5,41,-103,106,-20,-67,127,91,45,66,84,55,97,51,83,45,71,-83,79,13,7,-103,30,-44,88,-34,98,86,0,0,0,-22,20,0,100,49,58,101,105,48,101,52,58,105,112,118,52,52,58,114,77,-78,-29,49,50,58,99,111,109,112,108,101,116,101,95,97,103,111,105,49,101,49,58,109,100,49,49,58,117,112,108,111,97,100,95,111,110,108,121,105,51,101,49,49,58,108,116,95,100,111,110,116,104,97,118,101,105,55,101,49,50,58,117,116,95,104,111,108,101,112,117,110,99,104,105,52,101,49,49,58,117,116,95,109,101,116,97,100,97,116,97,105,50,101,54,58,117,116,95,112,101,120,105,49,101,49,48,58,117,116,95,99,111,109,109,101,110,116,105,54,101,101,49,51,58,109,101,116,97,100,97,116,97,95,115,105,122,101,105,49,48,54,52,53,48,101,49,58,112,105,52,54,57,51,51,101,52,58,114,101,113,113,105,50,53,53,101,49,58,118,49,55,58,66,105,116,84,111,114,114,101,110,116,32,55,46,49,48,46,51,50,58,121,112,105,53,56,52,48,50,101,54,58,121,111,117,114,105,112,52,58,115,-57,-79,-69,101,0,0,2,-103,5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-17,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,127,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,127,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-73,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-67,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,0,0,0,5,4,0,0,15,113,0,0,0,5,4,0,0,20,102,0,0,0,5,4,0,0,4,-92,0,0,0,5,4,0,0,10,-79,0,0,0,5,4,0,0,5,-34,0,0,0,5,};
        byte[] bytes = new byte[]{19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114, 111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1, -85, 51, 0, -98, 19, 10, -101, 62, -126, 76, 85, -96, -5, -81, -42, -87, 103, 108, 117, -104, 45, 66, 67, 48, 49, 52, 55, 45, 12, 15, 112, -124, 84, -86, -126, -34, -50, -30, 45, 41, 0, 0, 0, 3, 9, 61, 33, 0, 0, 0, 119, 20, 0, 100, 49, 58, 101, 105, 48, 101, 49, 58, 109, 100, 49, 49, 58, 117, 116, 95, 109, 101, 116, 97, 100, 97, 116, 97, 105, 49, 101, 54, 58, 117, 116, 95, 112, 101, 120, 105, 50, 101, 101, 49, 51, 58, 109, 101, 116, 97, 100, 97, 116, 97, 95, 115, 105, 122, 101, 105, 50, 51, 54, 49, 57, 101, 49, 58, 112, 105, 49, 53, 54, 52, 57, 101, 52, 58, 114, 101, 113, 113, 105, 53, 48, 101, 49, 58, 118, 49, 51, 58, 66, 105, 116, 67, 111, 109, 101, 116, 32, 49, 46, 52, 55, 54, 58, 121, 111, 117, 114, 105, 112, 52, 58, -73, -100, 103, -32, 101, 0, 0, 0, -109, 5, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0,};

        //协议长度
        int protocolLen = bytes[0] & 0xff;
        //协议字节
        byte[] protocolBytes = ArrayUtils.subarray(bytes, 1, protocolLen + 1);
        //协议字符
        String protocol = new String(protocolBytes, CharsetUtil.ISO_8859_1);

        //版本号
        byte[] versionBytes = ArrayUtils.subarray(bytes, protocolLen + 1, protocolLen + 1 + 8);


        //info_hash字节
        byte[] infoHashBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8, protocolLen + 1 + 8 + 20);
        //info_hash字符
        String infoHashHexStr = CodeUtil.bytes2HexStr(infoHashBytes);

        //peerId字节
        byte[] peerIdBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8 + 20, protocolLen + 1 + 8 + 20 + 20);
        //peerId字符
        String peerId = new String(protocolBytes, CharsetUtil.ISO_8859_1);
        String peerId1 = new String(protocolBytes, CharsetUtil.UTF_8);

        byte[] otherBytes = ArrayUtils.subarray(bytes, protocolLen + 1 + 8 + 20 + 20, bytes.length);
        //最多循环三次,尝试找到bencode编码的字符串消息
        String message = null;
        byte[] messageBytes = null;
        int index = 0;
        for (int i = 0; i < 3; i++) {
            //扩展消息长度(该长度包括 消息id和扩展消息id)
            byte[] messageLenBytes = ArrayUtils.subarray(otherBytes, index, index + 4);
            int messageLen = CodeUtil.bytes2Int(messageLenBytes);

            //消息id, 固定为20
            byte messageIdByte = otherBytes[index + 4];
            int messageId = messageIdByte & 0xff;

            //扩展消息id, 0:握手; >0:握手指定的扩展消息
            byte extendMessageIdByte = otherBytes[index + 4 + 1];
            int extendMessageId = extendMessageIdByte & 0xff;

            //消息主体
            messageBytes = ArrayUtils.subarray(otherBytes, index + 4 + 1 + 1, index + 4 + messageLen);
            message = new String(messageBytes, CharsetUtil.ISO_8859_1);

            index += 4 + messageLen;

            //找了,退出循环
            if (message.substring(0, 1).equals("d") && message.substring(message.length() - 1).equals("e"))
                break;

        }


        Bencode bencode = new Bencode();
        Map<String, Object> messageMap = bencode.decode(messageBytes, Map.class);
        System.out.println("消息Map:" + messageMap);

        if (!messageMap.containsKey("metadata_size")) {
            log.info("该peer不支持bep-009协议.");
            return;
        }

        //总长度
        Integer metadataSize = BTUtil.getParamInteger(messageMap, "metadata_size", "metadata_size属性不存在");
        //总分块数
        int blockSum = (int) Math.ceil(metadataSize.doubleValue() / Config.METADATA_PIECE_SIZE);


    }

}
