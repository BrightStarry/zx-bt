package com.zx.bt.common.enums;

/**
 * author:ZhengXing
 * datetime:2017/10/16 0016 17:35
 * 通用的带code/message的枚举接口
 */
public interface CodeEnum<T> {
    T getCode();
    String getMessage();
}
