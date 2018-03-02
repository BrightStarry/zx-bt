package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.exception.BTException;
import com.zx.bt.factory.BootstrapFactory;
import com.zx.bt.util.CodeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

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

	public TCPClient(Config config, BootstrapFactory bootstrapFactory) {
		this.config = config;
		this.bootstrapFactory = bootstrapFactory;
	}

	public void connection(InetSocketAddress address,String infoHashHexStr,byte[] peerId) {
		bootstrapFactory.build()
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline()
								.addLast(new SimpleChannelInboundHandler<ByteBuf>() {

							@Override
							public void channelActive(ChannelHandlerContext ctx) throws Exception {
								byte[] infoHash = CodeUtil.hexStr2Bytes(infoHashHexStr);
								byte[] sendBytes = new byte[68];
								System.arraycopy(Config.GET_METADATA_HANDSHAKE_PRE_BYTES,0,sendBytes,0,28);
								System.arraycopy(infoHash,0,sendBytes,28,20);
								System.arraycopy(peerId,0,sendBytes,48,20);
								ctx.channel().writeAndFlush(Unpooled.copiedBuffer(sendBytes));
							}

							@Override
							protected void messageReceived(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
								log.info("收到消息");
								byte[] bytes = new byte[msg.readableBytes()];
								msg.readBytes(bytes);

								byte[] b = new byte[]{1,};
								StringBuilder sb = new StringBuilder("字节:");
								sb.append("{");
								for (int i = 0; i < bytes.length; i++) {
									sb.append(bytes[i]).append(",");
								}
								sb.append("}");
								System.out.println(sb);

								//协议长度
								int protocolLen = bytes[0] & 0xff;
								//协议字节
								byte[] protocolBytes = ArrayUtils.subarray(bytes, 1, 28);
								//协议字符
								String protocol = new String(protocolBytes, CharsetUtil.ISO_8859_1);
								String protocol1 = new String(protocolBytes, CharsetUtil.UTF_8);

								//info_hash字节
								byte[] infoHashBytes = ArrayUtils.subarray(bytes, 28, 48);
								//info_hash字符
								String infoHashHexStr = CodeUtil.bytes2HexStr(infoHashBytes);

								//peerId字节
								byte[] peerIdBytes = ArrayUtils.subarray(bytes, 48, 68);
								//peerId字符
								String peerId = new String(protocolBytes, CharsetUtil.ISO_8859_1);
								String peerId1 = new String(protocolBytes, CharsetUtil.UTF_8);



								System.out.println(new String(bytes, CharsetUtil.ISO_8859_1));
							}
						});
					}
				})
				.connect(address);
	}


	public static void main(String[] args) {
		byte[] bytes = new byte[]{19,66,105,116,84,111,114,114,101,110,116,32,112,114,111,116,111,99,111,108,0,0,0,0,0,16,0,5,-81,71,107,106,19,-121,-1,-51,-21,127,74,-82,5,41,-103,106,-20,-67,127,91,45,66,84,55,97,51,83,45,71,-83,79,13,7,-103,30,-44,88,-34,98,86,0,0,0,-22,20,0,100,49,58,101,105,48,101,52,58,105,112,118,52,52,58,114,77,-78,-29,49,50,58,99,111,109,112,108,101,116,101,95,97,103,111,105,49,101,49,58,109,100,49,49,58,117,112,108,111,97,100,95,111,110,108,121,105,51,101,49,49,58,108,116,95,100,111,110,116,104,97,118,101,105,55,101,49,50,58,117,116,95,104,111,108,101,112,117,110,99,104,105,52,101,49,49,58,117,116,95,109,101,116,97,100,97,116,97,105,50,101,54,58,117,116,95,112,101,120,105,49,101,49,48,58,117,116,95,99,111,109,109,101,110,116,105,54,101,101,49,51,58,109,101,116,97,100,97,116,97,95,115,105,122,101,105,49,48,54,52,53,48,101,49,58,112,105,52,54,57,51,51,101,52,58,114,101,113,113,105,50,53,53,101,49,58,118,49,55,58,66,105,116,84,111,114,114,101,110,116,32,55,46,49,48,46,51,50,58,121,112,105,53,56,52,48,50,101,54,58,121,111,117,114,105,112,52,58,115,-57,-79,-69,101,0,0,2,-103,5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-17,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,127,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-5,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,127,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-73,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-65,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-9,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-67,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-2,0,0,0,5,4,0,0,15,113,0,0,0,5,4,0,0,20,102,0,0,0,5,4,0,0,4,-92,0,0,0,5,4,0,0,10,-79,0,0,0,5,4,0,0,5,-34,0,0,0,5,};

		//协议长度
		int protocolLen = bytes[0] & 0xff;
		//协议字节
		byte[] protocolBytes = ArrayUtils.subarray(bytes, 1, 28);
		//协议字符
		String protocol = new String(protocolBytes, CharsetUtil.ISO_8859_1);
		String protocol1 = new String(protocolBytes, CharsetUtil.UTF_8);

		//info_hash字节
		byte[] infoHashBytes = ArrayUtils.subarray(bytes, 28, 48);
		//info_hash字符
		String infoHashHexStr = CodeUtil.bytes2HexStr(infoHashBytes);

		//peerId字节
		byte[] peerIdBytes = ArrayUtils.subarray(bytes, 48, 68);
		//peerId字符
		String peerId = new String(protocolBytes, CharsetUtil.ISO_8859_1);
		String peerId1 = new String(protocolBytes, CharsetUtil.UTF_8);

		//20 http://bittorrent.org/beps/bep_0010.html
		byte[] last = ArrayUtils.subarray(bytes, 68, bytes.length);
		System.out.println("last:" + new String(last,CharsetUtil.ISO_8859_1));

//		System.out.println("all:" + new String(bytes, CharsetUtil.ISO_8859_1));

	}

}
