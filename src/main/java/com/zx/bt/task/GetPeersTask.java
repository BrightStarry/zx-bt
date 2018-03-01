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
    private final List<RoutingTable> routingTables;
    private final Config config;
    private final ExecutorService service;
    private final InfoHashRepository infoHashRepository;
    private final ReentrantLock lock;
    private final Condition condition;
    private final List<String> nodeIds;

    public GetPeersTask(CommonCache<CommonCache.GetPeersSendInfo> getPeersCache, List<RoutingTable> routingTables,
                        Config config, InfoHashRepository infoHashRepository) {
        this.getPeersCache = getPeersCache;
        this.routingTables = routingTables;
        this.config = config;
        this.queue = new LinkedBlockingQueue<>(config.getPerformance().getGetPeersTaskInfoHashQueueLen());
        this.infoHashRepository = infoHashRepository;
        this.service = Executors.newFixedThreadPool(this.routingTables.size());
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.nodeIds = config.getMain().getNodeIds();

    }

    //info_hash等待队列
    private final BlockingQueue<String> queue;

    /**
     * 入队
     */
    public void put(String infoHashHexStr, int index) {
        //去重
        if (infoHashRepository.countByInfoHashAndType(infoHashHexStr, InfoHashTypeEnum.ANNOUNCE_PEER.getCode()) > 0 ||
                queue.parallelStream().filter(item -> item.equals(infoHashHexStr)).count() > 0 ||
                getPeersCache.isExist(new CommonCache.GetPeersSendInfo(infoHashHexStr)))
            return;
        queue.offer(infoHashHexStr);
    }

    /**
     * 长度
     */
    public int size() {
        return queue.size();
    }

    /**
     * 任务线程
     */
    public void start() {
        service.execute(() -> {
            int max = Integer.MAX_VALUE - 10000;
            int i = 0;
            int size = nodeIds.size();
            while (true) {
                //如果当前查找任务过多. 暂停30s再继续
                if ( getPeersCache.size() > config.getPerformance().getGetPeersTaskConcurrentNum()) {
                    log.info("{}当前任务数过多,暂停获取新任务线程30s.", LOG);
                    pause(20, TimeUnit.SECONDS);
                    continue;
                }
                //从队列中获取
                String infoHash = null;
                try {
                    infoHash = queue.poll(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    //..不可能发生
                }
                //开启新任务
                if (infoHash != null) {
                    start(infoHash, i++ % size);
                    //开始一个任务后,暂停1s.
                    pause(1, TimeUnit.SECONDS);
                    if(i > max)
                        i = 0;
                } else {
                    log.error("{}长时间没有get_peers任务入队...", LOG);
                }
            }
        });


    }


    /**
     * 暂停指定时间
     */
    public void pause(long time, TimeUnit timeUnit) {
        try {
            lock.lock();
            condition.await(time, timeUnit);
        } catch (Exception e) {
            //..不可能发生
        } finally {
            lock.unlock();
        }
    }


    /**
     * 开始任务
     */
    private void start(String infoHashHexStr, int index) {
        //获取最近的8个地址
        List<Node> nodeList = routingTables.get(index).getForTop8(CodeUtil.hexStr2Bytes(infoHashHexStr));
        //目标nodeId
        List<byte[]> nodeIdList = nodeList.stream().map(Node::getNodeIdBytes).collect(Collectors.toList());
        //目标地址
        List<InetSocketAddress> addresses = nodeList.stream().map(Node::toAddress).collect(Collectors.toList());
        //消息id
        String messageId = BTUtil.generateMessageIDOfGetPeers();
        log.info("{}索引:{},开始新任务.消息Id:{},infoHash:{},首次发送地址数:{}", LOG,index, messageId, infoHashHexStr,addresses.size());
        //存入缓存
        getPeersCache.put(messageId, new CommonCache.GetPeersSendInfo(infoHashHexStr).put(nodeIdList));
        //批量发送
        SendUtil.getPeersBatch(addresses, nodeIds.get(index), new String(CodeUtil.hexStr2Bytes(infoHashHexStr), CharsetUtil.ISO_8859_1), messageId, index);
    }

}
