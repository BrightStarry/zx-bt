package com.zx.bt.dto.method;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author:ZhengXing
 * datetime:2018-02-14 14:48
 * 通用请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommonRequest extends CommonParam{
    /**方法(ping/find_node等)*/
    protected String q;
}
