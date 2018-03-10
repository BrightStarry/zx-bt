package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.dto.MessageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:05
 * 处理对象
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcessObject {

	/**
	 * 消息信息
	 */
	private MessageInfo messageInfo;

	/**
	 * 原始map
	 */
	private Map<String,Object> rawMap;

	/**
	 * 消息发送者
	 */
	private InetSocketAddress sender;

	/**
	 * index
	 */
	private int index;
}
