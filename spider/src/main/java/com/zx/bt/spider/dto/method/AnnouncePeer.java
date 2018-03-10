package com.zx.bt.spider.dto.method;

import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.YEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.spider.util.BTUtil;
import com.zx.bt.common.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-15 14:53
 * 宣告peer方法
 */
public interface AnnouncePeer {
    /**
     * 请求主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class RequestContent {
        /**
         * 请求方nodeID
         */
        private String id;

        /**
         * 可选, 为1时,忽略port参数,直接将请求发送节点的发送port作为port
         */
        private Integer implied_port;

        /**
         * 种子文件的infohash
         */
        private String info_hash;

        /**
         * 正在下载种子的端口
         */
        private Integer port;

        /**
         * 之前get_peers请求中的token
         */
        private String token;

        public  RequestContent (Map<String, Object> map,int defaultPort) {
            Map<String, Object> aMap = BTUtil.getParamMap(map, "a", "ANNOUNCE_PEER,找不到a参数.map:" + map);
            info_hash = CodeUtil.bytes2HexStr(BTUtil.getParamString(aMap, "info_hash", "ANNOUNCE_PEER,找不到info_hash参数.map:" + map)
                    .getBytes(CharsetUtil.ISO_8859_1));
            if (aMap.get("implied_port") == null || ((long) aMap.get("implied_port") )== 0) {
                Object portObj = aMap.get("port");
                if(portObj == null)
                    throw new BTException("ANNOUNCE_PEER,找不到port参数.map:" + map);
                port = ((Long) portObj).intValue();
            }else
                port = defaultPort;
            id = CodeUtil.bytes2HexStr(BTUtil.getParamString(aMap, "id", "ANNOUNCE_PEER,找不到id参数.map:" + map).getBytes(CharsetUtil.ISO_8859_1));

        }
    }


    /**
     * 请求
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Request extends CommonRequest {

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private AnnouncePeer.RequestContent a;

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = MethodEnum.ANNOUNCE_PEER.getCode();
            a = new AnnouncePeer.RequestContent();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String info_hash,int port,String token) {
            init();
            a.id = nodeId;
            a.info_hash = info_hash;
            a.port = port;
            a.token = token;
        }
    }

    /**
     * 响应主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class ResponseContent {
        /**
         * 回复方nodeID
         */
        private String id;
    }

    /**
     * 响应
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Response extends CommonResponse {

        /**主体,*/
        private AnnouncePeer.ResponseContent r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new AnnouncePeer.ResponseContent();
        }

        /**
         * 指定回复方id
         */
        public Response(String nodeId,String messageId) {
            init();
            r.id = nodeId;
            t = messageId;
        }
    }



}
