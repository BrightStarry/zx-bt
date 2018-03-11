package com.zx.bt.spider.dto.method;

import com.zx.bt.spider.enums.MethodEnum;
import com.zx.bt.spider.enums.YEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018-02-15 15:21
 * get_peers方法
 */
public interface GetPeers {

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
         * 种子文件的infohash
         */
        private String info_hash;
    }


    /**
     * 请求
     * 该类不自动生成消息id
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Request extends CommonRequest {

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private GetPeers.RequestContent a;

        private void init() {
//            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = MethodEnum.GET_PEERS.getCode();
            a = new GetPeers.RequestContent();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String info_hash,String messageId) {
            init();
            t = messageId;
            a.id = nodeId;
            a.info_hash = info_hash;
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

        /**
         * 回复方定义的token
         */
        private String token;

        /**
         * 当有该种子时,回复的是values,没有时,回复的是nodes.
         */
        private String nodes;
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
        private GetPeers.ResponseContent r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new GetPeers.ResponseContent();
        }

        /**
         * 指定回复方id/ token/ nodes
         */
        public Response(String nodeId,String token,String nodes,String messageId) {
            init();
            r.id = nodeId;
            r.token = token;
            r.nodes = nodes;
            t = messageId;
        }
    }
}
