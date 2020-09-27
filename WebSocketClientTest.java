package com.cloudminds.ross.wss.controller;

import com.cloudminds.ross.common.base.BaseController;
import com.cloudminds.ross.common.base.BaseResp;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import org.springframework.web.socket.sockjs.transport.handler.SockJsWebSocketHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/socket")
public class WebSocketClientTest extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientTest.class);

    private static final List<WebSocketSession> list = new ArrayList<>(10240);
    private static final List<StompSession> stompOverSockjsSessionList = new ArrayList<>(10240);

    @GetMapping("/connect/{num}")
    public BaseResp connect(@PathVariable Integer num) {
        long start = System.currentTimeMillis();
        stompOverSockjs(num);

        System.out.println(System.currentTimeMillis() - start);

        return respSuccess();
    }

    @GetMapping("/sub")
    public BaseResp sub() {

        for (StompSession session : stompOverSockjsSessionList) {
            if(session.isConnected()) {
                session.subscribe("/topic/skych", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {

                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        log.info("received msg: " + payload.toString());
                    }
                });
            } else {
                log.warn("Session is not connected.");
            }
        }

        return respSuccess();
    }

    @GetMapping("/disconnect")
    public BaseResp disconnect() {

        log.info("size: {}", stompOverSockjsSessionList.size());

        for (StompSession session :stompOverSockjsSessionList) {
            if (session.isConnected()) {
                try {
                    session.disconnect();
                } catch (Exception e) {
                    log.error("Disconnect websocket error.", e);
                }
            }
        }

        stompOverSockjsSessionList.clear();

        return respSuccess();
    }

    private void stompOverSockjs(Integer num) {
        if (num == null) {
            return;
        }
        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());

        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.setTaskScheduler(new ThreadPoolTaskScheduler()); // for heartbeats

        String url = "ws://10.14.32.240:8096/ws-endpoint";

        for (int i = 0; i < num; i++) {
            ListenableFuture<StompSession> connect = stompClient.connect(url, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    stompOverSockjsSessionList.add(session);
                    log.info("Connected...");
                    super.afterConnected(session, connectedHeaders);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    log.error("handleTransportError...", exception);
                    super.handleTransportError(session, exception);
                }
            });
        }

    }

    private static SockJsClient sockjsConnect02() {
        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());

        SockJsClient sockJsClient = new SockJsClient(transports);
        ListenableFuture<WebSocketSession> future = sockJsClient.doHandshake(new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                list.add(session);
                log.info("afterConnectionEstablished...");
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                super.handleMessage(session, message);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                log.info("handleTextMessage...");
            }

            @Override
            protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
                log.info("handleBinaryMessage...");
            }

            @Override
            protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
                log.info("handlePongMessage...");
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.info("handleTransportError...");
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                log.info("afterConnectionClosed...");
            }

            @Override
            public boolean supportsPartialMessages() {
                return super.supportsPartialMessages();
            }
        }, "ws://10.14.32.240:8096/ws-endpoint");


        return sockJsClient;
    }

    private static SockJsClient sockjsConnect01() {

        HttpClient jettyHttpClient = new HttpClient();
        jettyHttpClient.setMaxConnectionsPerDestination(10240);
        jettyHttpClient.setExecutor(new QueuedThreadPool(10240));

        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(new JettyWebSocketClient()));
        transports.add(new JettyXhrTransport(jettyHttpClient));

        SockJsClient sockJsClient = new SockJsClient(transports);
        sockJsClient.doHandshake(new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                list.add(session);
                log.info("afterConnectionEstablished...");
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                super.handleMessage(session, message);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                log.info("handleTextMessage...");
            }

            @Override
            protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
                log.info("handleBinaryMessage...");
            }

            @Override
            protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
                log.info("handlePongMessage...");
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.info("handleTransportError...");
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                log.info("afterConnectionClosed...");
            }

            @Override
            public boolean supportsPartialMessages() {
                return super.supportsPartialMessages();
            }
        }, "ws://10.14.32.240:8096/ws-endpoint");

        return sockJsClient;
    }

    private static void stompConnect() {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.setTaskScheduler(new ThreadPoolTaskScheduler()); // for heartbeats

        String url = "ws://10.14.32.240:8096/ws-endpoint";

        for (int i = 0; i < 10000; i++) {
            ListenableFuture<StompSession> connect = stompClient.connect(url, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    log.info("Connected...");
                    super.afterConnected(session, connectedHeaders);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    log.error("handleTransportError...", exception);
                    super.handleTransportError(session, exception);
                }
            });
        }
    }
}
