package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.exception.BTException;
import com.zx.bt.socket.processor.ProcessObject;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.Bencode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht服务端处理类
 */
@Slf4j
@ChannelHandler.Sharable//此处,该注解是为了重启时,不会报错,而非该对象可以被复用(因为重启时,上一个服务可能未完全停止,会报错)
public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final String LOG = "[DHT服务端处理类]-";

	//当前处理器针对的nodeId索引
	private final int index;

	private final Bencode bencode;
	private final Config config;
	private final UDPProcessorManager udpProcessorManager;
	private final Sender sender;



	public UDPServerHandler(int index, Bencode bencode, Config config, UDPProcessorManager udpProcessorManager,
							Sender sender) {
		this.index = index;
		this.bencode = bencode;
		this.config = config;
		this.udpProcessorManager = udpProcessorManager;
		this.sender = sender;
	}


	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("{}通道激活", LOG);
		//给发送器工具类的channel赋值
		this.sender.setChannel(ctx.channel(),this.index);
	}


	/**
	 * 接收到消息
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		byte[] bytes = getBytes(packet);
		InetSocketAddress sender = packet.sender();

		//解码为map
		Map<String, Object> map;
		try {
			map = bencode.decode(bytes, Map.class);
		} catch (BTException e) {
			log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage());
			return;
		} catch (Exception e) {
			log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage(), e);
			return;
		}

		//解析出MessageInfo
		MessageInfo messageInfo;
		try {
			messageInfo = BTUtil.getMessageInfo(map);
		} catch (BTException e) {
			log.error("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage());
			return;
		} catch (Exception e) {
			log.error("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage(), e);
			return;
		}

		udpProcessorManager.process(new ProcessObject(messageInfo, map, sender,this.index));

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
		log.error("{}索引:{},发生异常:{}", LOG, index,cause.getMessage());
		//这个巨坑..发生异常(包括我自己抛出来的)后,就关闭了连接,..
//        ctx.close();
	}
}
