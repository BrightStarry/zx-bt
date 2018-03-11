package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.YEnum;
import com.zx.bt.spider.socket.Sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ping 请求 处理器
 */
@Slf4j
@Component
public class PingRequestUDPProcessor  extends UDPProcessor{

	private final Sender sender;

	public PingRequestUDPProcessor(Sender sender) {
		this.sender = sender;
	}

	@Override
	boolean process1(ProcessObject processObject) {
		this.sender.pingReceive(processObject.getSender(), nodeIds.get(processObject.getIndex()),
				processObject.getMessageInfo().getMessageId(),processObject.getIndex());


		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.PING.equals(processObject.getMessageInfo().getMethod()) && YEnum.QUERY.equals(processObject.getMessageInfo().getStatus());
	}
}
