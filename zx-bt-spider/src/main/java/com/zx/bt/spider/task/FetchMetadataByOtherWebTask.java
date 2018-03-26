package com.zx.bt.spider.task;

import com.zx.bt.spider.config.Config;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.spider.parser.AbstractInfoHashParser;
import com.zx.bt.spider.store.InfoHashFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018-03-06 20:16
 * 尝试从其他web网站获取已有的metadata信息
 */
@Component
@Slf4j
public class FetchMetadataByOtherWebTask {
    private static final String LOG = "[FetchMetadataByOtherWebTask]";

    /**
     * 等待尝试获取队列
     * 存储16进制的infoHash
     */
    private final BlockingQueue<String> queue;

    /**
     * 所有解析类集合
     */
    private final List<AbstractInfoHashParser> allInfoHashParsers;

    /**
     * 当前开启的解析类集合
     */
    private  volatile  List<AbstractInfoHashParser> currentInfoHashParsers = new LinkedList<>();

    /**
     * 线程池
     */
    private final ExecutorService threadPool;

    /**
     * 其他功能类
     */
    private final GetPeersTask getPeersTask;
    private final MetadataService metadataService;
    private final InfoHashFilter infoHashFilter;

    public FetchMetadataByOtherWebTask(Config config,
                                       List<AbstractInfoHashParser> infoHashParsers, GetPeersTask getPeersTask, MetadataService metadataService,
                                       InfoHashFilter infoHashFilter) {
        this.queue = new LinkedBlockingQueue<>(config.getPerformance().getFetchMetadataByOtherWebTaskQueueNum());
        this.getPeersTask = getPeersTask;
        this.metadataService = metadataService;
        this.infoHashFilter = infoHashFilter;
        this.threadPool = Executors.newFixedThreadPool(config.getPerformance().getFetchMetadataByOtherWebTaskThreadNum());
        this.allInfoHashParsers = infoHashParsers;
        //构建需要的解析器集合
        buildInfoHashParser(config.getMain().getInfoHashParserTypes());
    }

    /**
     * 抽取出配置中包含的需要的解析类
     * 需要传入解析类类型字符串,用","分割.
     * 例如 1,2,3,4
     */
    public void buildInfoHashParser(String metadataTypes) {
        this.currentInfoHashParsers.clear();
        if (StringUtils.isBlank(metadataTypes)) {
            this.currentInfoHashParsers.addAll(this.allInfoHashParsers);
            return;
        }
        Integer[] infoHashParserTypeArr = Arrays.stream(metadataTypes.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
        for (AbstractInfoHashParser item : this.allInfoHashParsers) {
            Integer type = item.getMetadataType().getCode();
            if (ArrayUtils.contains(infoHashParserTypeArr, type)) {
                this.currentInfoHashParsers.add(item);
            }
        }
    }

    /**
     * 获取当前解析器类型集合
     */
    public List<Integer> getCurrentParserTypesId() {
        return this.currentInfoHashParsers.stream().map(item -> item.getMetadataType().getCode()).collect(Collectors.toList());
    }

    /**
     * 入队
     * 下面的操作如果没有当队列满了,不加入过滤器的需求, 可以直接使用infoHashFilter.put()方法,
     * 并通过返回值判断. 为true,put成功,不重复; 为false,put失败,重复
     */
    public void put(String infoHashHexStr) {
        //如果处理过该infoHash,则不做任何操作
        if (infoHashFilter.contain(infoHashHexStr)) {
            return;
        }
        //否则将其加入布隆过滤器和队列
        //当队列已满.抛弃该任务, 也不加入过滤器
        if (queue.offer(infoHashHexStr)) {
            infoHashFilter.put(infoHashHexStr);
        }
    }


    /**
     * 队列长度
     */
    public int size() {
        return queue.size();
    }

    public static void main(String[] args) {
        System.out.println(new Config());
    }

    /**
     * 开启若干线程,执行run()
     * 从原先的直接启动x个线程,靠阻塞队列实现暂停, 修改为使用线程池,只控制最大线程数
     */
    public void start() {
        new Thread(()->{
            while (true) {
                try {
                    String infoHashHexStr = queue.take();
                    this.threadPool.execute(()->{
                        try {
                            Metadata metadata = run(infoHashHexStr);
                            //将种子信息入库
                            if (!Objects.isNull(metadata)) {
								metadataService.insert(metadata);
							} else {
								//将任务加入get_peers队列
								getPeersTask.put(infoHashHexStr);
							}
                        } catch (Exception e) {
                            log.error("{}子任务异常:{}",LOG,e.getMessage(),e);
                        }
                    });
                } catch (Exception e) {
                    log.error("{}主任务异常:{}",LOG,e.getMessage(),e);
                }
            }
        }).start();


//        for (int i = 0; i < config.getPerformance().getFetchMetadataByOtherWebTaskThreadNum(); i++) {
//            new Thread(()->{
//                while (true) {
//                    try {
//                        String infoHashHexStr = queue.take();
//                        Metadata metadata = run(infoHashHexStr);
//                        //将种子信息入库
//                        if (!Objects.isNull(metadata)) {
//                            metadataService.insert(metadata);
//                        } else {
//                            //将任务加入get_peers队列
//                            getPeersTask.put(infoHashHexStr);
//                        }
//                    } catch (Exception e) {
//                        log.error("{}异常:{}",LOG,e.getMessage(),e);
//                    }
//                }
//            }).start();
//        }
    }

    /**
     * 取出一个infoHash依次请求
     */
    private Metadata run(String infoHashHexStr) {
        try {
            log.info("{}开始新任务.infoHash:{}", LOG, infoHashHexStr);
            Metadata metadata;
            for (AbstractInfoHashParser item : this.currentInfoHashParsers) {
                try {
                    metadata = item.parse(infoHashHexStr);
                } catch (Exception e) {
                    continue;
                }
                if (metadata != null) {
                    log.info("{}成功.infoHash:{}", LOG, infoHashHexStr);
                    return metadata;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }







}
