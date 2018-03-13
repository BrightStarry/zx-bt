package com.zx.bt.spider;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.socket.UDPServer;
import com.zx.bt.spider.task.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class SpiderApplication implements CommandLineRunner{

	private final InitTask initTask;
	private final FindNodeTask findNodeTask;
	private final GetPeersTask getPeersTask;
	private final FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask;
	private final FetchMetadataByPeerTask fetchMetadataByPeerTask;
	private final Config config;

	public SpiderApplication( InitTask initTask, FindNodeTask findNodeTask, GetPeersTask getPeersTask, FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask, FetchMetadataByPeerTask fetchMetadataByPeerTask, Config config) {
		this.initTask = initTask;
		this.findNodeTask = findNodeTask;
		this.getPeersTask = getPeersTask;
		this.fetchMetadataByOtherWebTask = fetchMetadataByOtherWebTask;
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
		this.config = config;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpiderApplication.class, args);
	}

	/**
	 * 结束程序
	 */
	public static void exit() {
		System.exit(0);
	}


	/**
	 * 启动任务
	 * 最高优先级
	 */
	@Order(Integer.MIN_VALUE)
	@Override
	public void run(String... strings) throws Exception {
		if(!config.getMain().getStart()) return;
		//同步执行初始化任务
		initTask.run();
		//异步启动find_node任务
		findNodeTask.start();
		//异步启动get_peers任务
		getPeersTask.start();
		//异步启动fetchMetadataByOtherWeb任务
		fetchMetadataByOtherWebTask.start();
		//异步启动fetchMetadataByPeerTask任务
		fetchMetadataByPeerTask.start();

	}
}
