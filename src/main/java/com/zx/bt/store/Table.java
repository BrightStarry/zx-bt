package com.zx.bt.store;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * author:ZhengXing
 * datetime:2018-02-15 14:48
 * 路由表
 * 用于存储节点
 *
 * 废弃, 已使用字典树实现
 * see {@link RoutingTable}
 */
@Deprecated
@Slf4j
public class Table {

    private final Config config;
    private final String ip;

    /**
     * 临时存储节点
     */
    private  final LoadingCache<String, Node> cache ;

    public Table(Config config) {
        this.config = config;
        this.cache = Caffeine.newBuilder()
//                .initialCapacity(config.getMain().getTableLen())
//                .maximumSize(config.getMain().getTableLen())
//            .expireAfterAccess(30, TimeUnit.MINUTES)
                //传入缓存加载策略,key不存在时调用该方法返回一个value回去
                //此处直接返回空
                .build(key -> null);
        this.ip = config.getMain().getIp();
    }


    /**
     * 存入节点
     */
    public void put(Node node) {
//        log.info("[路由表]加入节点:{}",node);
        if(!ip.equals(node.getIp()))
            cache.put(node.getNodeId(), node);
    }


    /**
     * 删除节点
     */
    public void remove(String nodeId) {
        cache.invalidate(nodeId);
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

    /**
     * 获取前8个节点
     */
    public List<Node> getTop8Nodes() {
        Node[] srcNodes = cache.asMap().values().toArray(new Node[0]);
        Node[] resultNodes = new Node[8];
        System.arraycopy(srcNodes,0,resultNodes,0,8);
        return Arrays.stream(resultNodes).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
