package com.zx.bt.config;

import com.zx.bt.task.ScheduleTask;
import com.zx.bt.util.BTUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
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
public class Config {
    /**
     * 主要配置
     */
    private Main main = new Main();

    /**
     * 性能相关
     */
    private Performance performance = new Performance();

    //常量配置--------

    //每个节点信息默认占用的字节长度. 为20位nodeId,4位ip,2位port
    public static final Integer NODE_BYTES_LEN = 26;

    //每个peer信息占用的字节长度,4位ip + 2位port
    public static final Integer PEER_BYTES_LEN = 6;


    //nodeId和infohash的长度
    public static final Integer BASIC_HASH_LEN = 20;

    //获取种子元信息时,第一条握手信息的前缀, 28位byte. 第2-20位,是ASCII码的BitTorrent protocol,
    // 第一位19,是固定的,表示这个字符串的长度.后面八位是BT协议的版本.可以全为0,不必理会.
    public static final byte[] GET_METADATA_HANDSHAKE_PRE_BYTES = {19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
            111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1};




    @Data
    public static class Main{

        /**
         * 机器的ip
         */
        private String ip;

        /**
         * nodeId
         */
        private String nodeId = BTUtil.generateNodeIdString();


        /**UDP服务器端端口号*/
        private Integer port = 6881;

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
         * see {@link ScheduleTask#updateTargetNodeId()}
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
    }

    /**
     * 性能相关
     */
    @Data
    public static class Performance{
        /**UDP服务器主任务线程数*/
        private Integer udpServerMainThreadNum = 5;

        /**TCP处理任务线程数*/
        private Integer tcpClientThreadNum = 2;

        /**TCP连接超时时间(ms)*/
        private Integer tcpConnectTimeoutMillis = 5000;

        /**
         * 路由表分段锁 数量
         */
        private Integer routingTableLockNum = 30;

        /**
         * 路由表 非自己的节点id 的一侧分支, 最大可存储的层数. <=160
         */
        private Integer routingTablePrefixLen = 13;

        /**
         * get_peers任务过期时间
         */
        private Integer getPeersTaskExpireSecond = 600;

        /**
         * get_peers任务, info_hash等待队列长度
         */
        private Integer getPeersTaskInfoHashQueueLen = 1024000;

        /**get_peers任务,最多同时进行的任务数*/
        private Integer getPeersTaskConcurrentNum = 3000;

        /**
         * 普通节点超时时间
         */
        private Integer generalNodeTimeoutMinute = 10;

        /**
         * rank值较高的节点超时时间
         */
        private Integer specialNodeTimeoutMinute = 120;

        /**
         * 发送缓存默认长度
         * 消息id(t)为2个字节,最大表示也就是2的16次方
         */
        private Integer defaultCacheLen = 1<<16;

        /**FindNodeTask群发路由表线程,间隔时间(s)*/
        private Integer findNodeTaskIntervalSecond = 10;

    }




}
