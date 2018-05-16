package com.zx.bt.spider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.repository.MetadataRepository;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * author:ZhengXing
 * datetime:2018-05-13 13:46
 * 将es数据导出为文本文件
 */
@Slf4j
public class ExportESDataToFileTest extends SpiderApplicationTests {

    @Autowired
    private  MetadataService metadataService;
    @Autowired
    private  ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    public void importTest() {
        final File file = new File(File.separator + "a.txt");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        int total = 0;
        //每次查询条数
        int size = 1000;
        //查询超时时间
        int timeoutSecond =  3000;
        //总条数 总页数
        MetadataService.ScrollResult<Metadata> scrollResult = metadataService.preListFindMetadata(size, timeoutSecond);
        //第一页数据
        List<Metadata> metadataList = scrollResult.getList();
        try {
            writeStringToFile(file,objectMapper.writeValueAsString(metadataList),true);
        } catch (Exception e) {
            log.error("发生异常：",e);
        }
        //scrollId
        String scrollId = scrollResult.getScrollId();

        while(CollectionUtils.isNotEmpty(metadataList)){

            //查询下一页数据
            MetadataService.ListFindResult<Metadata> result = metadataService.listFindMetadata(scrollId, timeoutSecond);
            metadataList = result.getList();
            scrollId = result.getScrollId();
            final List<Metadata> metadatas = metadataList;
            total += metadatas.size();
            log.info("当前总数：{}",total);
            executorService.execute(()->{
                try {
                    writeStringToFile(file,objectMapper.writeValueAsString(metadatas),true);
                } catch (Exception e) {
                    log.error("发生异常：",e);
                }
            });
        }

        //清除分页
        metadataService.clearScroll(scrollId);

        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.HOURS);

    }


    private  static final String LINE_BREAK = System.getProperty("line.separator");
    /**
     * 将string写入文件,追加
     * @param isLine 是否分行
     */
    public void writeStringToFile(File file, String data, boolean isLine){
        try {
            FileUtils.writeStringToFile(file,
                    data + (isLine ? LINE_BREAK : ""),
                    CharsetUtil.UTF_8,
                    true);
        } catch (IOException e) {
            log.error("[文件存取器]写入文件失败.e:{}",e.getMessage(),e);
        }
    }


}
