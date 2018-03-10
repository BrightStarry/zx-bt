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
import com.zx.bt.spider.task.GetPeersTask;
import com.zx.bt.spider.util.BTUtil;
import com.zx.bt.common.util.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:30
 * ANNOUNCE_PEER 请求 处理器
 */
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


    public AnnouncePeerRequestUDPProcessor(List<RoutingTable> routingTables, NodeRepository nodeRepository,
                                           GetPeersTask getPeersTask, Sender sender, InfoHashService infoHashService,
                                           FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask) {
        this.routingTables = routingTables;
        this.nodeRepository = nodeRepository;
        this.getPeersTask = getPeersTask;
        this.sender = sender;
        this.infoHashService = infoHashService;
        this.fetchMetadataByOtherWebTask = fetchMetadataByOtherWebTask;
    }

    @Override
    boolean process1(ProcessObject processObject) {
        InetSocketAddress sender = processObject.getSender();
        int index = processObject.getIndex();
        AnnouncePeer.RequestContent requestContent = new AnnouncePeer.RequestContent(processObject.getRawMap(), sender.getPort());

        log.info("{}ANNOUNCE_PEER.发送者:{},ports:{},info_hash:{},map:{}",
                LOG, sender, requestContent.getPort(), requestContent.getInfo_hash(), processObject.getRawMap());


        //入库
        infoHashService.saveInfoHash(requestContent.getInfo_hash(), BTUtil.getIpBySender(sender) + ":" + requestContent.getPort() + ";");
        //尝试从get_peers等待任务队列删除该任务,正在进行的任务可以不删除..因为删除比较麻烦.要遍历value
        getPeersTask.remove(requestContent.getInfo_hash());
        //回复
        this.sender.announcePeerReceive(processObject.getMessageInfo().getMessageId(), sender, nodeIds.get(index), index);
        Node node = new Node(CodeUtil.hexStr2Bytes(requestContent.getId()), sender, NodeRankEnum.ANNOUNCE_PEER.getCode());
        //加入路由表
        routingTables.get(index).put(node);
        //入库
        nodeRepository.save(node);
        fetchMetadataByOtherWebTask.put(requestContent.getInfo_hash());
        return true;

    }

    @Override
    boolean isProcess(ProcessObject processObject) {
        return MethodEnum.ANNOUNCE_PEER.equals(processObject.getMessageInfo().getMethod()) && YEnum.QUERY.equals(processObject.getMessageInfo().getStatus());
    }
}
