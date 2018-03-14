package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.YEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ANNOUNCE_PEER 回复 处理器
 */
@Slf4j
//@Component
public class AnnouncePeerResponseUDPProcessor extends UDPProcessor{
	private static final String LOG = "[ANNOUNCE_PEER_RECEIVE]";

	@Override
	boolean process1(ProcessObject processObject) {
		log.info("{}TODO",LOG);
		return true;
	}

	@Override
	boolean isProcess(ProcessObject processObject) {
		return MethodEnum.ANNOUNCE_PEER.equals(processObject.getMessageInfo().getMethod()) && YEnum.RECEIVE.equals(processObject.getMessageInfo().getStatus());
	}
}
