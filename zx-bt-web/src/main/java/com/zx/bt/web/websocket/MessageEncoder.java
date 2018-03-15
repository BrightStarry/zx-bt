package com.zx.bt.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.web.websocket.dto.WebSocketResponseDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 17:47
 * 发送消息时的消息编码器
 */
@Slf4j
@Component
public class MessageEncoder implements Encoder.Text<WebSocketResponseDTO>{

	private static ObjectMapper objectMapper;

	@Autowired
	private void init(ObjectMapper objectMapper) {
		MessageEncoder.objectMapper = objectMapper;
	}

	@Override
	@SneakyThrows
	public String encode(WebSocketResponseDTO object) throws EncodeException {
		return objectMapper.writeValueAsString(object);
	}

	@Override
	public void init(EndpointConfig endpointConfig) {

	}

	@Override
	public void destroy() {

	}


}
