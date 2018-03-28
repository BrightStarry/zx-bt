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
    private final Sender sender;

    public FindNodeTask(Config config,Sender sender) {
        this.config = config;
        this.nodeIds = config.getMain().getNodeIds();
        this.sender = sender;
        this.queue = new LinkedBlockingDeque<>(config.getPerformance().getFindNodeTaskMaxQueueLength());
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    /**
     * 发送队列
     */
    private final BlockingDeque<InetSocketAddress> queue;

    /**
     * 入队首
     * announce_peer等
     */
    public void put(InetSocketAddress address) {
        // 如果插入失败
        if(!queue.offer(address)){
            //从末尾移除一个
            queue.pollLast();
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
                int j;
                while (true) {
                    try {
                        //轮询使用每个端口向外发送请求
                        for (j = 0; j < size; j++) {
                            sender.findNode(queue.take(),nodeIds.get(j),BTUtil.generateNodeIdString(),j);
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
     * 长度
     */
    public int size() {
        return queue.size();
    }
}
