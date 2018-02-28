package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.exception.BTException;
import com.zx.bt.factory.BootstrapFactory;
import com.zx.bt.util.CodeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Arrays;

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
									protected void messageReceived(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
										log.info("收到消息");
										byte[] bytes = new byte[msg.readableBytes()];
										msg.readBytes(bytes);
										String s = new String(bytes, CharsetUtil.ISO_8859_1);
										System.out.println(s);

									}
								});
					}
				})
				.connect(address)
				.addListener((ChannelFutureListener) future -> {
					if(!future.isSuccess()){
						throw new BTException("连接失败");
					}
					byte[] infoHash = CodeUtil.hexStr2Bytes(infoHashHexStr);
					byte[] sendBytes = new byte[68];
					System.arraycopy(Config.GET_METADATA_HANDSHAKE_PRE_BYTES,0,sendBytes,0,28);
					System.arraycopy(infoHash,0,sendBytes,28,20);
					System.arraycopy(peerId,0,sendBytes,48,20);
					future.channel().writeAndFlush(sendBytes);
				});
	}


}
