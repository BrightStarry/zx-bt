package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.socket.ProcessQueue;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final Config config;
    private final String nodeId;
    private final RoutingTable routingTable;
    private final ProcessQueue processQueue;

    public FindNodeTask(Config config, RoutingTable routingTable, ProcessQueue processQueue) {
        this.config = config;
        nodeId = config.getMain().getNodeId();
        this.routingTable = routingTable;
        this.processQueue = processQueue;
    }

    /**
     * 循环执行该任务
     */
    public void start() {

        //定时群发路由表线程
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);


        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                    findNode();
            } catch (Exception e) {
                log.error("[FindNodeTask]定时群发路由表异常:{}", e.getMessage());
            }
        }, 0, config.getPerformance().getFindNodeTaskIntervalSecond(), TimeUnit.SECONDS);
    }


    /**
     * 向路由表群发
     */
    private void findNode() {
        long size = routingTable.size();
        long l = (size >> 6) + 1;
        log.info("[FindNodeTask]当前路由表长度:{},l:{},当前处理队列长度:{}", size,l,processQueue.size());
        for (int i = 0; i < l; i++) {
            byte[] target = BTUtil.generateNodeId();
            List<Node> nodeList = routingTable.getForTop8(target);
            nodeList.forEach(node -> SendUtil.findNode(node.toAddress(), nodeId, config.getMain().getTargetNodeId()));
        }
    }



}
