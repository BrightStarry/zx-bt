package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.store.InfoHashFilter;
import com.zx.bt.util.BTUtil;
import com.zx.bt.socket.Sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-17 11:41
 * 初始化任务
 */
@Slf4j
@Component
public class InitTask {

    private final Config config;
    private final Sender sender;
    private final InfoHashFilter infoHashFilter;

    public InitTask(Config config, Sender sender, InfoHashFilter infoHashFilter) {
        this.config = config;
        this.sender = sender;
        this.infoHashFilter = infoHashFilter;
    }

    /**
     * 加载初始队列,发送find_node请求
     */
    public void run() {
        log.info("当前配置:",config);
        //初始化发送任务
        initSend();
        //初始化过滤器
        infoHashFilter.importExistInfoHash();
    }

    /**
     * 初始化发送任务
     * 向yml中的节点发送请求
     */
    private void initSend() {
        //获取初始化地址
        final InetSocketAddress[] initAddressArray = config.getMain().getInitAddressArray();
        List<String> nodeIds = config.getMain().getNodeIds();
        for (int i = 0; i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            //向每个地址发送请求
            for (InetSocketAddress address : initAddressArray) {
                this.sender.findNode(address,nodeId, BTUtil.generateNodeIdString(),i);
            }
        }
    }
}
