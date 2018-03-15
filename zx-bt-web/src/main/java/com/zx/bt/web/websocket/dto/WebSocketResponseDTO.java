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
	 * hash 防篡改
	 * 状态码 + websocket的session.id + 时间戳 作MD5 16进制32位小写
	 */
	private String hash;

	/**
	 * 时间戳
	 */
	private Long timestamp;

	/**
	 * 消息数据
	 */
	private T  data;

	/**
	 * 生成hash码
	 */
	public void generateHash(String webSocketSessionId) {
		this.hash = CodeUtil.stringToMd5(code + webSocketSessionId + timestamp);
	}

	public WebSocketResponseDTO(Integer type, Long timestamp, T data) {
		this.type = type;
		this.timestamp = timestamp;
		this.data = data;
	}

	/**
	 * 传入消息类型和timestamp和data,以及sessionIds
	 * 批量生成该对象
	 */
	public  static <T> List<WebSocketResponseDTO<T>> batchCreate(Integer type, T data, Long timestamp, String... sessionIds) {
		if(ArrayUtils.isEmpty(sessionIds))
			return Collections.emptyList();
		LinkedList<WebSocketResponseDTO<T>> result = new LinkedList<>();
		for (int i = 0; i < sessionIds.length; i++) {
			result.add(new WebSocketResponseDTO<>(sessionIds[i], type, timestamp, data));
		}
		return result;
	}


	public WebSocketResponseDTO(String webSocketSessionId, Integer type, Long timestamp, T data) {
		this.type = type;
		this.timestamp = timestamp;
		this.data = data;
		generateHash(webSocketSessionId);
	}

	public WebSocketResponseDTO(String webSocketSessionId, Integer type, T data) {
		this.type = type;
		this.timestamp = System.currentTimeMillis();
		this.data = data;
		generateHash(webSocketSessionId);
	}

	public WebSocketResponseDTO(String webSocketSessionId, Integer type, WebSocketMessageCodeEnum webSocketMessageCodeEnum) {
		this.type = type;
		this.code = webSocketMessageCodeEnum.getCode();
		this.message = webSocketMessageCodeEnum.getMessage();
		generateHash(webSocketSessionId);
	}
}
