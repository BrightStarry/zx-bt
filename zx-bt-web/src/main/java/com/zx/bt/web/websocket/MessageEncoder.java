package com.zx.bt.web.websocket;

import com.zx.bt.web.websocket.dto.WebSocketResponseDTO;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 17:47
 * 发送消息时的消息编码器
 */
public class MessageEncoder implements   Encoder.Text<WebSocketResponseDTO>{

	@Override
	public String encode(WebSocketResponseDTO object) throws EncodeException {
		return null;
	}

	@Override
	public void init(EndpointConfig endpointConfig) {

	}

	@Override
	public void destroy() {

	}
}
