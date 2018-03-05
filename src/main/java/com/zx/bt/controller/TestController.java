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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final  List<Integer> ports;
	private final Config config;

	public TestController(List<RoutingTable> routingTables, FindNodeTask findNodeTask,
						  CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, GetPeersTask getPeersTask, Config config) {
		this.routingTables = routingTables;
		this.findNodeTask = findNodeTask;
		this.getPeersCache = getPeersCache;
		this.getPeersTask = getPeersTask;
		this.config = config;
		this.ports = config.getMain().getPorts();
	}


	@RequestMapping("/stat")
	public Map<String, Object> stat() {
		Map<String, Object> result = new HashMap<>();
		result.put("findNde队列", findNodeTask.size());
		result.put("getPeers缓存",  getPeersCache.size());
		result.put("getPeers队列",  getPeersTask.size());
		HashMap<String, Object> port = new HashMap<>();
		for (int i = 0; i < routingTables.size(); i++) {
			port.put(String.valueOf(ports.get(i)), routingTables.get(i).size());
		}
		result.put("端口信息",port);
		return result;
	}

}
