package com.zx.bt.dto;

import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.util.BTUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018-02-14 17:07
 * find_node请求
 */
public interface FindNode {

    /**
     * 主体
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
         * 要查找的nodeID
         */
        private String target;
    }


    /**
     * 请求
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Request extends CommonRequest{

        /**主体,包含请求发送方的nodeID*/
        private RequestContent a;

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = MethodEnum.FIND_NODE.getCode();
            a = new RequestContent();
        }

        /**
         * 指定请求发送方nodeID/ 要查找的nodeId构造
         */
        public Request(String nodeId,String targetNodeId) {
            init();
            a.id = nodeId;
            a.target = targetNodeId;
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
         * 与要查找的nodeId最接近的8个node的nodeIds
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
    public static class Response extends CommonResponse{

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private ResponseContent r;

        private void init() {
            y = YEnum.QUERY.getCode();
            r = new ResponseContent();
        }

        /**
         * 指定请回复方nodeID/ nodes
         */
        public Response(String nodeId,String nodes,String messageId) {
            init();
            r.id = nodeId;
            r.nodes = nodes;
            t = messageId;
        }
    }
}
