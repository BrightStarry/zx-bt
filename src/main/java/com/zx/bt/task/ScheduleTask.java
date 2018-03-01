package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018/2/26 0026 16:05
 * 定时任务
 */
@Component
@Slf4j
public class ScheduleTask {

	private final Config config;
	private final RoutingTable routingTable;

	public ScheduleTask(Config config, RoutingTable routingTable) {
		this.config = config;
		this.routingTable = routingTable;
	}

	/**
	 * 更新线程
	 * 每x分钟,更新一次要find_Node的目标节点
	 */
	@Scheduled(cron = "0 0/3 * * * ? ")
	public void updateTargetNodeId() {
		config.getMain().setTargetNodeId(BTUtil.generateNodeIdString());
		log.info("已更新TargetNodeId");
	}

	/**
	 * 每x分钟清理一次路由表
	 */
	@Scheduled(cron = "0 0/10 * * * ? ")
	public void clearRoutingTable() {
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
			byte[] nodeId = config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1);
			//等待清理队列
			List<byte[]> waitClearNodeIds = new LinkedList<>();


			routingTable.loop(trieNode ->{
				Node[] nodes = trieNode.getNodes();
				Node item;
				for (int i = 0; i < nodes.length; i++) {
					try {
						if(nodes[i] == null)
							continue;
						item = nodes[i];
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

	public static void main(String[] args) {
		Date a = new Date();
		Date b = DateUtils.addMinutes(new Date(), 15);
		long i = b.getTime() - a.getTime();
		long convert = TimeUnit.MINUTES.convert(i, TimeUnit.MILLISECONDS);
		System.out.println(convert);
	}
}
