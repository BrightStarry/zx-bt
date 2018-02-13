package com.zx.bt;

import com.zx.bt.socket.DHTClient;
import com.zx.bt.socket.DHTClientHandler;
import com.zx.bt.socket.DHTServer;
import com.zx.bt.util.BTUtil;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * author:ZhengXing
 * datetime:2018-02-13 14:04
 * 测试UDP
 */
public class DHTServerTest extends BtApplicationTests{

    @Autowired
    private DHTServer dhtServer;
    @Autowired
    private DHTClient dhtClient;

    @Before
    @SneakyThrows
    public void init() {

    }


    @Test
    @SneakyThrows
    public void test1() {
        //启动服务端
        dhtServer.start();
        Thread.sleep(3000);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 9703);
        Optional<Channel> channelOptional = dhtClient.getChannel(inetSocketAddress,new DHTClientHandler());
        Channel channel = channelOptional.get();
        BTUtil.writeAndFlush(channel,"xxxxx",inetSocketAddress);
        Thread.sleep(1000000000);
    }

}