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
import java.util.Date;

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
     * 如果是announce_peer类型(type == 1),则保存其peer的ip:ports
     */
    private String peerAddress = "";

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    public InfoHash(String infoHash,  String peerAddress) {
        this.infoHash = infoHash;
        this.peerAddress = peerAddress;
    }

    public InfoHash(String infoHash) {
        this.infoHash = infoHash;
    }
}
