package com.zx.bt.store;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-19 15:56
 * 路由表
 * 使用Trie Tree实现, 空间换取时间, 插入和查询复杂度都为O(k),k为key的长度,此处key为nodeId,即160位
 */
@Component
public class RoutingTable {

    private final Config config;

    private final byte[] nodeId;

    //字典树最大层数
    private static final int MAX_PREFIX_LEN = 160;

    //节点最大存储Node数
    private static final int MAX_NODE_NUM = 8;

    //根节点
    private TrieNode root;

    /**
     * 字典树-节点
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class TrieNode {
        //当前节点的层数(root为-1,第二层节点从0开始,最大为159)
        private int prefixLen;
        //当前节点存储的Node数量
        private int count = 0;
        //保存子节点的引用,此处其大小为2,保存下一位0 或 1
        private TrieNode next[] = new TrieNode[2];
        //Bucket:存储最多8个Node
        private Node[] nodes = new Node[MAX_NODE_NUM];

    }

    /**
     * 初始化
     *
     * @param config
     */
    public RoutingTable(Config config) {
        this.config = config;
        nodeId = config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1);
        //初始化根节点
        root = new TrieNode().setPrefixLen(-1).setNodes(null);
        //存入主节点(自己的nodeId)

    }

    /**
     * 新增节点
     */
    public boolean put(Node node) {
        byte[] nodeId = CodeUtil.hexStr2Bytes(node.getNodeId());

        TrieNode currentNode = root;
        for (int i = 0; i < 160; i++) {
            //获取下一节点(根据nodeId字节数组的第i位)
            TrieNode nextNode = currentNode.next[CodeUtil.getBitByBytes(nodeId, i)];
            //如果下一节点不为空
            if (nextNode != null) {
                currentNode = nextNode;
            }
            //如果该节点未存满
            if (currentNode.count < MAX_NODE_NUM) {
                currentNode.nodes[currentNode.count] = node;
                currentNode.count++;
                //TODO nodes排序
            } else {
                //如果存满了
                //计算该nodeId和自己的nodeId的异或值
                byte[] xorResult = CodeUtil.bytesXorBytes(nodeId, nodeId);
                //获取该异或值的为0的前x位
                int topN = CodeUtil.getTopNZeroByBytes(xorResult);
                //如果当前节点层数小于该topN,也就是说 当前路径是自己的nodeId的前缀
                if (i <= topN) {

                }
                //否则抛弃该NodeId
            }

        }


        //外层循环字节, 最多20次
        for (int i = 0; i < nodeId.length; i++) {
            //获取该字节的bit数组
            byte[] bitArr = CodeUtil.byte2Bit(nodeId[i]);
            //内层循环每个bit,最多8次
            for (int j = 0; j < MAX_NODE_NUM; j++) {

            }
        }

        return false;
    }
}
