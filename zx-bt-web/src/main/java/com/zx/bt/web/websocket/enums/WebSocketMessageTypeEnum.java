package com.zx.bt.web.websocket.enums;

import com.zx.bt.common.enums.CodeEnum;
import com.zx.bt.web.websocket.dto.BarrageRequestDTO;
import com.zx.bt.web.websocket.dto.HandshakeResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 09:42
 * 消息类型
 */
@Getter
@AllArgsConstructor
public enum WebSocketMessageTypeEnum implements CodeEnum<Integer>{

	HANDSHAKE(0,"握手,客户端需要在连接成功后发送握手请求,服务端再返回token,表示成功建立连接",String.class),
	BARRAGE(1,"弹幕",BarrageRequestDTO.class),
	;
	private Integer code;
	private String message;
	private Class<?> requestJavaType;
}
