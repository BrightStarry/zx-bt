package com.zx.bt.spider.task;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.repository.NodeRepository;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.socket.UDPServer;
import com.zx.bt.spider.store.InfoHashFilter;
import com.zx.bt.spider.util.BTUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
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
    private final NodeRepository nodeRepository;


    public InitTask(Config config, Sender sender, InfoHashFilter infoHashFilter, UDPServer udpServer, NodeRepository nodeRepository) {
        this.config = config;
        this.sender = sender;
        this.infoHashFilter = infoHashFilter;
        this.udpServer = udpServer;
        this.nodeRepository = nodeRepository;
    }

    /**
     * 加载初始队列,发送find_node请求
     */
    public void run() {
        //初始化过滤器
        infoHashFilter.run();
        //获取初始化发送地址
        InetSocketAddress[] initAddresses = getInitAddresses();
        //异步启动udp服务端
        udpServer.start();
        //初始化发送任务
        initSend(initAddresses);
    }

    /**
     * 获取初始化发送地址集合
     */
    private InetSocketAddress[] getInitAddresses() {
        // 从数据库中查询地址
        Integer initTaskSendNum = config.getMain().getInitTaskSendNum();
        List<Node> nodeList = nodeRepository.findTopXNode(initTaskSendNum);
        //获取配置文件中的初始化地址
        InetSocketAddress[] initAddressArray = config.getMain().getInitAddressArray();
        if(CollectionUtils.isNotEmpty(nodeList))
            initAddressArray = ArrayUtils.addAll(initAddressArray, nodeList.stream().map(Node::toAddress).toArray(InetSocketAddress[]::new));
        return initAddressArray;
    }

    /**
     * 初始化发送任务
     * 向yml中的节点发送请求
     */
    private void initSend(InetSocketAddress[] initAddressArray) {
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
