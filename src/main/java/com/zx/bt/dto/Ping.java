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
 * datetime:2018-02-14 10:48
 * ping 请求&回复
 *
 * ping请求={"t":"aa", "y":"q","q":"ping", "a":{"id":"abcdefghij0123456789"}}
 * B编码=d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe
 * 回复={"t":"aa", "y":"r", "r":{"id":"mnopqrstuvwxyz123456"}}
 * B编码=d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
 *
 */
public interface Ping {


    /**
     * 主体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Content {
        private String id;
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

        /**主体,包含请求发送方的nodeID(也就是自己的)*/
        private Content a;

        private void init() {
            t = BTUtil.generateMessageID();
            y = YEnum.QUERY.getCode();
            q = MethodEnum.PING.getCode();
            a = new Content();
        }

        /**
         * 指定请求发送方nodeID,构造
         */
        public Request(String id) {
            init();
            a.id = id;
        }
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
        /**主体,包含回复者的nodeID*/
        private Content r;

        private void init() {
            y = YEnum.RECEIVE.getCode();
            r = new Content();
        }

        /**
         * 根据回复方nodeID/ 消息id构造
         */
        public Response(String id,String messageId) {
            init();
            t = messageId;
            r.id = id;
        }
    }



}
