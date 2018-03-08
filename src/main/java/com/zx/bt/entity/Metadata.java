package com.zx.bt.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.enums.LengthUnitEnum;
import com.zx.bt.enums.MetadataTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018/3/6 0006 16:46
 * 种子 metadata 信息
 * <p>
 * 可参考如下bencode string
 * d5:filesld6:lengthi898365e4:pathl2:BK12:IMG_0001.jpgeed6:lengthi1042574e4:pathl2:BK12:IMG_0002.jpgeed6:lengthi2346980e4:pathl2:BK12:IMG_0003.jpgeed6:lengthi2129668e4:pathl2:BK12:IMG_0004.jpgeed6:lengthi1221991e4:pathl2:BK12:IMG_0005.jpgeed6:lengthi1093433e4:pathl2:BK12:IMG_0006.jpgeed6:lengthi1644002e4:pathl2:BK12:IMG_0007.jpgeed6:lengthi580397e4:pathl2:BK12:IMG_0008.jpgeed6:lengthi481513e4:pathl2:BK12:IMG_0009.jpgeed6:lengthi1006799e4:pathl2:BK12:IMG_0010.jpgeed6:lengthi144512e4:pathl10:Cover1.jpgeed6:lengthi259951e4:pathl10:Cover2.jpgeed6:lengthi25669111e4:pathl4:FLAC36:01. ろまんちっく☆2Night.flaceed6:lengthi28988677e4:pathl4:FLAC22:02. With…you….flaceed6:lengthi24600024e4:pathl4:FLAC51:03. ろまんちっく☆2Night (Instrumental).flaceed6:lengthi27671024e4:pathl4:FLAC37:04. With…you… (Instrumental).flaceee4:name166:[얼티메이트] [130904] TVアニメ「神のみぞ知るセカイ」神のみキャラCD.000 エルシィ&ハクア starring 伊藤かな恵&早見沙織 (FLAC+BK)12:piece lengthi131072ee
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
@DynamicUpdate
public class Metadata {

    @Id
    @GeneratedValue()
    private Long id;

    /**
     * infoHash信息,16进制形式
     */
    private String infoHash;

    /**
     * 文件信息 json
     * {@link Metadata#infos} 的json string
     */
    private String infoString;

    /**
     * 名字
     */
    private String name;

    /**
     * 总长度(所有文件相加长度)
     */
    private Long length;

    /**
     * 种子类型
     */
    private Integer type;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;



    /**
     * 文件信息对象 List 不存入数据库
     */
    @Transient
    private List<Info> infos;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Info {
        /**
         * 名字
         * 如果是单文件为名字, 如果为多文件为路径, 如果为多文件多级路径,"/"分割,也就是文件名.
         */
        private String name;

        /**
         * 长度
         */
        private Long length;
    }

    /**
     * metadata解析后的Map 转 该对象
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static Metadata map2Metadata(Map<String, Object> rawMap, ObjectMapper objectMapper, String infoHashHesStr) {
        Metadata metadata = new Metadata();
        List<Info> infos;
        //名字/infoHash
        metadata.setName((String) rawMap.get("name")).setInfoHash(infoHashHesStr);
        //总长度
        long length = 0;
        //多文件
        if (rawMap.containsKey("files")) {
            List<Map<String, Object>> files = (List<Map<String, Object>>) rawMap.get("files");
            infos = files.parallelStream().map(file -> {
                Info info = new Info();
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
            for (Info info : infos) {
                length += info.getLength();
            }
            metadata.setLength(length);
        } else {
            length = (long) rawMap.get("length");
            infos = Collections.singletonList(new Info(metadata.getName(), length));
            metadata.setLength(length);
        }
        //infos /  原始map转json / infos转json
        return metadata.setInfos(infos).setInfoString(objectMapper.writeValueAsString(infos))
                .setType(MetadataTypeEnum.PEER.getCode());
    }

    public Metadata(String infoHash, String infoString, String name, Long length, Integer type, List<Info> infos) {
        this.infoHash = infoHash;
        this.infoString = infoString;
        this.name = name;
        this.length = length;
        this.type = type;
        this.infos = infos;
    }
}
