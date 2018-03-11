package com.zx.bt.spider.dto;

import com.zx.bt.common.store.CommonCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-12 0:01
 *
 * get_peers发送信息
 * 该类被保存在该缓存中
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class GetPeersSendInfo {

    private String infoHash;

    /**
     * 已发送get_peers请求的nodeIds
     */
    private List<byte[]> sentNodeIds = new LinkedList<>();

    /**
     * 判断当前对象的sentNodeIds是否包含传入的nodeId
     */
    public boolean contains(byte[] nodeId) {
        return sentNodeIds.contains(nodeId);
    }

    /**
     * 将对象加入到sentNodeIds
     */
    public GetPeersSendInfo put(List<byte[]> bytes) {
        if (CollectionUtils.isEmpty(bytes))
            return this;
        bytes.forEach(this::put);
        return this;
    }

    /**
     * 将对象加入到sentNodeIds
     */
    public GetPeersSendInfo put(byte[] bytes) {
        if(!sentNodeIds.contains(bytes))
            sentNodeIds.add(bytes);
        return this;
    }

    public GetPeersSendInfo(String infoHash) {
        this.infoHash = infoHash;
    }
}
