package com.zx.bt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:43
 * 存储info_hash信息
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
@DynamicUpdate
public class InfoHash {

    @Id
    @GeneratedValue()
    private Long id;

    /**
     * infoHash信息,16进制形式
     */
    private String infoHash;

    /**
     * 类型
     * see {@link com.zx.bt.enums.InfoHashTypeEnum}
     */
    private Integer type;

    /**
     * 如果是announce_peer类型(type == 1),则保存其peer的ip:ports
     */
    private String peerAddress = "";

    public InfoHash(String infoHash, Integer type, String peerAddress) {
        this.infoHash = infoHash;
        this.type = type;
        this.peerAddress = peerAddress;
    }

    public InfoHash(String infoHash, Integer type) {
        this.infoHash = infoHash;
        this.type = type;
    }
}
