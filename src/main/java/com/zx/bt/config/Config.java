package com.zx.bt.config;

import com.zx.bt.task.FindNodeTask;
import com.zx.bt.util.BTUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:39
 * 自定义配置类
 */
@Component
@Accessors(chain = true)
@ConfigurationProperties(prefix = "zx-bt")
@Data
@Slf4j
@Validated
public class Config {
    /**
     * 主要配置
     */
    @Valid
    private Main main = new Main();

    /**
     * 性能相关
     */
    @Valid
    private Performance performance = new Performance();

    //常量配置--------

    //磁力前缀
    private static final String magnetLinkPre = "magnet:?xt=urn:btih:";

    //每个节点信息默认占用的字节长度. 为20位nodeId,4位ip,2位port
    public static final Integer NODE_BYTES_LEN = 26;

    //每个peer信息占用的字节长度,4位ip + 2位port
    public static final Integer PEER_BYTES_LEN = 6;


    //nodeId和infohash的长度
    public static final Integer BASIC_HASH_LEN = 20;

    //获取种子元信息时,第一条握手信息的前缀, 28位byte. 第2-20位,是ASCII码的BitTorrent protocol,
    // 第一位19,是固定的,表示这个字符串的长度.后面八位是BT协议的版本.可以全为0,某些软件对协议进行了扩展,协议号不全为0,不必理会.
    public static final byte[] GET_METADATA_HANDSHAKE_PRE_BYTES = {19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
            111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1};

    //metadata数据, 每一分片大小 16KB, 此处为byte
    public static final long METADATA_PIECE_SIZE = 16 << 10;







    @Data
    public static class Main{

        /**
         * 机器的ip
         */
        private String ip;

		/**
		 * nodeIds
		 */
		private List<String> nodeIds = new ArrayList<>();



        /**
		 * UDP服务器端端口号
		 */
		private List<Integer> ports = new ArrayList<>();

        /**初始地址*/
        private List<String> initAddresses = new LinkedList<>();
        
        /**
         * token(自己响应其他节点的get_peers请求时,需回复别人该token,等该节获取到该种子后,会将种子info_hash和该token一起发回来(announce_peer请求))
         */
        private String token = "zx";

        /**
         * 返回假nodeId时,和对方nodeId后x个字节不同
         * <= 20
         */
        private Integer similarNodeIdNum = 1;

        /**
         * 要查询的目标节点
         * see {@link FindNodeTask#updateTargetNodeId()}
         */
        private volatile String targetNodeId = BTUtil.generateNodeIdString();

        /**
         * 获取初始化地址
         */
        public InetSocketAddress[] getInitAddressArray() {
            return this.initAddresses.stream().map(item -> {
                String[] split = item.split(":");
                return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
            }).toArray(InetSocketAddress[]::new);
        }

        /**
         * 初始化nodeId
         */
        public void initNodeIds() {
            for (int i = 0; i < this.ports.size(); i++) {
                this.nodeIds.add(BTUtil.generateNodeIdString());
            }
        }
    }

    /**
     * 性能相关
     */
    @Data
    public static class Performance{
        /**UDP服务器主任务线程数,单个端口的数量*/
        private Integer udpServerMainThreadNum = 5;

        /**
         * find_node任务, 发送间隔. 毫秒
         * see {@link FindNodeTask#start()}
         */
        private Integer findNodeTaskIntervalMS = 10;

        /**
         * get_peers任务, 开启新任务最大间隔
         */
        private Integer getPeersTaskCreateIntervalMs = 1000;

        /**
         * get_peers任务, 任务满载后,暂停开启新任务时间
         */
        private Integer getPeersTaskPauseSecond = 10;

        /**
         * get_peers请求, 最小发送间隔
         */
        private Integer getPeersRequestSendIntervalMs = 10;

        /**
         * find_node任务,线程数
         */
        private Integer findNodeTaskThreadNum = 20;

        /**连接peer任务TCP线程数*/
        private Integer tcpClientThreadNum = 4;

        /**连接peer任务TCP连接超时时间(ms)*/
        private Integer tcpConnectTimeoutMs = 5000;

        /**
         * 路由表分段锁 数量
         */
        private Integer routingTableLockNum = 10;

        /**
         * 路由表 非自己的节点id 的一侧分支, 最大可存储的层数. <=160
         */
        private Integer routingTablePrefixLen = 10;

        /**
         * get_peers任务过期时间
         */
        private Integer getPeersTaskExpireSecond = 300;

        /**
         * get_peers任务, info_hash等待队列长度
         */
        private Integer getPeersTaskInfoHashQueueLen = 102400;

        /**get_peers任务,最多同时进行的任务数*/
        private Integer getPeersTaskConcurrentNum = 100;

        /**
         * fetchMetadataByPeerTask,最大线程数
         */
        private Integer fetchMetadataByPeerTaskTreadNum = 10;

        /**
         * fetchMetadataByOtherWebTask,等待尝试获取队列最大长度
         */
        private Integer fetchMetadataByOtherWebTaskQueueNum = 10240000;

        /**
         *  fetchMetadataByOtherWebTask,线程数
         *  该任务多为http连接, 可设置多点的线程,
         *  但也要考虑过多会不会被网站封掉(在未使用代理的情况下.)
         */
        private Integer fetchMetadataByOtherWebTaskThreadNum = 10;

        /**
         * 普通节点超时时间
         */
        @Deprecated
        private Integer generalNodeTimeoutMinute = 10;

        /**
         * rank值较高的节点超时时间
         */
        @Deprecated
        private Integer specialNodeTimeoutMinute = 120;

        /**
         * 发送缓存默认长度
         * 消息id(t)为2个字节,最大表示也就是2的16次方
         */
        private Integer defaultCacheLen = 1<<16;




    }




}
