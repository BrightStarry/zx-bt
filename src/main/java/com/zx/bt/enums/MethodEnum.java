package com.zx.bt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-14 10:31
 * KRPC协议方法枚举
 */
@Getter
@AllArgsConstructor
public enum MethodEnum implements CodeEnum<String>{
    PING("ping", "检测目标状态"),
    FIND_NODE("find_node", "查找目标主机"),
    GET_PEERS("get_peers", "查找拥有某种子的目标主机"),
    ANNOUNCE_PEER("announce_peer", "通知其他主机,该主机有对某种子的上传下载"),
    ;

    private String code;
    private String message;

}
