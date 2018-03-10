package com.zx.bt.common.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zx.bt.common.dto.MetadataDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.Date;

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
// 此处忽略空值,是因为在es新增方法中,需要将该对象转为json,但新增时不能携带_id参数.所以需要忽略.
// 之所以不能直接在字段上忽略,是因为在传到前端时,需要携带_id参数.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metadata {

    @Id
    @GeneratedValue()
    private Long id;

    @Transient
    private String _id;

    /**
     * infoHash信息,16进制形式
     */
    private String infoHash;

    /**
     * 文件信息 json
     * {@link MetadataDTO#infos} 的json string
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
     * 热度
     */
    @Transient
    private Long hot = 0L;

    /**
     * 创建时间
     */
    private Date createTime = new Date();

    /**
     * 修改时间
     */
    private Date updateTime = new Date();








    public Metadata(String infoHash, String infoString, String name, Long length, Integer type) {
        this.infoHash = infoHash;
        this.infoString = infoString;
        this.name = name;
        this.length = length;
        this.type = type;
    }
}
