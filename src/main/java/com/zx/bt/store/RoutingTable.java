package com.zx.bt.store;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

/**
 * author:ZhengXing
 * datetime:2018-02-19 15:56
 * 路由表
 * 使用Trie Tree实现, 空间换取时间, 插入和查询复杂度都为O(k),k为key的长度,此处key为nodeId,即160位
 */
@Component
public class RoutingTable {

    private final Config config;

    //自己的nodeId
    private final byte[] nodeId;

    //字典树最大层数
    private static final int MAX_PREFIX_LEN = 160;

    //节点最大存储Node数
    private static final int MAX_NODE_NUM = 8;

    //根节点
    private TrieNode root;

    //总node个数
    private LongAdder count;

    //非自己的nodeId分支, 最大可存储的层数
    private int maxStorePrefixLen;

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

        public TrieNode(int prefixLen) {
            this.prefixLen = prefixLen;
        }


        /**
         * 判断该节点的nodes中是否包含某个Node
         */
        public Node contain(byte[] nodeId) {
            for (Node node : nodes) {
                if(node != null && CodeUtil.equalsBytes(nodeId, CodeUtil.hexStr2Bytes(node.getNodeId())))
                    return node;
            }
            return null;
        }
    }

    /**
     * 初始化
     *
     * @param config
     */
    public RoutingTable(Config config) {
        this.config = config;
        nodeId = config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1);
        maxStorePrefixLen = config.getMain().getRoutingTablePrefixLen();
        //初始化根节点
        root = new TrieNode().setPrefixLen(-1).setNodes(null);
        root.getNext()[0] = new TrieNode(0);
        root.getNext()[1] = new TrieNode(0);
        //存入主节点(自己的nodeId)
        put(new Node(CodeUtil.bytes2HexStr(config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1)), config.getMain().getIp(), config.getMain().getPort()));
    }

    public static void main(String[] args) {
        //创建
        RoutingTable routingTable = new RoutingTable(new Config().setMain(
                new Config.Main().setNodeId(BTUtil.generateNodeIdString())
                .setIp("106.14.7.29")
                .setPort(6881)
        )
        );

        boolean b = false;
        Node node1 = new Node(CodeUtil.bytes2HexStr(BTUtil.generateNodeId()), "106.14.7.29", 2);
        for (int i = 0; i < 1000000; i++) {
            Node node = new Node(CodeUtil.bytes2HexStr(BTUtil.generateNodeId()), "106.14.7.29", i);
               b = routingTable.put(node);
            System.out.println(b);
        }

        System.out.println(routingTable.count);

//        boolean delete = routingTable.delete(routingTable.nodeId);
        TrieNode trieNode = routingTable.get(routingTable.nodeId);
        System.out.println(trieNode);



    }

    /**
     * 新增节点
     */
    public boolean put(Node node) {
        byte[] nodeId = CodeUtil.hexStr2Bytes(node.getNodeId());

        TrieNode currentNode = root;
        for (int i = 0; i <= MAX_PREFIX_LEN; i++) {
            //获取下一节点(根据nodeId字节数组的第i位)
            TrieNode nextNode = currentNode.next[CodeUtil.getBitByBytes(nodeId, i)];
            //如果下一节点不为空
            if (nextNode != null) {
                currentNode = nextNode;
                continue;
            }
            //如果该节点未存满
            if (currentNode.count < MAX_NODE_NUM) {
                currentNode.nodes[currentNode.count++] = node;
                count.increment();
                return true;
            }
            //如果存满了
            //如果当前节点包含自己的节点,或者不超过x层
            if (currentNode.contain(this.nodeId) != null && i != MAX_PREFIX_LEN || i <= maxStorePrefixLen) {
                //将nodes分裂
                //创建出子节点
                currentNode.getNext()[0] = new TrieNode(i);
                currentNode.getNext()[1] = new TrieNode(i);
                //获取当前节点保存的nodes
                Node[] nodes = currentNode.getNodes();
                for (Node itemNode : nodes) {
                    //获取node的第i位(下一位)二进制值
                    byte nextBit = CodeUtil.getBitByBytes(CodeUtil.hexStr2Bytes(itemNode.getNodeId()), i);
                    //给对应子节点的nodes赋值
                    currentNode.getNext()[nextBit].getNodes()[currentNode.getNext()[nextBit].count++] = itemNode;
                }
                //清空当前节点
                currentNode.setCount(0).setNodes(null);
                count.increment();
                return true;
            }
            //否则抛弃该NodeId
            return false;
        }
        return false;
    }

    /**
     * 删除某节点
     */
    public boolean delete(byte[] nodeId) {
        TrieNode trieNode = get(nodeId);
        //如果该节点不存在
        if(trieNode == null)
            return false;
        Node[] nodes = trieNode.nodes;
        //循环保存了该节点的trieNode的nodes
        for (int i = 0; i < trieNode.count; i++) {
            //如果有相同的
            if (CodeUtil.equalsBytes(nodeId, CodeUtil.hexStr2Bytes(nodes[i].getNodeId()))) {
                trieNode.nodes[i] = null;
                //如果不是末尾,将末尾的值赋值到该索引
                if (i != trieNode.count - 1) {
                    trieNode.nodes[i] = trieNode.nodes[trieNode.count-1];
                    trieNode.nodes[trieNode.count - 1] = null;
                }
                trieNode.count--;
                return true;
            }
        }
        return false;
    }

    /**
     * 搜索包含指定node的节点(trieNode)
     */
    public TrieNode get(byte[] nodeId) {
        TrieNode currentNode = root;
        for (int i = 0; i <= MAX_PREFIX_LEN; i++) {
            //获取下一节点(根据nodeId字节数组的第i位)
            TrieNode nextNode = currentNode.next[CodeUtil.getBitByBytes(nodeId, i)];
            //如果下一节点不为空
            if (nextNode != null) {
                currentNode = nextNode;
            } else {
                //为空则搜索该节点的nodes
                if(currentNode.count == 0)
                    return null;
                Node node = currentNode.contain(nodeId);
                return node == null ? null : currentNode;
            }
        }
        return null;
    }
}
