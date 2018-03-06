package com.zx.bt.task;

import com.sun.javafx.binding.StringFormatter;
import com.zx.bt.config.Config;
import com.zx.bt.entity.InfoHash;
import com.zx.bt.entity.Metadata;
import com.zx.bt.factory.BootstrapFactory;
import com.zx.bt.util.Bencode;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.HttpClientUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * author:ZhengXing
 * datetime:2018/3/6 0006 14:46
 * 根据bep-009/bep-010协议获取metadata信息任务
 */
@Component
@Slf4j
public class FetchMetadataByPeerTask {
	private static final String LOG = "[FetchMetadataByPeerTask]";

	private final Config config;
	private final BootstrapFactory bootstrapFactory;
	private final Bencode bencode;

	public FetchMetadataByPeerTask(Config config, BootstrapFactory bootstrapFactory, Bencode bencode) {
		this.config = config;
		this.bootstrapFactory = bootstrapFactory;
		this.bencode = bencode;
		this.queue = new LinkedBlockingQueue<>(config.getPerformance().getFetchMetadataByPeerTaskQueueNum());
	}

	//等待连接peers的infoHash队列
	private BlockingQueue<InfoHash> queue;

	/**
	 * 从库中查询出若干有peers的infoHash
	 */
	private List<InfoHash> listInfoHash(int num) {
		return null;
	}


	/**
	 * 获取metadata任务
	 */
	@AllArgsConstructor
	private class FetchMetadataTask implements Callable<Metadata>{
		private InfoHash infoHash;
		@Override
		public Metadata call() throws Exception {

			return null;
		}
	}


	/**
	 * 消息处理类
	 */
	@Getter
	@NoArgsConstructor
	private class FetchMetadataHandler extends SimpleChannelInboundHandler<ByteBuf> {
		private String infoHashHexStr;
		private byte[] metadataBytes;
		@Override
		protected void messageReceived(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
			byte[] bytes = new byte[msg.readableBytes()];
			msg.readBytes(bytes);

			String messageStr = new String(bytes, CharsetUtil.ISO_8859_1);

			//收到握手消息回复
			if (bytes[0] == (byte) 19) {
				//发送扩展消息
				SendExtendMessage(ctx);
			}

			//如果收到的消息中包含ut_metadata,提取出ut_metadata的值
			String utMetadataStr = "ut_metadata";
			String metadataSizeStr = "metadata_size";
			if (messageStr.contains(utMetadataStr) && messageStr.contains(metadataSizeStr)) {
				sendMetadataRequest(ctx, messageStr, utMetadataStr, metadataSizeStr);
			}

			//如果是分片信息
			if (messageStr.contains("msg_type")) {
//				log.info("收到分片消息:{}", messageStr);
				fetchMetadataBytes(messageStr);
			}
		}

		/***
		 * 获取metadataBytes
		 */
		private void fetchMetadataBytes(String messageStr) {
			String resultStr = messageStr.substring(messageStr.indexOf("ee") + 2, messageStr.length());
			byte[] resultStrBytes = resultStr.getBytes(CharsetUtil.ISO_8859_1);
			if (metadataBytes != null) {
				metadataBytes = ArrayUtils.addAll(metadataBytes, resultStrBytes);
			}else{
				metadataBytes = resultStrBytes;
			}
		}

		/**
		 * 发送 metadata 请求消息
		 */
		private void sendMetadataRequest(ChannelHandlerContext ctx, String messageStr, String utMetadataStr, String metadataSizeStr) {
			int utMetadataIndex = messageStr.indexOf(utMetadataStr) + utMetadataStr.length() + 1;
			//ut_metadata值
			int utMetadataValue = Integer.parseInt(messageStr.substring(utMetadataIndex, utMetadataIndex + 1));
			int metadataSizeIndex = messageStr.indexOf(metadataSizeStr) + metadataSizeStr.length() + 1;
			String otherStr = messageStr.substring(metadataSizeIndex);
			//metadata_size值
			int metadataSize = Integer.parseInt(otherStr.substring(0, otherStr.indexOf("e")));
			//分块数
			int blockSum = (int) Math.ceil((double) metadataSize / Config.METADATA_PIECE_SIZE);
//				log.info("该种子metadata大小:{},分块数:{}",metadataSize,blockSum);

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
//					log.info("{}发送metadata请求消息:{}", infoHashHexStr, new String(metadataRequestBytes, CharsetUtil.ISO_8859_1));
			}
		}

		/**
		 * 发送扩展消息
		 * @param ctx
		 */
		private void SendExtendMessage(ChannelHandlerContext ctx) {
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
//			log.info("{}发送扩展消息:{}", infoHashHexStr, new String(extendMessageBytes, CharsetUtil.ISO_8859_1));
			ctx.channel().writeAndFlush(Unpooled.copiedBuffer(extendMessageBytes));
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			log.error("{}{}异常:{}", LOG,infoHashHexStr,cause.getMessage());
		}

		public FetchMetadataHandler(String infoHashHexStr) {
			this.infoHashHexStr = infoHashHexStr;
		}
	}

	/**
	 * 连接监听器
	 */
	@AllArgsConstructor
	private class ConnectListener implements ChannelFutureListener{
		private String infoHashHexStr;
		//自己的peerId,直接定义为和nodeId相同即可
		private byte[] selfPeerId;

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				//连接成功发送握手消息
				SendHandshakeMessage(future);
				return;
			}
			//如果失败 TODO
		}

		/**
		 * 发送握手消息
		 */
		private void SendHandshakeMessage(ChannelFuture future) {
			byte[] infoHash = CodeUtil.hexStr2Bytes(infoHashHexStr);
			byte[] sendBytes = new byte[68];
			System.arraycopy(Config.GET_METADATA_HANDSHAKE_PRE_BYTES, 0, sendBytes, 0, 28);
			System.arraycopy(infoHash, 0, sendBytes, 28, 20);
			System.arraycopy(selfPeerId, 0, sendBytes, 48, 20);
			future.channel().writeAndFlush(Unpooled.copiedBuffer(sendBytes));
		}
	}

	/**
	 * 通道初始化器
	 */
	@AllArgsConstructor
	private class CustomChannelInitializer extends ChannelInitializer {
		private final InetSocketAddress address;
		private String infoHashHexStr;
		//自己的peerId,直接定义为和nodeId相同即可
		private byte[] selfPeerId;
		@Override
		protected void initChannel(Channel ch) throws Exception {
			ch.pipeline().addLast(new FetchMetadataHandler(infoHashHexStr)).connect(address)
					.addListener(new ConnectListener(infoHashHexStr, selfPeerId));
		}
	}

}
