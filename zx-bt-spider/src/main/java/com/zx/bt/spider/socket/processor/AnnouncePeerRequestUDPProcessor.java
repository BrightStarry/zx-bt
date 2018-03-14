package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.dto.method.AnnouncePeer;
import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.NodeRankEnum;
import com.zx.bt.spider.enums.YEnum;
import com.zx.bt.spider.repository.NodeRepository;
import com.zx.bt.spider.service.InfoHashService;
import com.zx.bt.spider.socket.Sender;
import com.zx.bt.spider.store.RoutingTable;
import com.zx.bt.spider.task.FetchMetadataByOtherWebTask;
import com.zx.bt.spider.task.FindNodeTask;
import com.zx.bt.spider.task.GetPeersTask;
import com.zx.bt.spider.util.BTUtil;
import com.zx.bt.common.util.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ANNOUNCE_PEER 请求 处理器
 */
@Order(2)
@Slf4j
@Component
public class AnnouncePeerRequestUDPProcessor extends UDPProcessor {
    private static final String LOG = "[ANNOUNCE_PEER]";

    private final List<RoutingTable> routingTables;
    private final NodeRepository nodeRepository;
    private final GetPeersTask getPeersTask;
    private final Sender sender;
    private final InfoHashService infoHashService;
    private final FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask;
    private final FindNodeTask findNodeTask;


    public AnnouncePeerRequestUDPProcessor(List<RoutingTable> routingTables, NodeRepository nodeRepository,
                                           GetPeersTask getPeersTask, Sender sender, InfoHashService infoHashService,
                                           FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask, FindNodeTask findNodeTask) {
        this.routingTables = routingTables;
        this.nodeRepository = nodeRepository;
        this.getPeersTask = getPeersTask;
        this.sender = sender;
        this.infoHashService = infoHashService;
        this.fetchMetadataByOtherWebTask = fetchMetadataByOtherWebTask;
        this.findNodeTask = findNodeTask;
    }

    @Override
    boolean process1(ProcessObject processObject) {
        AnnouncePeer.RequestContent requestContent = new AnnouncePeer.RequestContent(processObject.getRawMap(), processObject.getSender().getPort());
        log.info("{}收到消息.",LOG);
        //入库
        infoHashService.saveInfoHash(requestContent.getInfo_hash(), BTUtil.getIpBySender(processObject.getSender()) + ":" + requestContent.getPort() + ";");
        //尝试从get_peers等待任务队列删除该任务,正在进行的任务可以不删除..因为删除比较麻烦.要遍历value
        getPeersTask.remove(requestContent.getInfo_hash());
        //回复
        this.sender.announcePeerReceive(processObject.getMessageInfo().getMessageId(), processObject.getSender(), nodeIds.get(processObject.getIndex()), processObject.getIndex());
        Node node = new Node(CodeUtil.hexStr2Bytes(requestContent.getId()), processObject.getSender(), NodeRankEnum.ANNOUNCE_PEER.getCode());
        //加入路由表
        routingTables.get(processObject.getIndex()).put(node);
        //入库
        nodeRepository.save(node);
        //加入任务队列
        fetchMetadataByOtherWebTask.put(requestContent.getInfo_hash());
        //加入findNode任务队列
        findNodeTask.put(processObject.getSender());
        return true;
    }

    @Override
    boolean isProcess(ProcessObject processObject) {
        return MethodEnum.ANNOUNCE_PEER.equals(processObject.getMessageInfo().getMethod()) && YEnum.QUERY.equals(processObject.getMessageInfo().getStatus());
    }
}
