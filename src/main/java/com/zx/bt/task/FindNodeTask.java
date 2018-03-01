package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
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
public class FindNodeTask {

    private final Config config;
    private final List<String> nodeIds;
    private final ReentrantLock lock;
    private final Condition condition;

    public FindNodeTask(Config config) {
        this.config = config;
        this.nodeIds = config.getMain().getNodeIds();
        this.queue = new LinkedBlockingQueue<>(1 << 20);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    /**
     * 发送队列
     */
    private BlockingQueue<FindNode> queue;

    /**
     * 入队
     */
    public void put(InetSocketAddress address) {
        queue.offer(new FindNode(address));
    }

    /**
     * 循环执行该任务
     */
    public void start() {
        new Thread(()->{
            int max = Integer.MAX_VALUE - 10000;
            int i = 0;
            int size = nodeIds.size();
            while (true) {
                run(i++ % size);
                pause(20,TimeUnit.MILLISECONDS);
                if(i > max)
                    i = 0;
            }
        }).start();
    }

    /**
     * 任务
     */
    @SneakyThrows
    public void run(int index) {
        FindNode findNode = queue.take();
        SendUtil.findNode(findNode.getAddress(),nodeIds.get(index),BTUtil.generateNodeIdString(),index);
    }

    /**
     * 暂停指定时间
     */
    public void pause(long time, TimeUnit timeUnit) {
        try {
            lock.lock();
            condition.await(time, timeUnit);
        } catch (Exception e){
            //..不可能发生
        }finally {
            lock.unlock();
        }
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
