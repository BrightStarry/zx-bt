package com.zx.bt.controller;

import com.zx.bt.config.Config;
import com.zx.bt.service.MetadataService;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.InfoHashFilter;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.task.FetchMetadataByOtherWebTask;
import com.zx.bt.task.FetchMetadataByPeerTask;
import com.zx.bt.task.FindNodeTask;
import com.zx.bt.task.GetPeersTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/2/26 0026 17:25
 */
@RestController
@RequestMapping("/")
public class StatController {

	private final List<RoutingTable> routingTables;
	private final FindNodeTask findNodeTask;
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final GetPeersTask getPeersTask;
	private final  List<Integer> ports;
	private final Config config;
	private final FetchMetadataByPeerTask fetchMetadataByPeerTask;
	private final FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask;
	private final MetadataService metadataService;
	private final InfoHashFilter infoHashFilter;

	public StatController(List<RoutingTable> routingTables, FindNodeTask findNodeTask,
						  CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, GetPeersTask getPeersTask, Config config,
						  FetchMetadataByPeerTask fetchMetadataByPeerTask, FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask,
						  MetadataService metadataService, InfoHashFilter infoHashFilter) {
		this.routingTables = routingTables;
		this.findNodeTask = findNodeTask;
		this.getPeersCache = getPeersCache;
		this.getPeersTask = getPeersTask;
		this.config = config;
		this.ports = config.getMain().getPorts();
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
		this.fetchMetadataByOtherWebTask = fetchMetadataByOtherWebTask;
		this.metadataService = metadataService;
		this.infoHashFilter = infoHashFilter;
	}


	@RequestMapping("/stat")
	public Map<String, Object> stat() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put(config.getMain().getCountMetadataMinute() + "分钟内入库数",metadataService.countByMinute(config.getMain().getCountMetadataMinute()));
		result.put("findNde队列", findNodeTask.size());
		result.put("getPeers缓存",  getPeersCache.size());
		result.put("getPeers队列",  getPeersTask.size());
		result.put("fetchMetadataByPeerTask队列",  fetchMetadataByPeerTask.size());
		result.put("fetchMetadataByOtherWebTask队列",  fetchMetadataByOtherWebTask.size());
		result.put("InfoHashFilter长度",  infoHashFilter.size());
		HashMap<String, Object> port = new HashMap<>();
		for (int i = 0; i < routingTables.size(); i++) {
			port.put(String.valueOf(ports.get(i)), routingTables.get(i).size());
		}
		result.put("端口信息",port);
		result.put("当前配置", config);
		return result;
	}

}
