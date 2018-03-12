package com.zx.bt.web.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 17:51
 * 服务器端响应给客户端消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class WebSocketResponse<T> {
}
