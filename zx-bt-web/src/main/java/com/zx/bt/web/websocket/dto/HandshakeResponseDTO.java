package com.zx.bt.web.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 14:28
 * 握手响应(服务端->客户端),只在连接建立成功后响应一次
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandshakeResponseDTO {
	/**
	 * websocket连接的sessionId
	 */
	private String sessionId;
}
