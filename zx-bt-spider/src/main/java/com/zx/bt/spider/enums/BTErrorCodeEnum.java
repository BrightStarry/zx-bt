package com.zx.bt.spider.enums;

import com.zx.bt.common.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-14 10:31
 * KRPC协议异常枚举
 */
@Getter
@AllArgsConstructor
public enum BTErrorCodeEnum implements CodeEnum<Integer> {
    COMMON_ERROR(201, "一般错误"),
    SERVER_ERROR(202, "服务错误"),
    PROTOCOL_ERROR(203, "协议错误"),
    UNKNOWN_METHOD(204, "未知方法"),
    ;

    private Integer code;
    private String message;

}
