package com.zx.bt;

import com.dampcake.bencode.Bencode;
import com.zx.bt.socket.DHTClient;
import com.zx.bt.socket.DHTClientHandler;
import com.zx.bt.socket.DHTServer;
import com.zx.bt.socket.DHTServerHandler;
import com.zx.bt.util.BTUtil;
import io.netty.channel.Channel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * author:ZhengXing
 * datetime:2018-02-13 18:07
 * 启动器
 */
@Component
public class BTRunner implements CommandLineRunner{

    private final DHTServer dhtServer;
    private final DHTClient dhtClient;
    private final Bencode bencode;

    public BTRunner(DHTServer dhtServer, DHTClient dhtClient, Bencode bencode) {
        this.dhtServer = dhtServer;
        this.dhtClient = dhtClient;
        this.bencode = bencode;
    }

    @Override
    public void run(String... strings) throws Exception {
        //启动服务端
        dhtServer.start();
        //连接到目标服务器
//        InetSocketAddress address = new InetSocketAddress("router.bittorrent.com", 6881);
//        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9595);

        /**getChannel
         * 发送find_node请求
         */
        byte[] bytes = BTUtil.generateNodeID();
        byte[] encoded = bencode.encode(new HashMap<Object, Object>() {{
            put("t", "aa");
            put("y", "q");
            put("q", "find_node");
            put("a", new HashMap<Object, Object>() {{
                put("id",    new String(bytes));
                put("target",new String(bytes));
            }});
        }});

        InetSocketAddress address1 = new InetSocketAddress("router.bittorrent.com", 6881);
        InetSocketAddress address2 = new InetSocketAddress("dht.transmissionbt.com", 6881);
        BTUtil.writeAndFlush(  DHTServerHandler.channel, encoded, address1);
        BTUtil.writeAndFlush(  DHTServerHandler.channel, encoded, address2);


    }

    public static void main(String[] args) {
        Bencode bencode = new Bencode();
        byte[] encoded = bencode.encode(new HashMap<Object, Object>() {{
            put("t", "aa");
            put("y", "q");
            put("q", "find_node");
            put("a", new HashMap<Object, Object>() {{
                put("id",    "33334554530123456789");
                put("target","23444334433443123456");
            }});
        }});
        System.out.println(new String(encoded));
    }


}
