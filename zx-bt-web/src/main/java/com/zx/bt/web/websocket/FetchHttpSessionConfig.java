package com.zx.bt.web.websocket;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 15:20
 * 在websocket中获取HttpSession的配置类
 *
 * ${@link ServerEndpointConfig.Configurator}用于在握手中获取httpSession
 * ${@link ServletRequestListener}用于强制请求携带HttpSession,如果不实现,在握手中获取HttpSession会为空
 */
public class FetchHttpSessionConfig extends ServerEndpointConfig.Configurator implements ServletRequestListener{

	/**
	 * 修改握手策略
	 * @param sec
	 * @param request
	 * @param response
	 */
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		//获取httpSession 将其放入websocket的用户属性中
		sec.getUserProperties().put(HttpSession.class.getSimpleName(), request.getHttpSession());
	}

	/**
	 * 请求初始化
	 */
	@Override
	public void requestInitialized(ServletRequestEvent servletRequestEvent) {
		//将所有请求都带上httpSession
		((HttpServletRequest)servletRequestEvent.getServletRequest()).getSession();
	}


	/**
	 * 请求销毁, 什么也不做
	 */
	@Override
	public void requestDestroyed(ServletRequestEvent servletRequestEvent) {

	}
}
