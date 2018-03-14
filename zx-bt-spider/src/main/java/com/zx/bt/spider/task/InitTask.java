package com.zx.bt.spider.task;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.socket.UDPServer;
import com.zx.bt.spider.store.InfoHashFilter;
import com.zx.bt.spider.util.BTUtil;
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
    private final UDPServer udpServer;

    public InitTask(Config config, Sender sender, InfoHashFilter infoHashFilter, UDPServer udpServer) {
        this.config = config;
        this.sender = sender;
        this.infoHashFilter = infoHashFilter;
        this.udpServer = udpServer;
    }

    /**
     * 加载初始队列,发送find_node请求
     */
    public void run() {
        //初始化过滤器
        infoHashFilter.run();
        //异步启动udp服务端
        udpServer.start();
        //初始化发送任务
        initSend();
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
