package com.zx.bt.web.websocket.dto;

import com.zx.bt.common.util.CodeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 09:41
 * WebSocket请求消息对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class WebSocketMessageRequestDTO<T> {

	private static final int ONE_MINUTE_MILLS = 1000 * 60;

	/**
	 * 消息类型
	 * see {@link com.zx.bt.web.websocket.enums.WebSocketMessageTypeEnum}
	 */
	private Integer type;

	/**
	 * 时间戳
	 */
	private Long timestamp;

	/**
	 * token 用户的websocket的session.id + 消息类型 + 时间戳 作md5 16进制 32位 小写
	 */
	private String token;

	/**
	 * 消息数据
	 */
	private T data;


	/**
	 * 校验token是否正确
	 */
	public boolean verifyToken(String webSocketSessionId) {
		if (StringUtils.isBlank(token) || token.length() != CodeUtil.MD5_LENGTH_32 || type == null)
			return false;

		return token.equals(CodeUtil.stringToMd5(webSocketSessionId + type + timestamp));
	}

	/**
	 * 校验时间戳是否正确,在当前时间正负1分钟内
	 */
	public boolean verifyTimestamp() {
		long now = System.currentTimeMillis();
		return timestamp.compareTo(now + ONE_MINUTE_MILLS) <= 0 && timestamp.compareTo(now - ONE_MINUTE_MILLS) >= 0;
	}
}