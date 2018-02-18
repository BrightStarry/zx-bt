package com.zx.bt.dto;

import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018-02-14 15:09
 * 一个消息的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MessageInfo {

    /**
     * 方法
     * See {@link MethodEnum}
     */
    private MethodEnum method;

    /**
     * 状态
     * See {@link YEnum}
     */
    private YEnum status;

    /**
     * 消息id
     */
    private String messageId;


}
