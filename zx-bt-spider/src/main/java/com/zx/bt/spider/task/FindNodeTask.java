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
        this.putLastMaxNum = config.getPerformance().getFindNodeTaskMaxQueueLength() / 5;
    }

    /**
     * 普通入队条件, 队列最大长度的10分之一
     * 只有当size小于该长度,才允许普通入队
     */
    private final Integer putLastMaxNum;

    /**
     * 发送队列
     */
    private final BlockingDeque<FindNode> queue;

    /**
     * 入队首
     * announce_peer等
     */
    public void put(InetSocketAddress address) {
        // 如果插入失败
        if(!queue.offer(new FindNode(address))){
            //从末尾移除一个
            queue.pollLast();
        }
    }

    /**
     * 入队尾
     * 普通的find_node回复等
     */
    public void putLast(InetSocketAddress address) {
        queue.offerLast(new FindNode(address));
    }

    /**
     * 判断是否允许普通的node入队
     */
    public boolean isAllowPutLast() {
        return queue.size() < putLastMaxNum;
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
