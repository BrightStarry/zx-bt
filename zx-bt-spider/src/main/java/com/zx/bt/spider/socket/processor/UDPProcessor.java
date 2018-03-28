package com.zx.bt.spider.socket.processor;

import com.zx.bt.spider.config.Config;
import com.zx.bt.common.exception.BTException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/3/1 0001 10:05
 * udp处理器接口
 */
@Slf4j
@Component
public abstract class UDPProcessor {

	protected static   Config config;
	protected static List<String> nodeIds;

	@Autowired
	public void init(Config config) {
		UDPProcessor.config = config;
		UDPProcessor.nodeIds = config.getMain().getNodeIds();
	}

	/**
	 * 下一个处理器
	 */
	private UDPProcessor next;

	/**
	 * 处理请求模版方法
	 * 不可重写
	 */
	final boolean process(ProcessObject processObject) {
		//此处next为空时直接返回false
		if(!isProcess(processObject))
			return next != null && next.process(processObject);
		try {
			return process1(processObject);
		}catch (BTException e) {
			log.error("[处理异常]e:{}",e.getMessage());
		} catch (Exception e) {
			log.error("[处理异常]e:{}",e.getMessage(),e);
		}
		return false;
	}

	/**
	 * 处理请求 真正的处理方法
	 */
	abstract boolean process1(ProcessObject processObject);

	/**
	 * 是否使用该处理器
	 */
	abstract boolean isProcess(ProcessObject processObject);

	/**
	 * 设置下一个处理器
	 */
	void setNext(UDPProcessor udpProcessor){
		this.next = udpProcessor;
	}
}
