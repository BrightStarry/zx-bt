package com.zx.bt.store;

import com.zx.bt.config.Config;
import com.zx.bt.entity.Node;
import com.zx.bt.exception.BTException;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import sun.text.normalizer.Trie;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author:ZhengXing
 * datetime:2018-02-19 15:56
 * 路由表
 * 使用Trie Tree实现, 空间换取时间, 插入和查询复杂度都为O(k),k为key的长度,此处key为nodeId,即160位
 *
 *
 */
@Component
public class RoutingTable {
    private static final String LOG = "[路由表]";

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
    private LongAdder count = new LongAdder();

    //非自己的nodeId分支, 最大可存储的层数
    private int maxStorePrefixLen;

    //分段锁
    private final ReentrantLock[] locks;

    //锁数量
    private final int lockNum;

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
            int index = containForIndex(nodeId);
            return index == -1 ? null : nodes[index];
        }

        /**
         * 判断该节点的nodes中是否包含某个Node, 返回索引
         */
        public int containForIndex(byte[] nodeId) {
            for (int i = 0; i < count; i++) {
                if (CodeUtil.equalsBytes(nodeId, CodeUtil.hexStr2Bytes(nodes[i].getNodeId())))
                    return i;
            }
            return -1;
        }
    }

    /**
     * 初始化
     *
     * @param config
     */
    public RoutingTable(Config config) {
        //参数
        this.config = config;
        nodeId = config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1);
        maxStorePrefixLen = config.getMain().getRoutingTablePrefixLen();
        lockNum = config.getMain().getRoutingTableLockNum();

        //初始化锁
        locks = new ReentrantLock[lockNum];
        for (int i = 0; i < lockNum; i++) {
            locks[i] = new ReentrantLock(true);
        }

        //初始化根节点
        root = new TrieNode().setPrefixLen(-1).setNodes(null);
        root.getNext()[0] = new TrieNode(0);
        root.getNext()[1] = new TrieNode(0);
        //存入主节点(自己的nodeId)
        put(new Node(CodeUtil.bytes2HexStr(config.getMain().getNodeId().getBytes(CharsetUtil.ISO_8859_1)), config.getMain().getIp(), config.getMain().getPort(),Integer.MAX_VALUE));
    }

    public static void main(String[] args) {
        //创建
        RoutingTable routingTable = new RoutingTable(new Config().setMain(
                new Config.Main().setNodeId(BTUtil.generateNodeIdString())
                        .setIp("106.14.7.29")
                        .setPort(6881)
        )
        );


        for (int i = 0; i < 1; i++) {
            new Thread(()->{
                for (int j = 0; j < 999999; j++) {
                    try {
                        Node node = new Node(CodeUtil.bytes2HexStr(BTUtil.generateNodeId()), "106.14.7.29", j);
                        System.out.println(routingTable.put(node));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 1; i++) {
            new Thread(()->{
                for (int j = 0; j < 999999; j++) {
                    try {
                        routingTable.getForTop8(BTUtil.generateNodeId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }






    }

    /**
     * 新增若干节点
     */
    public void putAll(Node... nodes) {
        if(ArrayUtils.isNotEmpty(nodes))
            for (Node node : nodes) {
                put(node);
            }
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
            //如果已经包含该nodeId
            int oldNodeIndex;
            if ((oldNodeIndex = currentNode.containForIndex(nodeId)) != -1) {
                //累加rank值,更新最后更新时间,暂不考虑nodeId冲突的可能
                currentNode.nodes[oldNodeIndex].addRank(node.getRank()).setLastActiveTime(new Date());
                return true;
            }
            //如果该节点未存满
            if (currentNode.count < MAX_NODE_NUM) {
                currentNode.nodes[currentNode.count++] = node;
                this.count.increment();
                return true;
            }
            //如果存满了,进行分裂,然后递归执行本方法,将节点插入
            //如果当前节点包含自己的节点并且小于160,或者不超过x层
            if (currentNode.contain(this.nodeId) != null && i != MAX_PREFIX_LEN || i <= maxStorePrefixLen) {
                //将nodes分裂
                //创建出子节点
                currentNode.getNext()[0] = new TrieNode(i);
                currentNode.getNext()[1] = new TrieNode(i);
                //获取当前节点保存的nodes
                Node[] nodes = currentNode.getNodes();
                //将原来的所有节点分配到新节点
                for (Node itemNode : nodes) {
                    //获取node的第i位(下一位)二进制值
                    byte nextBit = CodeUtil.getBitByBytes(CodeUtil.hexStr2Bytes(itemNode.getNodeId()), i);
                    //给对应子节点的nodes赋值
                    currentNode.getNext()[nextBit].getNodes()[currentNode.getNext()[nextBit].count++] = itemNode;
                }
                //清空当前节点
                currentNode.setCount(0).setNodes(null);
                //此处不能直接新增新节点,因为当所有旧节点都被分配到同一子节点时,会导致仍需分裂节点,所以使用递归(也可循环)

                return put(node);
            }
            //否则抛弃该NodeId
            return false;
        }
        return false;
    }

    /**
     * 删除某节点,根据节点id
     */
    public boolean delete(byte[] nodeId) {
        TrieNode trieNode = get(nodeId);
        //如果该节点不存在
        if (trieNode == null)
            return false;
        Node[] nodes = trieNode.nodes;
        //循环保存了该节点的trieNode的nodes
        for (int i = 0; i < trieNode.count; i++) {
            //如果有相同的
            if (CodeUtil.equalsBytes(nodeId, CodeUtil.hexStr2Bytes(nodes[i].getNodeId()))) {
                nodes[i] = null;
                //如果不是末尾,将末尾的值赋值到该索引
                if (i != trieNode.count - 1) {
                    nodes[i] = nodes[trieNode.count - 1];
                    nodes[trieNode.count - 1] = null;
                }
                //当前trieNode的node数量-1
                trieNode.count--;
                //路由表的node数量-1
                this.count.decrement();
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
                if (currentNode.count == 0)
                    return null;
                Node node = currentNode.contain(nodeId);
                return node == null ? null : currentNode;
            }
        }
        return null;
    }

    /**
     * 搜索和指定nodeId最近的8个node或 指定nodeId自己
     */
    public List<Node> getForTop8(byte[] nodeId) {
        List<Node> nodes = new LinkedList<>();
        TrieNode currentNode = root;
        TrieNode lastNode = null;//上一节点
        for (int i = 0; i <= MAX_PREFIX_LEN; i++) {
            //获取下一节点(根据nodeId字节数组的第i位)
            TrieNode nextNode = currentNode.next[CodeUtil.getBitByBytes(nodeId, i)];
            //如果下一节点不为空
            if (nextNode != null) {
                lastNode = currentNode;
                currentNode = nextNode;
                continue;
            }
            //为空则搜索该节点的nodes
            //如果nodes不为空
            if (currentNode.count != 0) {
                //查找node
                Node node = currentNode.contain(nodeId);
                //找到了，直接返回
                if (node != null) {
                    nodes.add(node);
                    return nodes;
                }
                //否则将该trieNode中的所有node放到返回集合中
                nodes.addAll(Arrays.asList(currentNode.nodes).subList(0, currentNode.count));
            }
            if(nodes.size() == MAX_NODE_NUM )
                return nodes;
            //如果list的长度没到8（说明nodes为空，或者nodes的长度不足8）,就去拥有相同父节点的隔壁节点找
            //(因为是由8个节点分裂而来，所有在未主动删除的情况下，是可以集齐8个的)
            byte lastIndex = CodeUtil.getBitByBytes(nodeId, i - 1);//上个节点，进入currentNode时的bit
            //此处lastNode不会为空，因为当i==0，currentNode为root时，必然会进入一次该lastNode赋值的循环体.
            TrieNode findNode = lastNode.next[lastIndex == 0 ? 1 : 0];//隔壁节点
            //要返回的nodes缺少的节点个数
            int lackNum = MAX_NODE_NUM - nodes.size();
            if(findNode.count != 0)
                //从隔壁节点截取若干节点添加到返回节点. 此处限制其数量,防止IndexOutOfBoundsException
                nodes.addAll(Arrays.asList(findNode.nodes).subList(0, lackNum <= findNode.count ? lackNum : findNode.count ));
            return nodes;
        }
        return nodes;
    }

    /**
     * 遍历节点,并使用指定函数 操作nodes
     */
    private void loop(TrieNode node, Consumer<TrieNode> consumer) {
        if (node.next[0] != null) loop(node.next[0], consumer);
        if (node.next[1] != null) loop(node.next[1], consumer);
        if (node.count > 0) consumer.accept(node);
    }

    /**
     * 遍历节点,并使用指定函数 操作nodes
     * 封装,去除一个参数
     */
    public void loop(Consumer<TrieNode> consumer) {
        loop(this.root,consumer);
    }

    /**
     * 长度
     */
    public long size() {
        return this.count.longValue();
    }



}
