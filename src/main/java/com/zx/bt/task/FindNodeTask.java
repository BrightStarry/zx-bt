package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.store.Table;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * author:ZhengXing
 * datetime:2018-02-17 11:23
 * find_node请求 任务
 */
@Component
@Slf4j
public class FindNodeTask {
    //存储等待向其发送find_node请求的节点
//    private final BlockingDeque<Node> nodeQueue = new LinkedBlockingDeque<>(102400);

    private final Config config;
    private final Table table;
    private final String nodeId;

    public FindNodeTask( Config config, Table table) {
        this.config = config;
        this.table = table;
        nodeId = config.getMain().getNodeId();
    }

    /**
     * 循环执行该任务
     */
    public void start() {
//        //队列线程
//        new Thread(() -> {
//            while (true) {
//                try {
//                    findNodeByQueue();
//                } catch (Exception e) {
//                    log.error("[FindNodeTask]获取节点队列发送异常:{}", e.getMessage(), e);
//                }
//            }
//        }).start();
        //定时群发路由表线程
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                findNodeByTable();
            } catch (Exception e) {
                log.error("[FindNodeTask]定时群发路由表异常:{}",e.getMessage(),e);
            }
        }, 5, config.getMain().getFindNodeTaskByTableIntervalSecond(), TimeUnit.SECONDS);
    }

    /**
     * 从队列中取出下一节点,并发送find_node请求
     */
//    private void findNodeByQueue() throws Exception {
//        //从阻塞队列中获取node
//        Node node = nodeQueue.pollFirst(3,TimeUnit.SECONDS);
//        //当队列为空时,向路由表群发
//        if (node == null) {
//            log.error("[FindNodeTask]队列为空");
//            findNodeByTable();
//            return;
//        }
//        SendUtil.findNode(new InetSocketAddress(node.getIp(), node.getPort()),nodeId, config.getMain().getTargetNodeId());
//    }

    /**
     * 向路由表群发
     */
    private void findNodeByTable() {
        Collection<Node> nodes = table.getAll();
        long start = System.currentTimeMillis();
        for (Node item : nodes) {
            SendUtil.findNode(new InetSocketAddress(item.getIp(), item.getPort()),nodeId,config.getMain().getTargetNodeId());
        }
        log.info("向{}个节点群发请求,耗时{}秒",nodes.size(),(System.currentTimeMillis() - start)/1000);
    }

//    /**
//     * 向队列中追加元素
//     */
//    public void put(Node node) {
//        //尝试追加元素到队列头部,如果失败
//        if (!nodeQueue.offer(node)) {
//            //尝试从末尾移除元素
//            nodeQueue.pollLast();
//            //然后再次追加,当然,这样也不确保能成功,但是能确保,一定有一个最新的元素被插入队列头部
//            nodeQueue.offer(node);
//        }
//    }
//
//    /**
//     * 向队列中批量追加元素
//     */
//    public void putAll(List<Node> nodeList) {
//        for (Node node : nodeList) {
//            put(node);
//        }
//    }


}
