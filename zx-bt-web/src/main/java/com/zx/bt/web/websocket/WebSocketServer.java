package com.zx.bt.web.websocket;

import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.store.CommonCache;
import com.zx.bt.common.util.CodeUtil;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.web.websocket.dto.*;
import com.zx.bt.web.websocket.enums.WebSocketMessageCodeEnum;
import com.zx.bt.web.websocket.enums.WebSocketMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@ServerEndpoint(value = "/websocket",
		decoders = {MessageDecoder.class},
		encoders = {MessageEncoder.class},
		configurator = FetchHttpSessionConfig.class
)
@Component
public class WebSocketServer {
	private static final String LOG = "[WebSocketServer]";

	private static CommonCache<Connection> webSocketConnectionCache;

	/**
	 * 由于该类被使用时不是由Spring 创建的那个Bean对象,所以需要静态属性
	 */
	@Autowired
	public void init(CommonCache<Connection> webSocketConnectionCache) {
		WebSocketServer.webSocketConnectionCache = webSocketConnectionCache;
	}




	/**
	 * 建立连接
	 *
	 * @param session websocket中的session
	 * @param config  服务端配置 通过该类获取Http的session
	 */
	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		//获取到httpSession,注意该key要和FetchHttpSessionConfig存入时的一致
		HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getSimpleName());
		//创建连接对象
		Connection connection = new Connection(httpSession, session);
		//根据websocket的session 的id为key,存储
		webSocketConnectionCache.put(session.getId(), connection);
	}

	/**
	 * 收到消息
	 */
	@OnMessage
	public void onMessage(WebSocketRequestDTO webSocketRequestDTO,Session session) throws Exception  {
		try {
			//校验时间戳,如果失败,抛出异常
			verifyRequestTimestamp(session, webSocketRequestDTO);

			switch (EnumUtil.getByCode(webSocketRequestDTO.getType(), WebSocketMessageTypeEnum.class)
					.orElseThrow(() -> new BTException("消息类型不存在:当前类型:" + webSocketRequestDTO.getType()))) {
				//握手请求
				case HANDSHAKE:
					//发送握手响应(sessionId)
					sendMessageOne(session,new WebSocketResponseDTO<>(
							session.getId(),WebSocketMessageTypeEnum.HANDSHAKE.getCode(),
							new HandshakeResponseDTO(session.getId())));
					break;
				//弹幕请求
				case BARRAGE:
					String barrageMessage = ((BarrageRequestDTO) webSocketRequestDTO.getData()).getBarrageMessage();
					//如果为空肯定是用户越权发送的.不做处理
					if(StringUtils.isBlank(barrageMessage))
						return;
					log.info("{}[onMessage]弹幕消息:{}",LOG,barrageMessage);
					WebSocketResponseDTO<BarrageResponseDTO> barrage = new WebSocketResponseDTO<>(WebSocketMessageTypeEnum.BARRAGE.getCode(), System.currentTimeMillis(),
							new BarrageResponseDTO(barrageMessage));
					sendMessageAll(barrage);
					break;
			}
		} catch (BTException e) {
			//发送异常响应
			sendMessageOne(session, new WebSocketResponseDTO<>(session.getId(),
					webSocketRequestDTO.getType(), WebSocketMessageCodeEnum.TOKEN_OR_TIMESTAMP_ERROR));
		} catch (Exception e) {
			log.error("{}[onMessage]发生未知异常:{}",e.getMessage(),e);
			//发送异常响应
			sendMessageOne(session, new WebSocketResponseDTO<>(session.getId(),
					webSocketRequestDTO.getType(), WebSocketMessageCodeEnum.UNKNOWN_ERROR));
		}
	}

	/**
	 * 校验请求token,当失败时,发送异常响应
	 */
	public void verifyRequestToken(Session session, WebSocketRequestDTO<?> webSocketRequestDTO) throws BTException{
		//token校验失败
		if(!webSocketRequestDTO.verifyToken(session.getId())){
			log.info("{}[verifyRequestToken]校验token失败");
			throw new BTException(WebSocketMessageCodeEnum.TOKEN_OR_TIMESTAMP_ERROR);
		}
	}

	/**
	 * 校验请求时间戳,当失败时,发送异常响应
	 */
	public void verifyRequestTimestamp(Session session, WebSocketRequestDTO<?> webSocketRequestDTO) throws BTException{
		//token校验失败
		if(!webSocketRequestDTO.verifyTimestamp()){
			log.info("{}[verifyRequestTimestamp]校验timestamp失败");
			throw new BTException(WebSocketMessageCodeEnum.TOKEN_OR_TIMESTAMP_ERROR);
		}
	}




	/**
	 * 向某个客户端发送数据
	 */
	public  void sendMessageOne(Session session, WebSocketResponseDTO<?> webSocketResponseDTO) {
		try {
			if (session.isOpen()) {
				session.getAsyncRemote().sendObject(webSocketResponseDTO);
			} else {
				session.close();
				webSocketConnectionCache.remove(session.getId());
			}
		} catch (Exception e) {
			log.error("{}[sendMessageOne]向用户发送数据失败:{}",e.getMessage(),e);
		}
	}

	/**
	 * 向所有用户发送弹幕
	 */
	public void sendMessageAll(WebSocketResponseDTO<?> webSocketResponseDTO) {
		webSocketConnectionCache.getValues().parallelStream().forEach(item ->{
			try {
				sendMessageOne(item.getWebSocketSession(), webSocketResponseDTO.setHash(
						CodeUtil.stringToMd5(webSocketResponseDTO.getCode()+item.getWebSocketSession().getId() + webSocketResponseDTO.getTimestamp())));
			} catch (Exception e) {
				log.error("{}[sendMessageAll]向某用户发送数据失败:{}",e.getMessage(),e);
			}
		});
	}

	/**
	 * 关闭连接时的操作
	 */
	@OnClose
	public void onClose(Session session) {
		log.info("{}连接关闭", LOG);
		webSocketConnectionCache.remove(session.getId());
	}

	/**
	 * 发生异常
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.info("{}发生异常:{}", LOG, error.getMessage(),error);
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
				log.error("{}关闭session异常:{}", e.getMessage());
			}
		}
	}


}
