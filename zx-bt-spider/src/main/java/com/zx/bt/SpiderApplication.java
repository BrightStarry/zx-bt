package com.zx.bt;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.task.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.util.Collection;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@Slf4j
public class SpiderApplication implements CommandLineRunner {

	private final InitTask initTask;
	private final FindNodeTask findNodeTask;
	private final GetPeersTask getPeersTask;
	private final FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask;
	private final FetchMetadataByPeerTask fetchMetadataByPeerTask;
	private final Config config;

	public SpiderApplication(InitTask initTask, FindNodeTask findNodeTask, GetPeersTask getPeersTask,
							 FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask, FetchMetadataByPeerTask fetchMetadataByPeerTask,
							 Config config,
							 ObjectMapper objectMapper, MetadataService metadataService) {
		this.initTask = initTask;
		this.findNodeTask = findNodeTask;
		this.getPeersTask = getPeersTask;
		this.fetchMetadataByOtherWebTask = fetchMetadataByOtherWebTask;
		this.fetchMetadataByPeerTask = fetchMetadataByPeerTask;
		this.config = config;
		this.objectMapper = objectMapper;
		this.metadataService = metadataService;
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


	private final ObjectMapper objectMapper;

	private final MetadataService metadataService;

	@SneakyThrows
	public void test() {
		// 使用行迭代器，迭代该文件的每一行
		File file = new File(File.separator + "a.txt");
		LineIterator lineIterator = FileUtils.lineIterator(file);
		int i = 0;

		// 构造泛型集合，用于ObjectMapper的转换
		JavaType type = getCollectionType(List.class, Metadata.class);

		// 遍历每一行。读取为 list，存入es
		while (lineIterator.hasNext()) {
			String str = lineIterator.nextLine();
			List<Metadata> metadataList = (List<Metadata>) objectMapper.readValue(str, type);
			// 将es中的_id设为null，才能添加
			metadataList.parallelStream().forEach(item -> item.set_id(null));

			metadataService.batchInsert(metadataList);

			i += metadataList.size();
			log.info("当前数:{}",i);
		}

		lineIterator.close();
	}


	/**
	 * 获取泛型的Collection Type
	 */
	public <C extends Collection, E> JavaType getCollectionType(Class<C> collectionClass, Class<E> elementClass) {
		return objectMapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
	}



	/**
	 * 启动任务
	 * 最高优先级
	 */
	@Order(Integer.MIN_VALUE)
	@Override
	public void run(String... strings) throws Exception {
		test();
		if (!config.getMain().getStart()) return;

		/**
		 * 先行开启各队列任务, 重启时,任务并发过大
		 * 后启动会导致队列过长.虽然问题不大
		 */
		//异步启动get_peers任务
		getPeersTask.start();
		//异步启动fetchMetadataByOtherWeb任务
		fetchMetadataByOtherWebTask.start();
		//异步启动fetchMetadataByPeerTask任务
		fetchMetadataByPeerTask.start();

		//同步执行初始化任务
		initTask.run();
		//异步启动find_node任务
		findNodeTask.start();


	}
}
