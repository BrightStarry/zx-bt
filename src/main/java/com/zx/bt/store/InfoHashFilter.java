package com.zx.bt.store;

import com.google.common.hash.BloomFilter;
import com.zx.bt.config.Config;
import com.zx.bt.repository.MetadataRepository;
import com.zx.bt.util.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-08 20:26
 * infoHash 过滤器 去重
 *
 * 简单封装guava的{@link com.google.common.hash.BloomFilter}
 */
@Slf4j
@Component
public class InfoHashFilter {
    private static final String LOG = "[InfoHashFilter]";

    private BloomFilter<String> filter;
    private final MetadataRepository metadataRepository;

    public InfoHashFilter(Config config, MetadataRepository metadataRepository) {
        filter = BloomFilter.create((str, primitiveSink) -> primitiveSink.putBytes(CodeUtil.hexStr2Bytes(str)),
                config.getPerformance().getInfoHashFilterMaxNum(), config.getPerformance().getInfoHashFilterFpp());
        this.metadataRepository = metadataRepository;
    }

    /**
     * 加入
     */
    public void put(String infoHash){
        filter.put(infoHash);
    }

    /**
     * 判断是否存在
     */
    public boolean contain(String infoHash) {
        return filter.mightContain(infoHash);
    }

    /**
     * 预期长度
     */
    public long size() {
        return filter.approximateElementCount();
    }

    /**
     * 导入所有入库种子信息到过滤器
     */
    public void importExistInfoHash() {
        log.info("{}正在初始化过滤器...",LOG);
        //总条数
        long count = metadataRepository.count();
        //每次查询条数
        int size = 10000;
        for (int offer = 0; offer < count; offer += size) {
            List<String> infoHashs = metadataRepository.findInfoHash(offer, size);
            infoHashs.parallelStream().forEach(filter::put);
        }
        log.info("{}初始化完成.当前总长度:{}",LOG,size());
    }


}
