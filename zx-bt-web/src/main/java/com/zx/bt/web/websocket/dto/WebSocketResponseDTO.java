package com.zx.bt.web.websocket.dto;

import com.zx.bt.common.util.CodeUtil;
import com.zx.bt.web.websocket.enums.WebSocketMessageCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.util.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 09:41
 * WebSocket响应消息对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class WebSocketResponseDTO<T> {

	/**
	 * 消息类型
	 * see {@link com.zx.bt.web.websocket.enums.WebSocketMessageTypeEnum}
	 */
	private Integer type;

	/**
	 * 消息状态码
	 */
	private String code = WebSocketMessageCodeEnum.SUCCESS.getCode();

	/**
	 * 状态消息
	 */
	private String message = WebSocketMessageCodeEnum.SUCCESS.getMessage();

	/**
	 * 时间戳
	 */
	private Long timestamp;

	/**
	 * 消息数据
	 */
	private T  data;


	public WebSocketResponseDTO(Integer type, Long timestamp, T data) {
		this.type = type;
		this.timestamp = timestamp;
		this.data = data;
	}


	public WebSocketResponseDTO(String webSocketSessionId, Integer type, Long timestamp, T data) {
		this.type = type;
		this.timestamp = timestamp;
		this.data = data;
	}

	public WebSocketResponseDTO(String webSocketSessionId, Integer type, T data) {
		this.type = type;
		this.timestamp = System.currentTimeMillis();
		this.data = data;
	}

	public WebSocketResponseDTO(String webSocketSessionId, Integer type, WebSocketMessageCodeEnum webSocketMessageCodeEnum) {
		this.type = type;
		this.code = webSocketMessageCodeEnum.getCode();
		this.message = webSocketMessageCodeEnum.getMessage();
	}
}
