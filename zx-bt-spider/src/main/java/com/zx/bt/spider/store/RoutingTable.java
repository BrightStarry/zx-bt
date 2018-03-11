package com.zx.bt.spider.store;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.entity.Node;
import com.zx.bt.spider.enums.NodeRankEnum;
import com.zx.bt.common.util.CodeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * author:ZhengXing
 * datetime:2018-02-19 15:56
 * 路由表
 * 使用Trie Tree实现, 空间换取时间, 插入和查询复杂度都为O(k),k为key的长度,此处key为nodeId,即160位
 */
@Slf4j
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

    //分段锁-锁的选择，使用当前层数 % lockNum，作为数组标
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
        //锁id
        private Integer lockId = generateLockId();


        public TrieNode(int prefixLen) {
            this.prefixLen = prefixLen;
        }


        //锁id生成器
        private static AtomicInteger lockIdGenerator = new AtomicInteger(0);

        /**
         * 生成锁id
         */
        private int generateLockId() {
            int result = lockIdGenerator.getAndIncrement();
            if (result > 1 << 20)
                lockIdGenerator.lazySet(0);
            return result;
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
                if (nodes != null && CodeUtil.equalsBytes(nodeId, nodes[i].getNodeIdBytes()))
                    return i;
            }
            return -1;
        }

        /**
         * 分裂该节点
         */
        public void split() {
            //将nodes分裂
            //创建出子节点
            this.next[0] = new TrieNode(this.prefixLen + 1);
            this.next[1] = new TrieNode(this.prefixLen + 1);
            //将原来的所有节点分配到新节点
            for (Node itemNode : this.nodes) {
                //获取itemNode的下一位二进制值
                byte nextBit = CodeUtil.getBitByIndex(itemNode.getNodeIdBytes(), this.prefixLen + 1);
                //给对应子节点的nodes赋值
                this.next[nextBit].getNodes()[this.next[nextBit].count++] = itemNode;
            }
            //清空当前节点
            this.count = 0;
            this.nodes = null;
        }

        /**
         * 返回该节点的nodes中rank值最低的node的索引
         */
        public int getMinRankNodeIndex() {
            int minRank = Integer.MAX_VALUE;
            int minIndex = count -1;
            for (int i = 0; nodes != null && i < count; i++) {
                if (nodes[i].getRank() < minRank) {
                    minRank = nodes[i].getRank();
                    minIndex = i;
                }
            }
            return minIndex;
        }
    }

    /**
     * 初始化
     *
     * @param config
     */
    public RoutingTable(Config config,byte[] nodeId,int port) {
        //参数
        this.config = config;
        this.nodeId = nodeId;
        maxStorePrefixLen = config.getPerformance().getRoutingTablePrefixLen();
        lockNum = config.getPerformance().getRoutingTableLockNum();

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
        put(new Node(this.nodeId, config.getMain().getIp(), port, Integer.MAX_VALUE));
    }



    /**
     * 新增若干节点
     */
    public void putAll(Collection<Node> nodes) {
        if (CollectionUtils.isEmpty(nodes))
            return;
        for (Node node : nodes) {
            put(node);
        }
    }

    /**
     * 新增节点
     */
    public boolean put(Node node) {


        //nodeIds -> 160位二进制
        byte[] bits = CodeUtil.getBitAll(node.getNodeIdBytes());

        TrieNode currentNode = root;
        TrieNode nextNode;
        try {
            for (int i = 0; i < MAX_PREFIX_LEN; i++) {


                //获取下一节点(根据nodeId字节数组的第i位)
                nextNode = currentNode.next[bits[i]];

                //如果下一节点不为空
                if (nextNode != null) {
                    currentNode = nextNode;
                    nextNode = null;
                    continue;
                }
                boolean isSplit = false;
                try {
                    lock(currentNode.lockId);
                    if(currentNode.nodes == null)
                        return false;

                    //如果已经包含该nodeId
                    int oldNodeIndex;
                    if ((oldNodeIndex = currentNode.containForIndex(node.getNodeIdBytes())) != -1) {
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
                    //如果当前节点包含自己的节点,或者不超过x层
                    if (currentNode.contain(this.nodeId) != null || i <= maxStorePrefixLen) {
                        //分裂节点
                        currentNode.split();
                        isSplit = true;
                    }else{
                        //此处表示.整个路由表已经满了.如果旧节点rank值小于一定值, 或 rank较大但超过一定时间未活动, 替换
                        int minRankNodeIndex = currentNode.getMinRankNodeIndex();
                        if (currentNode.nodes[minRankNodeIndex].getRank() <= NodeRankEnum.ANNOUNCE_PEER.getCode() ||
                                TimeUnit.MINUTES.convert(System.currentTimeMillis() - currentNode.nodes[minRankNodeIndex].getLastActiveTime().getTime(),
                                        TimeUnit.MILLISECONDS) > 60) {
                            currentNode.nodes[minRankNodeIndex] = node;
                            return true;
                        }
                    }
                } finally {
                    unlock(currentNode.lockId);
                }
                //此处不能直接新增新节点,因为当所有旧节点都被分配到同一子节点时,会导致仍需分裂节点,所以使用递归(也可循环)
                if (isSplit)
                    put(node);
                //否则抛弃该NodeId
                return false;
            }
        } catch (Exception e) {
            log.error("{}put失败.e:{}", LOG,e.getMessage(),e);
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
        try {
            lock(trieNode.lockId);
            Node[] nodes = trieNode.nodes;
            //循环保存了该节点的trieNode的nodes
            for (int i = 0; i < trieNode.count; i++) {
                //如果有相同的
                if (CodeUtil.equalsBytes(nodeId, nodes[i].getNodeIdBytes())) {
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
        } catch (Exception e) {
            log.error("{}delete失败.", LOG);
        } finally {
            unlock(trieNode.lockId);
        }
        return false;
    }

    /**
     * 搜索包含指定node的节点(trieNode)
     */
    public TrieNode get(byte[] nodeId) {
        if (nodeId.length != 20)
            return null;
        TrieNode currentNode = root;
        //nodeIds -> 160位二进制
        byte[] bits = CodeUtil.getBitAll(nodeId);

        try {
            for (int i = 0; i <= MAX_PREFIX_LEN; i++) {

                    //获取下一节点(根据nodeId字节数组的第i位)
                    TrieNode nextNode = currentNode.next[bits[i]];
                    //如果下一节点不为空
                    if (nextNode != null) {
                        currentNode = nextNode;
                        continue;
                    }
                    //为空则搜索该节点的nodes
                    if (currentNode.count == 0)
                        return null;
                try {
                    lock(currentNode.lockId);
                    Node node = currentNode.contain(nodeId);
                    return node == null ? null : currentNode;
                } finally {
                    unlock(currentNode.lockId);
                }
            }
        } catch (Exception e) {
            log.error("{}get失败.e:{}", LOG, e.getMessage(), e);

        }
        return null;
    }

    /**
     * 搜索和指定nodeId最近的8个node或 指定nodeId自己
     */
    public List<Node> getForTop8(byte[] nodeId) {
        List<Node> nodes = new LinkedList<>();
        if(nodeId == null || nodeId.length != 20)
            return nodes;
        //nodeIds -> 160位二进制
        byte[] bits = CodeUtil.getBitAll(nodeId);
        TrieNode currentNode = root;
        TrieNode lastNode = null;//上一节点
        try {
            for (int i = 0; i <= MAX_PREFIX_LEN; i++) {
                //获取下一节点(根据nodeId字节数组的第i位)
                TrieNode nextNode = currentNode.next[bits[i]];
                //如果下一节点不为空
                if (nextNode != null) {
                    lastNode = currentNode;
                    currentNode = nextNode;
                    continue;
                }
                try {
                    lock(currentNode.lockId);
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
                } finally {
                    unlock(currentNode.lockId);
                }
                if (nodes.size() == MAX_NODE_NUM)
                    return nodes;
                //如果list的长度没到8（说明nodes为空，或者nodes的长度不足8）,就去拥有相同父节点的隔壁节点找
                //(因为是由8个节点分裂而来，所有在未主动删除的情况下，是可以集齐8个的)
                byte lastIndex = bits[i - 1];//上个节点，进入currentNode时的bit
                //此处lastNode不会为空，因为当i==0，currentNode为root时，必然会进入一次该lastNode赋值的循环体.
                TrieNode findNode = lastNode.next[lastIndex == 0 ? 1 : 0];//隔壁节点
                try {
                    lock(findNode.lockId);
                    //要返回的nodes缺少的节点个数
                    int lackNum = MAX_NODE_NUM - nodes.size();
                    if (findNode.count != 0)
                        //从隔壁节点截取若干节点添加到返回节点. 此处限制其数量,防止IndexOutOfBoundsException
                        nodes.addAll(Arrays.asList(findNode.nodes).subList(0, lackNum <= findNode.count ? lackNum : findNode.count));
                    return nodes;
                } finally {
                    unlock(findNode.lockId);
                }
            }
        } catch (Exception e) {
            log.error("{}getForTop8失败.", LOG);
        }
        return nodes;
    }

    /**
     * 遍历节点,并使用指定函数 操作nodes
     */
    private void loop(TrieNode node, Consumer<TrieNode> consumer) {
        if (node.next[0] != null) loop(node.next[0], consumer);
        if (node.next[1] != null) loop(node.next[1], consumer);
        if (node.count > 0) {
            try {
                lock(node.lockId);
                consumer.accept(node);
            } finally {
                unlock(node.lockId);
            }
        }

    }

    /**
     * 遍历节点,并使用指定函数 操作nodes
     * 封装,去除一个参数
     */
    public void loop(Consumer<TrieNode> consumer) {
        try {
            loop(this.root, consumer);
        } catch (Exception e) {
            log.error("{}loop失败.", LOG);
        }
    }

    /**
     * 长度
     */
    public long size() {
        return this.count.longValue();
    }

    /**
     * 加锁
     */
    public void lock(int prefixLen) {
        this.locks[prefixLen % this.lockNum].lock();
    }

    /**
     * 解锁
     */
    public void unlock(int prefixLen) {
        if (prefixLen != -1)
            this.locks[prefixLen % this.lockNum].unlock();

    }


}
