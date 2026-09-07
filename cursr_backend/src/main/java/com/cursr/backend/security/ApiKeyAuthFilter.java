package com.cursr.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * When {@code app.security.api-key} is non-blank, require {@code X-API-Key} or {@code Authorization:
 * Bearer} for /api/**.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String API_KEY_HEADER = "X-API-Key";

  private final String configuredApiKey;

  public ApiKeyAuthFilter(@Value("${app.security.api-key:}") String configuredApiKey) {
    this.configuredApiKey = configuredApiKey == null ? "" : configuredApiKey.trim();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (configuredApiKey.isBlank()) {
      return true;
    }
    String path = request.getRequestURI();
    if (path.startsWith("/actuator/health") || path.startsWith("/actuator/info")) {
      return true;
    }
    return !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (matches(request)) {
      filterChain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Missing or invalid API key\"}");
  }

  private boolean matches(HttpServletRequest request) {
    String headerKey = request.getHeader(API_KEY_HEADER);
    if (configuredApiKey.equals(headerKey)) {
      return true;
    }
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return configuredApiKey.equals(auth.substring(7).trim());
    }
    return false;
  }

  public boolean isEnabled() {
    return !configuredApiKey.isBlank();
  }

  public boolean apiKeyMatches(String candidate) {
    return isEnabled() && configuredApiKey.equals(candidate == null ? "" : candidate.trim());
  }
}
