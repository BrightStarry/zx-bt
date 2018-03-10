package com.zx.bt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-02-14 10:31
 * 缓存方式枚举
 */
@Getter
@AllArgsConstructor
public enum CacheMethodEnum {
    AFTER_ACCESS,
    AFTER_WRITE,
    NONE,
    ;


}
