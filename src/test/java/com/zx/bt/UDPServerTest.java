package com.zx.bt;

import com.zx.bt.entity.InfoHash;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.socket.TCPClient;
import com.zx.bt.socket.UDPServer;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-13 14:04
 * 测试UDP
 */
@Slf4j
public class UDPServerTest extends BtApplicationTests{

	@Autowired
	private InfoHashRepository infoHashRepository;

	@Autowired
	private TCPClient tcpClient;


	@Test
	@SneakyThrows
	public void test1() {

		List<InfoHash> all = infoHashRepository.findAll();
		all.stream().forEach(infoHash -> {
			String peerAddress = infoHash.getPeerAddress();
			String[] addArr = peerAddress.split(";");
			for (String s : addArr) {
				String[] ipPort = s.split(":");
				log.info("ip:{},ports:{},infoHash:{}",ipPort[0],Integer.parseInt(ipPort[1]),infoHash.getInfoHash());
				tcpClient.connection(new InetSocketAddress(ipPort[0],Integer.parseInt(ipPort[1])),
						infoHash.getInfoHash(), BTUtil.generateNodeId());
			}

		});



		Thread.sleep(100000000);
	}


}