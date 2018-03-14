package com.zx.bt.spider.task;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.enums.NodeRankEnum;
import com.zx.bt.spider.function.Pauseable;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.util.BTUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * author:ZhengXing
 * datetime:2018-02-17 11:23
 * find_node请求 任务
 */
@Component
@Slf4j
public class FindNodeTask implements Pauseable {

    private final Config config;
    private final List<String> nodeIds;
    private final ReentrantLock lock;
    private final Condition condition;
    private final List<RoutingTable> routingTables;
    private final Sender sender;

    public FindNodeTask(Config config, List<RoutingTable> routingTables, Sender sender) {
        this.config = config;
        this.nodeIds = config.getMain().getNodeIds();
        this.routingTables = routingTables;
        this.sender = sender;
        this.queue = new LinkedBlockingDeque<>(config.getPerformance().getFindNodeTaskMaxQueueLength());
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    /**
     * 发送队列
     */
    private final BlockingDeque<FindNode> queue;

    /**
     * 入队首
     */
    public void put(InetSocketAddress address) {
        //如果失败
        if (!queue.offer(new FindNode(address))) {
            //从末尾移除一个
            queue.pollLast();
            //再次增加..当然.是不保证成功的.但是总会有一个最新的插进去
            queue.offer(new FindNode(address));
        }
    }

    /**
     * 循环执行该任务
     */
    public void start() {
        //暂停时长
        int pauseTime = config.getPerformance().getFindNodeTaskIntervalMS();
        int size = nodeIds.size();
        TimeUnit milliseconds = TimeUnit.MILLISECONDS;
        for (int i = 0; i < config.getPerformance().getFindNodeTaskThreadNum(); i++) {
            new Thread(()->{
                while (true) {
                    try {
                        //轮询使用每个端口向外发送请求
                        for (int j = 0; j < size; j++) {
                            run(j);
                            pause(lock, condition, pauseTime, milliseconds);
                        }
                    } catch (Exception e) {
                        log.error("[FindNodeTask]异常.error:{}",e.getMessage());
                    }
                }
            }).start();
        }
    }

    /**
     * 每x分钟,取出rank值较大的节点发送findNode请求
     */
//    @Scheduled(cron = "0 0/10 * * * ? ")
    @Deprecated
    public void autoPutToFindNodeQueue() {
        for (int j = 0; j < routingTables.size(); j++) {
            RoutingTable routingTable = routingTables.get(j);
            try {
                List<Node> nodeList = routingTable.getForTop8(BTUtil.generateNodeId());
                if(CollectionUtils.isNotEmpty(nodeList))
                    nodeList.forEach(item ->put(item.toAddress()));
                routingTable.loop(trieNode -> {
                    Node[] nodes = trieNode.getNodes();
                    for (int i = 0; i < trieNode.getCount(); i++) {
                        if (nodes[i].getRank() > NodeRankEnum.GET_PEERS.getCode()) {
                            put(nodes[i].toAddress());
                        }
                    }
                });
            } catch (Exception e) {
                log.info("[autoPutToFindNodeQueue]异常.e:{}",e.getMessage(),e);
            }
        }
    }



    /**
     * 任务
     */
    @SneakyThrows
    public void run(int index) {
        sender.findNode(queue.take().getAddress(),nodeIds.get(index),BTUtil.generateNodeIdString(),index);
    }



    /**
     * 长度
     */
    public int size() {
        return queue.size();
    }

    /**
     * 待发送任务实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class FindNode{
        /**
         * 目标地址
         */
        private InetSocketAddress address;

        /**
         * 索引
         */
//        private int index;
    }


}
