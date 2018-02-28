package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.InfoHashTypeEnum;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.store.CommonCache;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.SendUtil;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018/2/27 0027 16:06
 * 发送get_peers请求,以获取目标主机
 */
@Component
@Slf4j
public class GetPeersTask {
	private static final String LOG = "[GetPeersTask]";


	//(消息id,info_hash)缓存
	private final CommonCache<CommonCache.GetPeersSendInfo> getPeersCache;
	private final RoutingTable routingTable;
	private final Config config;
	private final ExecutorService service;
	private final InfoHashRepository infoHashRepository;
	private final ReentrantLock lock;

	public GetPeersTask(CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, RoutingTable routingTable,
						Config config, InfoHashRepository infoHashRepository) {
		this.getPeersCache = getPeersCache;
		this.routingTable = routingTable;
		this.config = config;
		this.infoHashQueue = new LinkedBlockingQueue<>(config.getPerformance().getGetPeersTaskInfoHashQueueLen());
		this.infoHashRepository = infoHashRepository;
		this.service = Executors.newSingleThreadExecutor();
		this.lock = new ReentrantLock();
	}

	//info_hash等待队列
	private final BlockingQueue<String> infoHashQueue;

	/**
	 * 入队
	 */
	public void put(String infoHashHexStr) {
		//去重
		if (infoHashRepository.countByInfoHashAndType(infoHashHexStr, InfoHashTypeEnum.ANNOUNCE_PEER.getCode()) > 0 ||
				infoHashQueue.parallelStream().filter(item -> item.equals(infoHashHexStr)).count() > 0 ||
				getPeersCache.isExist(new CommonCache.GetPeersSendInfo(infoHashHexStr)))
			return;
		infoHashQueue.offer(infoHashHexStr);
	}


	/**
	 * 任务线程
	 */
	public void start() {
		service.execute(() -> {
			Condition condition = lock.newCondition();
			long taskNum;
			while (true) {
				//如果当前查找任务过多. 暂停30s再继续
				if((taskNum = getPeersCache.size()) > config.getPerformance().getGetPeersTaskConcurrentNum()){
					try {
						log.info("{}当前任务数过多:{},暂停获取新任务线程30s.",LOG,taskNum);
						lock.lock();
						condition.await(20, TimeUnit.SECONDS);
						continue;
					} catch (Exception e){
						//..不可能发生
					}finally {
						lock.unlock();
					}
				}
				//从队列中获取
				String infoHash = null;
				try {
					infoHash = infoHashQueue.poll(2, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					//..不可能发生
				}
				//开启新任务
				if(infoHash != null)
					start(infoHash);
				else{
					log.error("{}长时间没有get_peers任务入队...",LOG);
				}
			}
		});
	}



	/**
	 * 开始任务
	 */
	private void start(String infoHashHexStr) {
		//获取最近的8个地址
		List<Node> nodeList = routingTable.getForTop8(CodeUtil.hexStr2Bytes(infoHashHexStr));
		List<InetSocketAddress> addresses = nodeList.stream().map(node -> new InetSocketAddress(node.getIp(), node.getPort())).collect(Collectors.toList());
		//消息id
		String messageId = BTUtil.generateMessageID();
		log.info("{}开始新任务.消息Id:{},infoHash:{}",LOG,messageId,infoHashHexStr);
		//存入缓存
		getPeersCache.put(messageId,new CommonCache.GetPeersSendInfo(infoHashHexStr));
		//批量发送
		SendUtil.getPeersBatch(addresses, config.getMain().getNodeId(), new String(CodeUtil.hexStr2Bytes(infoHashHexStr), CharsetUtil.ISO_8859_1),messageId);
	}

}
