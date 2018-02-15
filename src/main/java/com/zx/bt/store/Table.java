package com.zx.bt.store;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.entity.Node;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
    private static final LoadingCache<String, Node> cache = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(1024)
//            .expireAfterAccess(30, TimeUnit.MINUTES)
            //传入缓存加载策略,key不存在时调用该方法返回一个value回去
            //此处直接返回空
            .build(key -> null);


    /**
     * 存入节点
     */
    public void put(Node node) {
        cache.put(node.getNodeId(), node);
    }

    /**
     * 获取节点
     */
    public Node get(String nodeId) {
        return cache.get(nodeId);
    }

    /**
     * 获取所有节点
     */
    public Collection<Node> getAll() {
        return cache.asMap().values();
    }
}
