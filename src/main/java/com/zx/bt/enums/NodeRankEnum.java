package com.zx.bt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-19 13:39
 * 节点权重枚举
 */
@Getter
@AllArgsConstructor
public enum NodeRankEnum implements CodeEnum<Integer>{

    FIND_NODE(1, "收到该节点find_node请求"),
    FIND_NODE_RECEIVE(10, "收到该节点find_node有效回复"),
    PING(10, "收到该节点ping请求"),
    PING_RECEIVE(1, "收到该节点ping回复"),
    GET_PEERS(100, "收到该节点get_peers请求"),
    GET_PEERS_RECEIVE(100, "收到该节点get_peers回复"),
    ANNOUNCE_PEER(100000, "收到该节点announce_peer请求"),


    ;

    private Integer code;
    private String message;
}
