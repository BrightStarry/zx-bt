package com.zx.bt.spider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.service.MetadataService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-05-16 11:52
 * 从文本文件中读取数据，导入回es
 */
@Slf4j
public class ReadFileToEsTest extends SpiderApplicationTests{


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MetadataService metadataService;

    @Test
    @SneakyThrows
    public void test() {
        // 使用行迭代器，迭代该文件的每一行
        File file = new File("C:\\Users\\97038\\Desktop\\a.txt");
        LineIterator lineIterator = FileUtils.lineIterator(file);
        int i = 0;

        // 构造泛型集合，用于ObjectMapper的转换
        JavaType type = getCollectionType(List.class, Metadata.class);

        // 遍历每一行。读取为 list，存入es
        while (lineIterator.hasNext()) {
            String str = lineIterator.nextLine();
            List<Metadata> metadataList = (List<Metadata>) objectMapper.readValue(str, type);
            // 将es中的_id设为null，才能添加
            metadataList.parallelStream().forEach(item -> item.set_id(null));

            metadataService.batchInsert(metadataList);

            i += metadataList.size();
            log.info("当前数:{}",i);
        }
    }


    /**
     * 获取泛型的Collection Type
     */
    public <C extends Collection, E> JavaType getCollectionType(Class<C> collectionClass, Class<E> elementClass) {
        return objectMapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }
}
