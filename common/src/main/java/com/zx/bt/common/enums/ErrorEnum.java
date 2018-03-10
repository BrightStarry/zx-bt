package com.zx.bt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2017/11/7 0007 16:42
 * 异常状态枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorEnum implements CodeEnum<String> {
    SUCCESS("0000","成功"),
    COMMON_ERROR("0001","通用异常"),
    ;
    private String code;
    private String message;

}
