package com.zx.bt.web.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018/3/13 0013 10:00
 * 客户端发送的弹幕类型消息请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BarrageRequestDTO {
	/**
	 * 弹幕消息
	 */
	private String barrageMessage;
}
