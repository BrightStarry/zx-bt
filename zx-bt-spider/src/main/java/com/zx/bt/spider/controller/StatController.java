package com.zx.bt.spider.controller;

import com.zx.bt.spider.config.Config;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.store.CommonCache;
import com.zx.bt.spider.dto.GetPeersSendInfo;
import com.zx.bt.spider.store.InfoHashFilter;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.task.FetchMetadataByOtherWebTask;
import com.zx.bt.spider.task.FetchMetadataByPeerTask;
import com.zx.bt.spider.task.FindNodeTask;
import com.zx.bt.spider.task.GetPeersTask;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * author:ZhengXing
 * datetime:2018/2/26 0026 17:25
 */
@RestController
@RequestMapping("/")
public class StatController {

	private final List<RoutingTable> routingTables;
	private final FindNodeTask findNodeTask;
	private final CommonCache<GetPeersSendInfo> getPeersCache;
	private final GetPeersTask getPeersTask;
	private final  List<Integer> ports;
	private final Config config;
	private final FetchMetadataByPeerTask fetchMetadataByPeerTask;
	private final FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask;
	private final MetadataService metadataService;
	private final InfoHashFilter infoHashFilter;

	public StatController(List<RoutingTable> routingTables, FindNodeTask findNodeTask,
						  CommonCache<GetPeersSendInfo> getPeersCache, GetPeersTask getPeersTask, Config config,
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
		result.put(config.getMain().getCountMetadataMinute() + "分钟内入库数",
				metadataService.countByCreateTimeGE(DateUtils.addMinutes(new Date(), -config.getMain().getCountMetadataMinute())));
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
		return result;
	}

}
