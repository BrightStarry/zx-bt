package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.factory.BootstrapFactory;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.Bencode;
import com.zx.bt.util.CodeUtil;
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
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-17 10:19
 * tcp连接客户端
 */
@Slf4j
@Component
public class TCPClient {

    private final Config config;
    private final BootstrapFactory bootstrapFactory;
    private final Bencode bencode;

    public TCPClient(Config config, BootstrapFactory bootstrapFactory, Bencode bencode) {
        this.config = config;
        this.bootstrapFactory = bootstrapFactory;
        this.bencode = bencode;
    }

    public void connection(InetSocketAddress address, String infoHashHexStr, byte[] peerId, Map<String, byte[]> result) {
        bootstrapFactory.build()
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
//                                .addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,Unpooled.copiedBuffer("ÿÿÿÿÿÿÿÿÿÿÿ".getBytes(CharsetUtil.ISO_8859_1))))
                                .addLast(new SimpleChannelInboundHandler<ByteBuf>() {

                                    @Override
                                    protected void messageReceived(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

                                        byte[] bytes = new byte[msg.readableBytes()];
                                        msg.readBytes(bytes);

                                        String messageStr = new String(bytes, CharsetUtil.ISO_8859_1);

//                                        StringBuilder sb = new StringBuilder("字节:");
//                                        sb.append("{");
//                                        for (int i = 0; i < bytes.length; i++) {
//                                            sb.append(bytes[i]).append(",");
//                                        }
//                                        sb.append("}");
//                                        log.info("{}消息字节:{}",infoHashHexStr,sb.toString());
//
                                        log.info("{}收到消息ISO:{}", infoHashHexStr, messageStr);

                                        //收到握手消息回复
                                        if (bytes[0] == (byte) 19) {
                                            //发送扩展消息
                                            Map<String, Object> extendMessageMap = new LinkedHashMap<>();
                                            Map<String, Object> extendMessageMMap = new LinkedHashMap<>();
                                            extendMessageMMap.put("ut_metadata", 1);
                                            extendMessageMap.put("m", extendMessageMMap);
                                            byte[] tempExtendBytes = bencode.encode(extendMessageMap);
                                            byte[] extendMessageBytes = new byte[tempExtendBytes.length + 6];
                                            extendMessageBytes[4] = 20;
                                            extendMessageBytes[5] = 0;
                                            byte[] lenBytes = CodeUtil.int2Bytes(tempExtendBytes.length + 2);
                                            System.arraycopy(lenBytes, 0, extendMessageBytes, 0, 4);
                                            System.arraycopy(tempExtendBytes, 0, extendMessageBytes, 6, tempExtendBytes.length);
                                            log.info("{}发送扩展消息:{}", infoHashHexStr, new String(extendMessageBytes, CharsetUtil.ISO_8859_1));
                                            ctx.channel().writeAndFlush(Unpooled.copiedBuffer(extendMessageBytes));
                                        }

                                        //如果收到的消息中包含ut_metadata,提取出ut_metadata的值
                                        String utMetadataStr = "ut_metadata";
                                        String metadataSizeStr = "metadata_size";
                                        if (messageStr.contains(utMetadataStr)) {
                                            int utMetadataIndex = messageStr.indexOf(utMetadataStr) + utMetadataStr.length() + 1;
                                            //ut_metadata值
                                            int utMetadataValue = Integer.parseInt(messageStr.substring(utMetadataIndex, utMetadataIndex + 1));
                                            int metadataSizeIndex = messageStr.indexOf(metadataSizeStr) + metadataSizeStr.length() + 1;
                                            String otherStr = messageStr.substring(metadataSizeIndex);
                                            //metadata_size值
                                            int metadataSize = Integer.parseInt(otherStr.substring(0, otherStr.indexOf("e")));
                                            //分块数
                                            int blockSum = (int) Math.ceil((double) metadataSize / Config.METADATA_PIECE_SIZE);
                                            log.info("该种子metadata大小:{},分块数:{}",metadataSize,blockSum);


                                            //发送metadata请求

                                            for (int i = 0; i < blockSum; i++) {
                                                Map<String, Object> metadataRequestMap = new LinkedHashMap<>();
                                                metadataRequestMap.put("msg_type", 0);
                                                metadataRequestMap.put("piece", i);
                                                byte[] metadataRequestMapBytes = bencode.encode(metadataRequestMap);
                                                byte[] metadataRequestBytes = new byte[metadataRequestMapBytes.length + 6];
                                                metadataRequestBytes[4] = 20;
                                                metadataRequestBytes[5] = (byte) utMetadataValue;
                                                byte[] lenBytes = CodeUtil.int2Bytes(metadataRequestMapBytes.length + 2);
                                                System.arraycopy(lenBytes, 0, metadataRequestBytes, 0, 4);
                                                System.arraycopy(metadataRequestMapBytes, 0, metadataRequestBytes, 6, metadataRequestMapBytes.length);
                                                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(metadataRequestBytes));
                                                log.info("{}发送metadata请求消息:{}", infoHashHexStr, new String(metadataRequestBytes, CharsetUtil.ISO_8859_1));
                                            }
                                        }

                                        //如果是分片信息
                                        if (messageStr.contains("total_size")) {
                                            log.info("收到分片消息:{}", messageStr);

                                            String resultStr = messageStr.substring(messageStr.indexOf("ee") + 2, messageStr.length());
                                            byte[] bytes1 = result.get(infoHashHexStr);
                                            if (bytes1 != null) {
                                                result.put(infoHashHexStr, ArrayUtils.addAll(bytes1, resultStr.getBytes(CharsetUtil.ISO_8859_1)));
                                            }else{
                                                result.put(infoHashHexStr, resultStr.getBytes(CharsetUtil.ISO_8859_1));
                                            }
                                        }
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        log.error("异常:{}", cause.getMessage(), cause);
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
        String a = "d6:lengthi347252558e4:name81:[ Torrent9.info ] American.Horror.Story.S06E10.FiNAL.FRENCH.HDTV.XViD-EXTREME.avi12:piece lengthi262144ee";
        Bencode bencode = new Bencode();
        Map decode = bencode.decode(a.getBytes(CharsetUtil.ISO_8859_1), Map.class);


    }

}
