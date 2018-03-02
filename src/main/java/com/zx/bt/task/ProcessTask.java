package com.zx.bt.task;

import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.exception.BTException;
import com.zx.bt.socket.processor.ProcessObject;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.Bencode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 14:04
 * 处理队列
 * 该类将接收到的消息暂存,另开线程匀速处理,尝试以此解决java.net.SocketException: Network dropped connection on reset: no further information
 *
 * 废弃. 想了下.对处理任务进行缓存会导致get_peers等任务,被长期延迟处理(因为无法很好的控制速率).
 * 如果积累过多任务,还会导致需要丢弃一部分任务.
 * 其实只需要控制发送速率. 也就可以间接控制接收速率了.
 */
@Slf4j
@Deprecated
@Component
public class ProcessTask {
	private static final String LOG = "[DHT服务端处理类]-";
	private final Config config;

	/**
	 * 队列
	 */
	private final BlockingQueue<A> queue;

	private final ExecutorService service;

	private final UDPProcessorManager udpProcessorManager;

	private final Bencode bencode;


	public ProcessTask(Config config, UDPProcessorManager udpProcessorManager, Bencode bencode) {
		this.config = config;
		this.udpProcessorManager = udpProcessorManager;
		this.bencode = bencode;
		this.queue = new LinkedBlockingQueue<>();
		this.service = Executors.newFixedThreadPool(40);

	}

	/**
	 * 入队
	 */
	public void put(A processObject) {
		queue.offer(processObject);
	}

	/**
	 * 长度
	 */
	public int size() {
		return queue.size();
	}

	public void start() {
		for (int i = 0; i < 40; i++) {
			service.execute(()->{
				while (true) {
					try {
						A processObject= null;
						try {
							processObject = queue.take();
						} catch (InterruptedException e) {
							//..不可能发生
						}
						byte[] bytes = processObject.getBytes();
						InetSocketAddress sender = processObject.getSender();
						//解码为map
						Map<String, Object> map;
						try {
							map = bencode.decode(bytes, Map.class);
						} catch (BTException e) {
							log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage());
							continue;
						} catch (Exception e) {
							log.error("{}消息解码异常.发送者:{}.异常:{}", LOG, sender, e.getMessage(), e);
							continue;
						}

						//解析出MessageInfo
						MessageInfo messageInfo;
						try {
							messageInfo = BTUtil.getMessageInfo(map);
						} catch (BTException e) {
							log.error("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage());
							continue;
						} catch (Exception e) {
							log.error("{}解析MessageInfo异常.异常:{}", LOG, e.getMessage(), e);
							continue;
						}


						udpProcessorManager.process(new ProcessObject(messageInfo, map, sender,processObject.getIndex()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class A{
		private byte[] bytes;
		private InetSocketAddress sender;
		private int index;
	}
}
