package com.zx.bt.spider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.enums.CacheMethodEnum;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.socket.UDPServer;
import com.zx.bt.spider.socket.processor.UDPProcessor;
import com.zx.bt.spider.socket.processor.UDPProcessorManager;
import com.zx.bt.common.store.CommonCache;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.util.Bencode;
import io.netty.util.CharsetUtil;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-17 13:57
 * 注入bean
 */
@Configuration
public class BeanConfig {
	//避免netty冲突
	static {		System.setProperty("es.set.netty.runtime.available.processors", "false"); }

	/**
	 * 初始化config的nodeIds
	 */
	@Autowired
	public void initConfigNodeIds(Config config) {
		config.getMain().initNodeIds();
	}

	/**
	 * Bencode编解码工具类
	 */
	@Bean
	public Bencode bencode() {
		return new Bencode();
	}

	/**
	 * get_peers请求消息缓存
	 */
	@Bean
	public CommonCache<CommonCache.GetPeersSendInfo> getPeersCache(Config config) {


		return new CommonCache<>(
				CacheMethodEnum.AFTER_WRITE,
				config.getPerformance().getGetPeersTaskExpireSecond(),
				config.getPerformance().getDefaultCacheLen());
	}

	/**
	 * udp 处理器管理器
	 * 可通过See{@link org.springframework.core.annotation.Order}改变处理器顺序
	 */
	@Bean
	public UDPProcessorManager udpProcessorManager(List<UDPProcessor> udpProcessors) {
		UDPProcessorManager udpProcessorManager = new UDPProcessorManager();
		udpProcessors.forEach(udpProcessorManager::register);
		return udpProcessorManager;
	}

	/**
	 * 创建多个路由表
	 */
	@Bean
	public List<RoutingTable> routingTables(Config config) {
		List<Integer> ports = config.getMain().getPorts();
		List<RoutingTable> result = new ArrayList<>(ports.size());
		List<String> nodeIds = config.getMain().getNodeIds();
		for (int i = 0; i < ports.size(); i++) {
			result.add(new RoutingTable(config, nodeIds.get(i).getBytes(CharsetUtil.ISO_8859_1), ports.get(i)));
		}
		return result;
	}

	/**
	 * udp handler类
	 */
	@Bean
	public List<UDPServer.UDPServerHandler> udpServerHandlers(Bencode bencode, Config config,
															  UDPProcessorManager udpProcessorManager,
															  Sender sender) {
		int size = config.getMain().getNodeIds().size();
		List<UDPServer.UDPServerHandler> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			result.add(new UDPServer.UDPServerHandler(i, bencode, config, udpProcessorManager, sender));
		}
		return result;
	}

	/**
	 * es 客户端
	 */
	@Bean
	public TransportClient transportClient(Config config) throws UnknownHostException {
		//此处可构造并传入多个es地址,也就是一整个集群的所有节点
		//构造一个地址对象,查看源码得知,ip使用4个字节的字节数组传入

		TransportAddress node = new TransportAddress(
				InetAddress.getByName(config.getEs().getIp()),config.getEs().getPort()
		);

		Settings settings = Settings.builder()
				.put("cluster.name",config.getEs().getClusterName())
//				.put("client.transport.sniff", true)
				.build();
		//如果settings为空,可以使用Settings.EMPTY
		//但是不传入settings,会无法访问
		TransportClient client = new PreBuiltTransportClient(settings);
		client.addTransportAddress(node);
		return client;
	}

	/**
	 * metadataService
	 */
	@Bean
	public MetadataService metadataService(TransportClient transportClient, ObjectMapper objectMapper) {
		return new MetadataService(transportClient,objectMapper);
	}


}
