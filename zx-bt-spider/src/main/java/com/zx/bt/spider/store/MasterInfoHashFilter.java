package com.zx.bt.spider.store;

import com.google.common.hash.BloomFilter;
import com.zx.bt.spider.SpiderApplication;
import com.zx.bt.spider.config.Config;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.util.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018-03-08 20:26
 * infoHash 过滤器 去重
 *
 * 主节点使用
 * 简单封装guava的{@link com.google.common.hash.BloomFilter}
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zx-bt.main",name = "master",havingValue = "true")
public class MasterInfoHashFilter implements InfoHashFilter {
    private static final String LOG = "[InfoHashFilter]";

    /**
     * 布隆过滤器
     */
    private volatile BloomFilter<String> filter;
    private final Config config;
    private final MetadataService metadataService;

    /**
     * 过滤器是否可用标识
     * true: 可用
     * false: 暂不可用
     */
    private volatile boolean available = false;

    public MasterInfoHashFilter(Config config, MetadataService metadataService) {
        this.config = config;
        this.metadataService = metadataService;
    }

    /**
     * 加入
     */
    @Override
    public void put(String infoHash){
        filter.put(infoHash);
    }

    /**
     * 判断是否存在
     * 当过滤器暂不可用, 此期间调用全部返回true,暂不接收任何info_hash
     */
    @Override
    public boolean contain(String infoHash) {
        return !available ||  filter.mightContain(infoHash);
    }

    /**
     * 预期长度
     */
    @Override
    public long size() {
        return filter.approximateElementCount();
    }

    /**
     * 启用/禁用
     */
    private void setAvailable(boolean available) {
        this.available = available;
        metadataService.setInfoHashFilterAvailable(available);
    }

    /**
     * 启动该过滤器
     */
    @Override
    public void run() {
        importExistInfoHash();
        resetTimer();
    }

    /**
     * 导入所有入库种子信息到过滤器
     */
    private void importExistInfoHash() {
        try {
            log.info("{}正在初始化过滤器...",LOG);
            //禁用
            setAvailable(false);
            filter = BloomFilter.create((str, primitiveSink) -> primitiveSink.putBytes(CodeUtil.hexStr2Bytes(str)),
                    config.getPerformance().getInfoHashFilterMaxNum(), config.getPerformance().getInfoHashFilterFpp());

            //每次查询条数
            int size = config.getEs().getImportExistInfoHashPageSize();
            //查询超时时间
            int timeoutSecond =  config.getEs().getImportExistInfoHashTimeoutSecond();
            //总条数 总页数
            MetadataService.ScrollResult<String> scrollResult = metadataService.preListFindInfoHash(size, timeoutSecond);
            //第一页数据
            List<String> infoHashs = scrollResult.getList();
            //scrollId
            String scrollId = scrollResult.getScrollId();

            while(CollectionUtils.isNotEmpty(infoHashs)){
                //将其加入过滤器
                infoHashs.parallelStream().forEach(filter::put);
                //查询下一页数据
                infoHashs = metadataService.listFindInfoHash(scrollId, timeoutSecond);
            }

            //清除分页
            metadataService.clearScroll(scrollId);

            //启用
            setAvailable(true);

            log.info("{}初始化完成.当前总长度:{}",LOG,size());
        } catch (Exception e) {
            log.error("{}初始化失败,程序崩溃.异常:{}", e.getMessage(), e);
            // TODO 发送邮件通知
            SpiderApplication.exit();
        }
    }

    /**
     * 启动定时器,定时重置该过滤器,防止其长度过长
     */
    private void resetTimer() {
        //重新执行导入方法即可
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(this::importExistInfoHash,
                        config.getMain().getInfoHashFilterResetHours(),
                config.getMain().getInfoHashFilterResetHours(),
                TimeUnit.HOURS);
    }

}
