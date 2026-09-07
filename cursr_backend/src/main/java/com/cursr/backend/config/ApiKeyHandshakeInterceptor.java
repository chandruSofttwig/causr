package com.cursr.backend.config;

import com.cursr.backend.security.ApiKeyAuthFilter;
import com.cursr.backend.security.TenantContext;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class ApiKeyHandshakeInterceptor implements HandshakeInterceptor {

  private final ApiKeyAuthFilter apiKeyAuthFilter;

  public ApiKeyHandshakeInterceptor(ApiKeyAuthFilter apiKeyAuthFilter) {
    this.apiKeyAuthFilter = apiKeyAuthFilter;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    if (!apiKeyAuthFilter.isEnabled()) {
      stashTenant(request, attributes);
      return true;
    }
    String key = null;
    if (request instanceof ServletServerHttpRequest servletRequest) {
      key = servletRequest.getServletRequest().getParameter("apiKey");
      if (key == null || key.isBlank()) {
        key = servletRequest.getServletRequest().getHeader(ApiKeyAuthFilter.API_KEY_HEADER);
      }
    }
    if (!apiKeyAuthFilter.apiKeyMatches(key)) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
    stashTenant(request, attributes);
    return true;
  }

  private static void stashTenant(ServerHttpRequest request, Map<String, Object> attributes) {
    String tenant = "default";
    if (request instanceof ServletServerHttpRequest servletRequest) {
      String header = servletRequest.getServletRequest().getHeader("X-Tenant-Id");
      String param = servletRequest.getServletRequest().getParameter("tenantId");
      tenant = TenantContext.normalize(header != null && !header.isBlank() ? header : param);
    }
    attributes.put("tenantId", tenant);
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
