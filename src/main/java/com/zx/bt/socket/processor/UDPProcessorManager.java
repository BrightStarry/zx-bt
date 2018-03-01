package com.zx.bt.socket.processor;

import lombok.extern.slf4j.Slf4j;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:08
 * 处理器管理器
 */
@Slf4j
public class UDPProcessorManager {

	/**
	 * 第一个处理器
	 */
	private UDPProcessor first;

	/**
	 * 最后一个处理器
	 */
	private UDPProcessor last;

	/**
	 * 注册
	 */
	public void register(UDPProcessor processor) {
		if (first == null) {
			first = last = processor;
			return;
		}
		last.setNext(processor);
		last = processor;
	}

	/**
	 * 处理请求
	 */
	public boolean process(ProcessObject processObject) {
		return first.process(processObject);
	}
}
