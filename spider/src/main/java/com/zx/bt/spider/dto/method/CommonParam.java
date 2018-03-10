package com.zx.bt.spider.dto.method;

import lombok.Data;

/**
 * author:ZhengXing
 * datetime:2018-02-14 14:50
 * 通用参数
 */
@Data
public class CommonParam {
    /**消息ID(必须为String)*/
    protected String t;
    /**状态(请求/回复/异常)*/
    protected String y;
}
