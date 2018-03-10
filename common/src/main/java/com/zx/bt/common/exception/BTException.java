package com.zx.bt.common.exception;

import com.zx.bt.common.enums.CodeEnum;
import com.zx.bt.common.enums.ErrorEnum;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2017/11/7 0007 16:40
 * 自定义异常
 */
@Getter
public class BTException extends RuntimeException {
    private String code = ErrorEnum.COMMON_ERROR.getCode();

    /**
     * 根据异常枚举构造自定义异常
     * @param codeEnum
     */
    public BTException(CodeEnum<String> codeEnum){
        super(codeEnum.getMessage());
        this.code = codeEnum.getCode();
    }

    /**
     * 根据异常码和消息构造自定义异常
     * @param code
     * @param message
     */
    public BTException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 根据消息构造自定义异常
     * @param message
     */
    public BTException(String message) {
        super(message);
    }
}
