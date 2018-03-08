package com.zx.bt.socket.processor;

import com.zx.bt.dto.MessageInfo;
import com.zx.bt.dto.Peer;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.repository.NodeRepository;
import com.zx.bt.service.InfoHashService;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.task.FetchMetadataByPeerTask;
import com.zx.bt.task.FindNodeTask;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.socket.Sender;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * GET_PEERS 回复 处理器
 */
@Slf4j
@Component
public class GetPeersResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[GET_PEERS_RECEIVE]";

	private final List<RoutingTable> routingTables;
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final InfoHashRepository infoHashRepository;
	private final NodeRepository nodeRepository;
	private final FindNodeTask findNodeTask;
	private final Sender sender;
	private final InfoHashService infoHashService;
	private final FetchMetadataByPeerTask fetchMetadataByPeerTask;

	public GetPeersResponseUDPProcessor(List<RoutingTable> routingTables,
										CommonCache<CommonCache.GetPeersSendInfo> getPeersCache,
										InfoHashRepository infoHashRepository, NodeRepository nodeRepository, FindNodeTask findNodeTask, Sender sender, InfoHashService infoHashService, FetchMetadataByPeerTask fetchMetadataByPeerTask) {
		this.routingTables = routingTables;
		this.getPeersCache = getPeersCache;
		this.infoHashRepository = infoHashRepository;
		this.nodeRepository = nodeRepository;
		this.findNodeTask = findNodeTask;
		this.sender = sender;
		this.infoHashService = infoHashService;
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
	}

	@Override
	boolean process1(ProcessObject processObject) {
			MessageInfo messageInfo = processObject.getMessageInfo();
			Map<String, Object> rawMap = processObject.getRawMap();
			InetSocketAddress sender = processObject.getSender();
			int index = processObject.getIndex();
			RoutingTable routingTable = routingTables.get(index);

			//查询缓存
			CommonCache.GetPeersSendInfo getPeersSendInfo = getPeersCache.get(messageInfo.getMessageId());
			//查询rMap,此处rMap不可能不存在
			Map<String, Object> rMap = BTUtil.getParamMap(rawMap, "r", "");
			//缓存过期，则不做任何处理了
			if (getPeersSendInfo == null) {
	//			log.info("{}发送者:{},消息id:{},该任务已经过期.", LOG, sender, messageInfo.getMessageId(), rMap);
				return true;
			}


			byte[] id = BTUtil.getParamString(rMap, "id", "GET_PEERS-RECEIVE,找不到id参数.map:" + rMap).getBytes(CharsetUtil.ISO_8859_1);
			//如果返回的是nodes
			if (rMap.get("nodes") != null) {
				List<Node> nodeList = BTUtil.getNodeListByRMap(rMap);
				//如果nodes为空
				if (CollectionUtils.isEmpty(nodeList)) {
	//				log.info("{}发送者:{},info_hash:{},消息id:{},返回nodes为空.", LOG, sender, getPeersSendInfo.getInfoHash(), messageInfo.getMessageId());
					routingTable.delete(id);
					return true;
				}
				//向新节点发送消息
				nodeList.forEach(item -> this.sender.findNode(item.toAddress(), nodeIds.get(index), BTUtil.generateNodeIdString(),index));
				//将消息发送者加入路由表.
				routingTable.put(new Node(id, sender, NodeRankEnum.GET_PEERS_RECEIVE.getCode()));
	//                    log.info("{}GET_PEERS-RECEIVE,发送者:{},info_hash:{},消息id:{},返回nodes", LOG, sender, getPeersSendInfo.getInfoHash(), messageInfo.getMessageId());

				//取出未发送过请求的节点
				List<Node> unSentNodeList = nodeList.stream().filter(node -> !getPeersSendInfo.contains(node.getNodeIdBytes())).collect(Collectors.toList());
				//为空退出
				if (CollectionUtils.isEmpty(unSentNodeList)){
					log.info("{}发送者:{},info_hash:{},消息id:{},所有节点已经发送过请求.",LOG,sender,getPeersSendInfo.getInfoHash(),messageInfo.getMessageId());
					return true;
				}
				//未发送过请求的节点id
				List<byte[]> unSentNodeIdList = unSentNodeList.stream().map(Node::getNodeIdBytes).collect(Collectors.toList());
				//将其加入已发送队列
				getPeersSendInfo.put(unSentNodeIdList);
				//未发送过请求节点的地址
				List<InetSocketAddress> unSentAddressList = unSentNodeList.stream().map(Node::toAddress).collect(Collectors.toList());
				//批量发送请求
				this.sender.getPeersBatch(unSentAddressList, nodeIds.get(index),
						new String(CodeUtil.hexStr2Bytes(getPeersSendInfo.getInfoHash()), CharsetUtil.ISO_8859_1),
						messageInfo.getMessageId(),index);
			} else if (rMap.get("values") != null) {
				//如果返回的是values peer
				List<String> rawPeerList = BTUtil.getParamList(rMap, "values", "GET_PEERS-RECEIVE,找不到values参数.map:" + rawMap);
				if (CollectionUtils.isEmpty(rawPeerList)) {
	//				log.info("{}发送者:{},info_hash:{},消息id:{},返回peers为空:{}", LOG, sender, getPeersSendInfo.getInfoHash(), messageInfo.getMessageId(), rMap);
					routingTable.delete(id);
					return true;
				}

				List<Peer> peerList = new LinkedList<>();
				for (String rawPeer : rawPeerList) {
					//byte[6] 转 Peer
					Peer peer = new Peer(rawPeer.getBytes(CharsetUtil.ISO_8859_1));
					peerList.add(peer);
				}
				//将peers连接为字符串
				final StringBuilder peersInfoBuilder = new StringBuilder();
				peerList.forEach(peer -> peersInfoBuilder.append(peer.getIp()).append(":").append(peer.getPort()).append(";"));

				log.info("{}发送者:{},info_hash:{},消息id:{},返回peers:{}", LOG, sender, getPeersSendInfo.getInfoHash(), messageInfo.getMessageId(), peersInfoBuilder.toString());
				//清除该任务缓存 和 连接peer任务
				getPeersCache.remove(messageInfo.getMessageId());
				fetchMetadataByPeerTask.remove(getPeersSendInfo.getInfoHash());
				//入库
				infoHashService.saveInfoHash(getPeersSendInfo.getInfoHash(),peersInfoBuilder.toString());

				//节点入库
//				nodeRepository.save(new Node(null, BTUtil.getIpBySender(sender), sender.getPort()));
				routingTable.put(new Node(id, sender, NodeRankEnum.GET_PEERS_RECEIVE_OF_VALUE.getCode()));
				//并向该节点发送findNode请求
				findNodeTask.put(sender);

			}
			//否则是格式错误,不做任何处理
			return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.GET_PEERS.equals(processObject.getMessageInfo().getMethod()) && YEnum.RECEIVE.equals(processObject.getMessageInfo().getStatus());
	}
}
