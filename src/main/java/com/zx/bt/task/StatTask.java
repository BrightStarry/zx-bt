package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/3/2 0002 14:16
 * 统计任务
 */
@Component
@Slf4j
public class StatTask {

	private final List<RoutingTable> routingTables;
	private final FindNodeTask findNodeTask;
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final GetPeersTask getPeersTask;

	public StatTask(List<RoutingTable> routingTables, FindNodeTask findNodeTask,
						CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, GetPeersTask getPeersTask) {
		this.routingTables = routingTables;
		this.findNodeTask = findNodeTask;
		this.getPeersCache = getPeersCache;
		this.getPeersTask = getPeersTask;
	}

	/**
	 * 状态提示
	 * 每x秒执行一次
	 */
	@Scheduled(cron = "0/10 * * * * ? ")
	public void status() {
		log.info("[状态报告]findNde队列:{},getPeers缓存:{},getPeers队列:{}",findNodeTask.size(),getPeersCache.size(),getPeersTask.size());
		for (int i = 0; i < routingTables.size(); i++) {
			log.info("[状态报告]索引:{},routingTable长度:{}",i,routingTables.get(i).size());
		}
	}


}
