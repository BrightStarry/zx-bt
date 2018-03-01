package com.zx.bt.socket.processor;

import com.zx.bt.dto.MessageInfo;
import com.zx.bt.dto.method.AnnouncePeer;
import com.zx.bt.entity.InfoHash;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.InfoHashTypeEnum;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.NodeRankEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.repository.NodeRepository;
import com.zx.bt.store.RoutingTable;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.SendUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ANNOUNCE_PEER 回复 处理器
 */
@Slf4j
@Component
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
