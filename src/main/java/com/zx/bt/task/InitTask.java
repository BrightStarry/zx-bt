package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * author:ZhengXing
 * datetime:2018-02-17 11:41
 * 初始化任务
 */
@Component
public class InitTask {

    private final Config config;

    public InitTask(Config config) {
        this.config = config;
    }

    /**
     * 加载初始队列,发送find_node请求
     */
    public void run() {
        //获取初始化地址
        InetSocketAddress[] initAddressArray = config.getMain().getInitAddressArray();

        String nodeId = config.getMain().getNodeId();
        //向每个地址发送请求
        for (InetSocketAddress address : initAddressArray) {
            SendUtil.findNode(address,nodeId, BTUtil.generateNodeIdString());
        }
    }
}
