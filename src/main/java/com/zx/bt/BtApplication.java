package com.zx.bt;

import com.dampcake.bencode.Bencode;
import com.zx.bt.socket.UDPServer;
import com.zx.bt.task.FindNodeTask;
import com.zx.bt.task.InitTask;
import io.netty.util.CharsetUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@SpringBootApplication
public class BtApplication implements CommandLineRunner{

	private final UDPServer udpServer;
	private final InitTask initTask;
	private final FindNodeTask findNodeTask;

	public BtApplication(UDPServer udpServer, InitTask initTask, FindNodeTask findNodeTask) {
		this.udpServer = udpServer;
		this.initTask = initTask;
		this.findNodeTask = findNodeTask;
	}

	public static void main(String[] args) {
		SpringApplication.run(BtApplication.class, args);
	}




	/**
	 * 启动任务
	 * 最高优先级
	 */
	@Order(Integer.MIN_VALUE)
	@Override
	public void run(String... strings) throws Exception {
		//异步启动udp服务端
		udpServer.start();
		//同步执行初始化任务
		initTask.run();
		//异步启动find_node任务
		findNodeTask.start();
	}
}
