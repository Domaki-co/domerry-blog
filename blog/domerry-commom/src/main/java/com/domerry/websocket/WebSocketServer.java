package com.domerry.websocket;


import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author: 拾壹
 * @date: 2023/11/13
 * @describe:
 */

@ServerEndpoint(value = "/websocket/{userId}")
@Component
public class WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 记录当前在线连接数
     */
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     * 握手必须携带有效 token（query 参数 token）且与路径中的 userId 一致，防止冒用他人身份建立连接
     */
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        if (!isAuthorized(session, userId)) {
            log.warn("拒绝未授权的websocket连接，userId={}", userId);
            closeQuietly(session);
            return;
        }
        sessionMap.put(userId, session);
        log.info("有新用户加入，username={}, 当前在线人数为：{}", userId, sessionMap.size());
    }

    /**
     * 校验握手 token 是否有效且属于该 userId
     */
    private boolean isAuthorized(Session session, String userId) {
        List<String> tokens = session.getRequestParameterMap().get("token");
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        Object loginId = StpUtil.getLoginIdByToken(tokens.get(0));
        return loginId != null && loginId.toString().equals(userId);
    }

    private void closeQuietly(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "unauthorized"));
        } catch (IOException e) {
            log.error("关闭未授权连接失败", e);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        // 条件移除：旧连接的关闭回调不能误删同一用户重连后的新会话
        sessionMap.remove(userId, session);
        log.info("有一连接关闭，移除userId={}的用户session, 当前在线人数为：{}", userId, sessionMap.size());
    }

    /**
     * 收到客户端消息后调用的方法
     * 后台收到客户端发送过来的消息
     * onMessage 是一个消息的中转站
     * 接受 浏览器端 socket.send 发送过来的 json数据
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String userId) {
        log.debug("服务端收到用户userId={}的消息:{}", userId, message);
        this.sendAllMessage(message);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") String userId) {
        log.error("用户{}的连接发生异常", userId, error);
        // 主动移除并关闭异常会话，避免网络异常断开时 session 滞留在 sessionMap
        sessionMap.remove(userId, session);
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("关闭异常会话失败", e);
        }
    }

    /**
     * 服务端发送消息给客户端
     */
    private void sendMessage(String message, Session toSession) {
        try {
            log.debug("服务端给客户端[{}]发送消息{}", toSession.getId(), message);
            toSession.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error("服务端发送消息给客户端失败", e);
        }
    }

    /**
     * 服务端发送消息给所有客户端
     */
    public void sendAllMessage(String message) {
        // 单个会话发送失败不能中断广播，逐个捕获
        for (Session session : sessionMap.values()) {
            try {
                log.debug("服务端给客户端[{}]发送消息{}", session.getId(), message);
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("服务端发送消息给客户端[{}]失败", session.getId(), e);
            }
        }
    }
}
