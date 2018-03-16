package com.zx.bt.web.websocket.enums;

import com.zx.bt.common.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 09:53
 * 消息状态码
 */
@Getter
@AllArgsConstructor
public enum WebSocketMessageCodeEnum implements CodeEnum<String>{
	SUCCESS("0000","成功"),
	UNKNOWN_ERROR("0001","未知异常"),
	TOKEN_OR_TIMESTAMP_ERROR("0002","未知异常(timestamp校验失败)"),
	;
	private String code;
	private String message;
}
