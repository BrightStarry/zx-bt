package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/2/27 0027 16:06
 * 发送get_peers请求,以获取目标主机
 */
@Component
@Slf4j
public class GetPeersTask {
	//(消息id,info_hash)缓存
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final RoutingTable routingTable;
	private final Config config;

	public GetPeersTask(CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, RoutingTable routingTable, Config config) {
		this.getPeersCache = getPeersCache;
		this.routingTable = routingTable;
		this.config = config;
	}


	/**
	 * 开始任务
	 */
	public void start(String infoHash) {
		//获取所有地址
		List<InetSocketAddress> addresses = new LinkedList<>();
		routingTable.loop(trieNode -> {
			Node[] nodes = trieNode.getNodes();
			for (Node node : nodes) {
				addresses.add(new InetSocketAddress(node.getIp(),node.getPort()));
			}
		});
		//消息id
		String messageId = BTUtil.generateMessageID();
		//存入缓存
		getPeersCache.put(messageId,new CommonCache.GetPeersSendInfo(messageId));
		//批量发送
		SendUtil.getPeersBatch(addresses, config.getMain().getNodeId(), infoHash,messageId);

	}

}
