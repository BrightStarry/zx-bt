package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.store.RoutingTable;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018/3/2 0002 14:10
 * 路由表清理任务
 *
 * 废弃: 想了下, 满了之后,直接替换掉rank值过小的节点即可.
 */
@Deprecated
//@Component
@Slf4j
public class ClearRoutingTableTask {

	private final List<RoutingTable> routingTables;
	private final List<String> nodeIds;
	private final Config config;

	public ClearRoutingTableTask(List<RoutingTable> routingTables, Config config) {
		this.routingTables = routingTables;
		this.nodeIds = config.getMain().getNodeIds();
		this.config = config;
	}

	/**
	 * 每x分钟清理一次路由表
	 */
	@Scheduled(cron = "0 0/10 * * * ? ")
	public void clearRoutingTable() {
		for (int i = 0; i < nodeIds.size(); i++) {
			RoutingTable routingTable = routingTables.get(i);
			try {
				log.info("[清理节点任务]任务开始.");
				//当前时间毫秒数
				final long now = new Date().getTime();
				//rank超过该值,超时时长延长
				final int minRank = NodeRankEnum.ANNOUNCE_PEER.getCode();
				//普通节点超时时间
				int generalNodeTimeoutMinute = config.getPerformance().getGeneralNodeTimeoutMinute();
				//rank值较大节点超时时间
				int specialNodeTimeoutMinute = config.getPerformance().getSpecialNodeTimeoutMinute();
				//自己的节点Id
				byte[] nodeId = nodeIds.get(i).getBytes(CharsetUtil.ISO_8859_1);
				//等待清理队列
				List<byte[]> waitClearNodeIds = new LinkedList<>();


				routingTable.loop(trieNode ->{
					Node[] nodes = trieNode.getNodes();
					Node item;
					for (int j = 0; j < nodes.length; j++) {
						try {
							if(nodes[j] == null)
								continue;
							item = nodes[j];
							//未活动时长
							long unActiveMinute = TimeUnit.MINUTES.convert(now - item.getLastActiveTime().getTime(), TimeUnit.MILLISECONDS);
							//true(超时,并且不是自己这个节点):清理该节点
							if ((item.getRank() > minRank && unActiveMinute > specialNodeTimeoutMinute
									|| item.getRank() < minRank && unActiveMinute > generalNodeTimeoutMinute)
									&&  !Arrays.equals(nodeId,item.getNodeIdBytes()))
								//加入待清理队列
								waitClearNodeIds.add(item.getNodeIdBytes());
						} catch (Exception e) {
							log.error("[清理节点任务]异常.e:{}",e.getMessage(),e);
						}
					}
				});

				//遍历.清理
				for (byte[] item : waitClearNodeIds) {
					try {
						routingTable.delete(item);
					} catch (Exception e) {
						log.error("[清理节点任务]异常.e:{}",e.getMessage(),e);
					}
				}
				log.info("[清理节点任务]任务结束.共清理节点:{}个",waitClearNodeIds.size());
			} catch (Exception e) {
				log.error("[清理节点任务]异常.e:{}",e.getMessage(),e);
			}
		}

	}

}
