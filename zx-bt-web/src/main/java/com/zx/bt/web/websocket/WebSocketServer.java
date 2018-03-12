package com.zx.bt.web.websocket;

import com.zx.bt.common.store.CommonCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * author:ZhengXing
 * datetime:2018-03-12 0:06
 * webSocket服务器
 */
@Slf4j
@ServerEndpoint(value = "/websocket")
@Component
public class WebSocketServer {
	private static final String LOG = "[WebSocketServer]";

	private final CommonCache<Connection> webSocketConnectionCache;

	public WebSocketServer(CommonCache<Connection> webSocketConnectionCache) {
		this.webSocketConnectionCache = webSocketConnectionCache;
	}


	/**
	 *  建立连接
	 * @param session websocket中的session
	 * @param config 服务端配置 通过该类获取Http的session
	 */
	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		//获取到httpSession
		HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		//创建连接对象
		Connection connection = new Connection(httpSession, session);
		//根据websocket的session 的id为key,存储
		webSocketConnectionCache.put(session.getId(),connection);
	}

	/**
	 * 收到消息
	 */
	@OnMessage
	public void onMessage(Session session,WebSocketRequest request) {

	}

	/**
	 * 关闭连接时的操作
	 */
	@OnClose
	public void onClose(Session session) {
		log.info("{}连接关闭",LOG);
		webSocketConnectionCache.remove(session.getId());
	}

	/**
	 * 发生异常
	 */
	@OnError
	public void onError(Session session,Throwable error) {
		log.info("{}发生异常:{}",LOG,error.getMessage(),error);
		closeSession(session);
	}

	/**
	 * 关闭Session
	 */
	public void closeSession(Session session) {
		if (session.isOpen()) {
			try {
				session.close();
			} catch (IOException e) {
				log.error("{}关闭session异常:{}",e.getMessage());
			}
		}
	}


}
