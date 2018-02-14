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

    }

}