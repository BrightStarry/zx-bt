package com.zx.bt.socket.processor;

import com.zx.bt.entity.Node;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.SendUtil;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * findNode 请求 处理器
 */
@Slf4j
@Component
public class FindNodeRequestUDPProcessor extends UDPProcessor{

	private final List<RoutingTable> routingTables;

	public FindNodeRequestUDPProcessor(List<RoutingTable> routingTables) {
		this.routingTables = routingTables;
	}

	@Override
	boolean process1(ProcessObject processObject) {
		//截取出要查找的目标nodeId和 请求发送方nodeId
		Map<String, Object> aMap = BTUtil.getParamMap(processObject.getRawMap(), "a", "FIND_NODE,找不到a参数.map:" + processObject.getRawMap());
		byte[] targetNodeId = BTUtil.getParamString(aMap, "target", "FIND_NODE,找不到target参数.map:" + processObject.getRawMap())
				.getBytes(CharsetUtil.ISO_8859_1);
		byte[] id = BTUtil.getParamString(aMap, "id", "FIND_NODE,找不到id参数.map:" + processObject.getRawMap()).getBytes(CharsetUtil.ISO_8859_1);
		//查找
		List<Node> nodes = routingTables.get(processObject.getIndex()).getForTop8(targetNodeId);
//                    log.info("{}FIND_NODE.发送者:{},返回的nodes:{}", LOG, sender,nodes);
		SendUtil.findNodeReceive(processObject.getMessageInfo().getMessageId(), processObject.getSender(),
				nodeIds.get(processObject.getIndex()), nodes,processObject.getIndex());
		//操作路由表
		routingTables.get(processObject.getIndex()).put(new Node(id, processObject.getSender(), NodeRankEnum.FIND_NODE.getCode()));
		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.FIND_NODE.equals(processObject.getMessageInfo().getMethod()) && YEnum.QUERY.equals(processObject.getMessageInfo().getStatus());
	}
}
