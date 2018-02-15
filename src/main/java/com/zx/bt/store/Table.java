package com.zx.bt.store;

import com.zx.bt.dto.Node;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author:ZhengXing
 * datetime:2018-02-15 14:48
 * 路由表
 * 用于存储节点
 */
@Component
public class Table {

    /**
     * 临时存储节点
     */
    private Map<String, Node> nodeMap = new ConcurrentHashMap<>();


    /**
     * 存入节点
     */
    public void put(Node node) {
        nodeMap.put(node.getId(), node);
    }

    /**
     * 获取节点
     */
    public Node get(String nodeId) {
        return nodeMap.get(nodeId);
    }

    /**
     * 获取所有节点
     */
    public Collection<Node> getAll() {
        return nodeMap.values();
    }
}
