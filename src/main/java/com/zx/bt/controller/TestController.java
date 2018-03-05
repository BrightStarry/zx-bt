package com.zx.bt.controller;

import com.sun.javafx.binding.StringFormatter;
import com.zx.bt.config.Config;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.task.FindNodeTask;
import com.zx.bt.task.GetPeersTask;
import javafx.beans.binding.StringExpression;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/2/26 0026 17:25
 */
@RestController
@RequestMapping("/")
public class TestController {

	private final List<RoutingTable> routingTables;
	private final FindNodeTask findNodeTask;
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final GetPeersTask getPeersTask;
	private final List<String> nodeIds;
	private final Config config;

	public TestController(List<RoutingTable> routingTables, FindNodeTask findNodeTask,
						  CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, GetPeersTask getPeersTask, Config config) {
		this.routingTables = routingTables;
		this.findNodeTask = findNodeTask;
		this.getPeersCache = getPeersCache;
		this.getPeersTask = getPeersTask;
		this.config = config;
		this.nodeIds = config.getMain().getNodeIds();
	}

	private static final String format = "[状态报告]findNde队列:%d,getPeers缓存:%d,getPeers队列:%d\n\t";

	@RequestMapping("/stat")
	public String stat() {
		StringExpression format = StringFormatter.format(TestController.format, findNodeTask.size(), getPeersCache.size(), getPeersTask.size());
		StringBuilder sb = new StringBuilder(format.getValue());

		for (int i = 0; i < routingTables.size(); i++) {
			sb.append(StringFormatter.format("[状态报告]端口:%d,routingTable长度:%d", nodeIds.get(i), routingTables.get(i).size()).getValue()).append("\n\t");
		}
		return sb.toString();
	}

}
