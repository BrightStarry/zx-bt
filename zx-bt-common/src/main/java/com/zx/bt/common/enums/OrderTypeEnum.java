package com.zx.bt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2017/11/7 0007 16:42
 * metadata 排序类型
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum implements CodeEnum<Integer> {
    NONE(0, "不进行任何排序", ""),
    EXPLAIN(1, "相关性", ""),
    HOT(2, "热度","hot"),
    LENGTH(3, "长度","length"),
    CREATE_TIME(4, "创建时间","createTime"),
    ;
    private Integer code;
    private String message;
    private String fieldName;

}
