package com.zx.bt.spider.enums;

import com.zx.bt.common.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-14 10:31
 * KRPC协议中的Y属性
 */
@Getter
@AllArgsConstructor
public enum YEnum implements CodeEnum<String> {
    QUERY("q", "请求"),
    RECEIVE("r", "回复"),
    ERROR("e", "异常"),
    ;

    private String code;
    private String message;

}
