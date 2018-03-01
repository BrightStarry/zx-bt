package com.zx.bt.socket.processor;

import com.zx.bt.entity.Node;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.exception.BTException;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.SendUtil;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * findNode 回复 处理器
 */
@Slf4j
@Component
public class FindNodeResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[FIND_NODE_RECEIVE]";

	private final RoutingTable routingTable;

	public FindNodeResponseUDPProcessor(RoutingTable routingTable) {
		this.routingTable = routingTable;
	}

	@Override
	boolean process1(ProcessObject processObject) {
		//回复主体
		Map<String, Object> rMap = BTUtil.getParamMap(processObject.getRawMap(), "r", "FIND_NODE,找不到r参数.map:" + processObject.getRawMap());
		List<Node> nodeList = BTUtil.getNodeListByRMap(rMap);
		//为空退出
		if (CollectionUtils.isEmpty(nodeList)){
			return true;
		}
		byte[] id = BTUtil.getParamString(rMap, "id", "FIND_NODE,找不到id参数.map:" + processObject.getRawMap()).getBytes(CharsetUtil.ISO_8859_1);
		//将发送消息的节点加入路由表
		routingTable.put(new Node(id, processObject.getSender(), NodeRankEnum.FIND_NODE_RECEIVE.getCode()));
		//向这些节点发送find_node请求.
		nodeList.forEach(item -> SendUtil.findNode(item.toAddress(), processObject.getConfig().getMain().getNodeId(), BTUtil.generateNodeIdString()));
//                log.info("{}FIND_NODE-RECEIVE.发送者:{},返回节点:{}", LOG, sender,nodeList);
		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.FIND_NODE.equals(processObject.getMessageInfo().getMethod()) && YEnum.RECEIVE.equals(processObject.getMessageInfo().getStatus());
	}
}
