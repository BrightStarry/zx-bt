package com.zx.bt;

import com.zx.bt.config.Config;
import com.zx.bt.socket.DHTServer;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * author:ZhengXing
 * datetime:2018-02-13 18:07
 * 启动器
 */
@Component
public class BTRunner implements CommandLineRunner{

    private final DHTServer dhtServer;
    private final Config config;

    public BTRunner(DHTServer dhtServer, Config config) {
        this.dhtServer = dhtServer;
        this.config = config;
    }

    @Override
    public void run(String... strings) throws Exception {
        //启动服务端
        dhtServer.start();

        /**getChannel
         * 发送find_node请求
         */
        InetSocketAddress address1 = new InetSocketAddress("router.bittorrent.com", 6881);
        InetSocketAddress address = new InetSocketAddress("dht.transmissionbt.com", 6881);
//        SendUtil.ping(address,config.getMain().getNodeId());
        SendUtil.findNode(address,config.getMain().getNodeId(),BTUtil.generateNodeIdString());
        SendUtil.findNode(address1,config.getMain().getNodeId(),BTUtil.generateNodeIdString());
    }

    public static void main(String[] args) {
        System.out.println("��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005��X�c���w�m��\u000BA �'&���\u0004\u001B\u0005".length());

    }


}
