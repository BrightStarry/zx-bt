package com.zx.bt.web.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 15:28
 * 表示了单个WebSocket连接
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Connection {
	/**
	 * http的session
	 */
	private HttpSession httpSession;

	/**
	 * websocket的session
	 */
	private Session webSocketSession;


}
