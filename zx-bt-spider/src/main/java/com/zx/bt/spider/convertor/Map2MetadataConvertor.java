package com.zx.bt.spider.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import lombok.SneakyThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018-03-10 22:28
 * Map 转 {@link com.zx.bt.common.entity.Metadata}
 */
public class Map2MetadataConvertor {

    /**
     * metadata解析后的Map 转 该对象
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static Metadata convert(Map<String, Object> rawMap, ObjectMapper objectMapper, String infoHashHesStr) {
        Metadata metadata = new Metadata();
        List<MetadataVO.Info> infos;
        //名字/infoHash
        metadata.setName((String) rawMap.get("name")).setInfoHash(infoHashHesStr);
        //总长度
        long length = 0;
        //多文件
        if (rawMap.containsKey("files")) {
            List<Map<String, Object>> files = (List<Map<String, Object>>) rawMap.get("files");
            infos = files.parallelStream().map(file -> {
                MetadataVO.Info info = new MetadataVO.Info();
                info.setLength((long) file.get("length"));
                Object pathObj = file.get("path");
                if (pathObj instanceof String) {
                    info.setName((String) pathObj);
                } else if (pathObj instanceof List) {
                    List<String> pathList = (List<String>) pathObj;
                    StringBuilder pathSB = new StringBuilder();
                    for (String item : pathList) {
                        pathSB.append("/").append(item);
                    }
                    info.setName(pathSB.toString());
                }
                return info;
            }).collect(Collectors.toList());
            for (MetadataVO.Info info : infos) {
                length += info.getLength();
            }
            metadata.setLength(length);
        } else {
            length = (long) rawMap.get("length");
            infos = Collections.singletonList(new MetadataVO.Info(metadata.getName(), length));
            metadata.setLength(length);
        }
        //infos /  原始map转json / infos转json
        return metadata.setInfoString(objectMapper.writeValueAsString(infos))
                .setType(MetadataTypeEnum.PEER.getCode());
    }
}
