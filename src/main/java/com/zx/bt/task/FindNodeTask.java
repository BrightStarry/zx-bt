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
        Collection<Node> nodes = table.getAll();
        long start = System.currentTimeMillis();
        for (Node item : nodes) {
            SendUtil.findNode(new InetSocketAddress(item.getIp(), item.getPort()),nodeId,config.getMain().getTargetNodeId());
        }
        log.info("向{}个节点群发请求,耗时{}秒",nodes.size(),(System.currentTimeMillis() - start)/1000);
    }




}
