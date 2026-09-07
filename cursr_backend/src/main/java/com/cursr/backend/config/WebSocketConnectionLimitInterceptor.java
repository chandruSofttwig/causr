package com.cursr.backend.config;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class WebSocketConnectionLimitInterceptor implements HandshakeInterceptor {

  private final int maxConnections;
  private final AtomicInteger activeConnections;

  public WebSocketConnectionLimitInterceptor(int maxConnections) {
    this.maxConnections = Math.max(1, maxConnections);
    this.activeConnections = new AtomicInteger(0);
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    int current = activeConnections.incrementAndGet();
    if (current > maxConnections) {
      activeConnections.decrementAndGet();
      return false;
    }
    attributes.put("wsConnectionCounted", true);
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // count decrement is handled by SessionDisconnectEvent listener in config
  }

  public void onDisconnect() {
    activeConnections.updateAndGet((count) -> Math.max(0, count - 1));
  }
}
