package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
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

    public FindNodeTask(Config config, RoutingTable routingTable) {
        this.config = config;
        nodeId = config.getMain().getNodeId();
        this.routingTable = routingTable;
    }

    /**
     * 循环执行该任务
     */
    public void start() {

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
     * 向路由表群发
     */
    private void findNodeByTable() {
        long start = System.currentTimeMillis();

        //路由表的循环方法
        routingTable.loop(trieNode -> {
            Node[] nodes = trieNode.getNodes();
            for (int i = 0; i < nodes.length; i++) {
                if(nodes[i] == null)
                    continue;
                SendUtil.findNode(new InetSocketAddress(nodes[i].getIp(), nodes[i].getPort()),nodeId,config.getMain().getTargetNodeId());
            }
        });

        log.info("向{}个节点群发请求,耗时{}秒",routingTable.size(),(System.currentTimeMillis() - start)/1000);
    }




}
