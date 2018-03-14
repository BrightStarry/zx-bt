package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.NodeRankEnum;
import com.zx.bt.spider.enums.YEnum;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.task.FindNodeTask;
import com.zx.bt.spider.util.BTUtil;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * findNode 回复 处理器
 */
@Order(1)
@Slf4j
@Component
public class FindNodeResponseUDPProcessor extends UDPProcessor {
	private static final String LOG = "[FIND_NODE_RECEIVE]";

	private final  List<RoutingTable> routingTables;
	private final FindNodeTask findNodeTask;

	public FindNodeResponseUDPProcessor(List<RoutingTable> routingTables, FindNodeTask findNodeTask) {
		this.routingTables = routingTables;
		this.findNodeTask = findNodeTask;
	}

	@Override
	boolean process1(ProcessObject processObject) {
			//回复主体
			Map<String, Object> rMap = BTUtil.getParamMap(processObject.getRawMap(), "r", "FIND_NODE,找不到r参数.map:" + processObject.getRawMap());
			List<Node> nodeList = BTUtil.getNodeListByRMap(rMap);
			//为空退出
			if (CollectionUtils.isEmpty(nodeList)) return true;
			//去重
			Node[] nodes = nodeList.stream().distinct().toArray(Node[]::new);
			//将nodes加入发送队列
			for (Node node : nodes) {
				findNodeTask.put(node.toAddress());
			}
			byte[] id = BTUtil.getParamString(rMap, "id", "FIND_NODE,找不到id参数.map:" + processObject.getRawMap()).getBytes(CharsetUtil.ISO_8859_1);
			//将发送消息的节点加入路由表
			routingTables.get(processObject.getIndex()).put(new Node(id, processObject.getSender(), NodeRankEnum.FIND_NODE_RECEIVE.getCode()));
			return true;

	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.FIND_NODE.equals(processObject.getMessageInfo().getMethod()) && YEnum.RECEIVE.equals(processObject.getMessageInfo().getStatus());
	}


}
