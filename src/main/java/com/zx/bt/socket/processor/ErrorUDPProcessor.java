package com.zx.bt.socket.processor;

import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * 异常回复  处理器
 * 使其优先级比其他处理器高
 */
@Order(0)
@Slf4j
@Component
public class ErrorUDPProcessor extends UDPProcessor{
	private static final String LOG = "[ERROR_PROCESS]";

	@Override
	boolean process1(ProcessObject processObject) {
//		log.error("{}对方节点:{},回复异常信息:{}", LOG, processObject.getSender(), processObject.getRawMap());
		//不做任何操作
		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return YEnum.ERROR.equals(processObject.getMessageInfo().getStatus());
	}
}
