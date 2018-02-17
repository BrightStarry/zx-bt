package com.zx.bt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-17 10:38
 * infoHash表中的type字段的枚举
 */
@Getter
@AllArgsConstructor
public enum InfoHashTypeEnum implements CodeEnum<Integer> {
    GET_PEERS(0, "从其他节点的GET_PEERS请求获取到的,通常丢弃"),
    ANNOUNCE_PEER(1, "从其他节点的ANNOUNCE_PEER请求获取到,通常有用,可直接从其peer下载种子信息"),
    ;
    private Integer code;
    private String message;
}
