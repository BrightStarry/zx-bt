package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.function.Pauseable;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.socket.Sender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;
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
        this.queue = new LinkedBlockingDeque<>(10240);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    /**
     * 发送队列
     */
    private BlockingDeque<FindNode> queue;

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
        for (int i = 0; i < config.getPerformance().getFindNodeTaskThreadNum(); i++) {
            new Thread(()->{
                int size = nodeIds.size();
                while (true) {
                    try {
                        //轮询使用每个端口向外发送请求
                        for (int j = 0; j < size; j++) {
                            run(j);
                            pause(lock, condition, pauseTime, TimeUnit.MILLISECONDS);
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
    @Scheduled(cron = "0 0/10 * * * ? ")
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
     * 更新线程
     * 每x分钟,更新一次要find_Node的目标节点
     */
    @Scheduled(cron = "0 0/3 * * * ? ")
    public void updateTargetNodeId() {
        config.getMain().setTargetNodeId(BTUtil.generateNodeIdString());
        log.info("已更新TargetNodeId");
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
