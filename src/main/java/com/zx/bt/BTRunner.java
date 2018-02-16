package com.zx.bt;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.socket.UDPServer;
import com.zx.bt.store.Table;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018-02-13 18:07
 * 启动器
 */
@Component
@Slf4j
public class BTRunner implements CommandLineRunner{

    private final UDPServer UDPServer;
    private final Config config;
    private final Table table;

    public BTRunner(UDPServer UDPServer, Config config, Table table) {
        this.UDPServer = UDPServer;
        this.config = config;
        this.table = table;
    }

    @Override
    public void run(String... strings) throws Exception {
        //启动服务端
        UDPServer.start();

        //获取初始化地址
        InetSocketAddress[] addresses = config.getMain().getInitAddresses().stream().map(item -> {
            String[] split = item.split(":");
            return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        }).toArray(InetSocketAddress[]::new);

        String nodeId = config.getMain().getNodeId();

        //给初始化地址发送find_node
        for (InetSocketAddress address : addresses) {
            SendUtil.findNode(address,nodeId,config.getMain().getTargetNodeId());
        }

        //开启定时任务
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                Collection<Node> nodeList = table.getAll();
                nodeList.forEach(item->{
                    try {
                        SendUtil.findNode(new InetSocketAddress(item.getIp(), item.getPort()),nodeId, BTUtil.generateNodeIdString());
                    } catch (Exception e) {
                        log.info("发送有误.异常:{}",e.getMessage(),e);
                    }

                });
            } catch (Exception e) {
                log.info("发送有误.异常:{}",e.getMessage(),e);
            }
        }, 30, 60, TimeUnit.SECONDS);


    }

    public static void main(String[] args) {
        System.out.println("��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005".length());

    }


}
