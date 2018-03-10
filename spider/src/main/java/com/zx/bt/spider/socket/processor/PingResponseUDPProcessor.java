package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.YEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ping 回复 处理器
 */
@Slf4j
@Component
public class PingResponseUDPProcessor extends UDPProcessor{

	@Override
	boolean process1(ProcessObject processObject) {

		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.PING.equals(processObject.getMessageInfo().getMethod()) && YEnum.RECEIVE.equals(processObject.getMessageInfo().getStatus());
	}
}
