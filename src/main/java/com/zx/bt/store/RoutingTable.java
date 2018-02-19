package com.zx.bt.store;

import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-19 15:56
 * 路由表
 * 使用Trie Tree实现, 空间换取时间, 插入和查询复杂度都为O(k),k为key的长度,此处key为nodeId,即160位
 */
@Component
public class RoutingTable {


    /**
     * 字典树-节点
     */
    public static class TrieNode{
        //保存子树的引用,此处其大小为2,保存下一位0 或 1
        private TrieNode next[] = new TrieNode[2];

    }
}
