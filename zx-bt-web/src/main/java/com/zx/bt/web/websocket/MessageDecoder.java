package com.zx.bt.web.websocket;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.web.websocket.dto.WebSocketRequestDTO;
import com.zx.bt.web.websocket.enums.WebSocketMessageTypeEnum;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.util.Arrays;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 17:48
 * 获取消息时的消息解码器
 */
@Slf4j
@Component
public class MessageDecoder implements Decoder.Text<WebSocketRequestDTO>{
	private static final String LOG = "[MessageDecoder]";
	private static ObjectMapper objectMapper;
	//每个类型的消息的javaType,用于 jsonStr -> 消息对象
	private static JavaType[] messageTypes;

	@Autowired
	private void init(ObjectMapper objectMapper) {
		MessageDecoder.objectMapper = objectMapper;
		//将每个消息类型对应的请求消息的类转为javaType数组
		messageTypes = Arrays.stream(WebSocketMessageTypeEnum.values())
				.map(item -> objectMapper.getTypeFactory()
						.constructParametricType(WebSocketRequestDTO.class,
								item.getRequestJavaType()))
				.toArray(JavaType[]::new);
	}

	/**
	 * 解码
	 */
	@Override
	public WebSocketRequestDTO decode(String s) throws DecodeException {
		try {
			JsonNode jsonNode = objectMapper.readTree(s);
			int type = jsonNode.get("type").asInt();
			WebSocketRequestDTO<?> webSocketRequestDTO = null;
			switch (EnumUtil.getByCode(type, WebSocketMessageTypeEnum.class).orElseThrow(()->new BTException("消息类型不存在,当前类型:" + type))) {
				// 握手
				case HANDSHAKE:
					webSocketRequestDTO = objectMapper.readValue(s, messageTypes[type]);

					break;
				// 弹幕
				case BARRAGE:
					webSocketRequestDTO = objectMapper.readValue(s, messageTypes[type]);
					break;
			}
			return webSocketRequestDTO;
		} catch (Exception e) {
			log.error("{}当前消息:{},解码异常:{}",s,e.getMessage(),e);
		}
		return null;
	}


	/**
	 * 解码前方法
	 * 验证该解码其是否支持该消息解码, 如果支持需要返回true
	 */
	@Override
	public boolean willDecode(String s) {
		return true;
	}

	/**
	 * 初始化
	 */
	@Override
	public void init(EndpointConfig endpointConfig) {
	}

	/**
	 * 销毁
	 */
	@Override
	public void destroy() {

	}
}
