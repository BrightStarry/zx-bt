package com.zx.bt.web.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 10:00
 * 服务端发送的弹幕类型消息响应
 * 该消息自动广播给所有连接者
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BarrageResponseDTO {
	/**
	 * 弹幕消息
	 */
	private String barrageMessage;
}
