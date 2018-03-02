package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

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
        final InetSocketAddress[] initAddressArray = config.getMain().getInitAddressArray();

        List<String> nodeIds = config.getMain().getNodeIds();
        for (int i = 0; i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            //向每个地址发送请求
            for (InetSocketAddress address : initAddressArray) {
                SendUtil.findNode(address,nodeId, BTUtil.generateNodeIdString(),i);
            }
        }
    }
}
