package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.NodeRankEnum;
import com.zx.bt.spider.enums.YEnum;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.util.BTUtil;
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
	private static final String LOG = "[FIND_NODE]";

	private final List<RoutingTable> routingTables;
	private final Sender sender;

	public FindNodeRequestUDPProcessor(List<RoutingTable> routingTables, Sender sender) {
		this.routingTables = routingTables;
		this.sender = sender;
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
			this.sender.findNodeReceive(processObject.getMessageInfo().getMessageId(), processObject.getSender(),
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
