package com.zx.bt.config;

import com.zx.bt.util.BTUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:39
 * 自定义配置类
 */
@Component
@ConfigurationProperties(prefix = "zx-bt")
@Data
public class Config {
    /**
     * 主要配置
     */
    private Main main = new Main();

    //常量配置--------

    //每个节点信息默认占用的node字节长度. 为20位nodeId,4位ip,2位port
    public static final Integer NODE_BYTES_LEN = 26;

    //获取种子元信息时,第一条握手信息的前缀, 28位byte. 第2-20位,是ASCII码的BitTorrent protocol,
    // 第一位19,是固定的,表示这个字符串的长度.后面八位是BT协议的版本.可以全为0,不必理会.
    public static final Byte[] GET_METADATA_HANDSHAKE_PRE_BYTES = {19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
            111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1};



    @Data
    public static class Main{
        /**
         * nodeId
         */
        private String nodeId = BTUtil.generateNodeIdString();

        /**UDP服务器端端口号*/
        private Integer port = 44444;

        /**UDP服务器主任务线程数*/
        private Integer udpServerMainThreadNum = 2;

        /**初始地址*/
        private List<String> initAddresses = new LinkedList<>();

        /**
         * 初始要查找的目标nodeId
         */
        private String targetNodeId = BTUtil.generateNodeIdString();

        /**
         * token(自己响应其他节点的get_peers请求时,需回复别人该token,等该节获取到该种子后,会将种子info_hash和该token一起发回来(announce_peer请求))
         */
        private String token = "zx";


    }
}
