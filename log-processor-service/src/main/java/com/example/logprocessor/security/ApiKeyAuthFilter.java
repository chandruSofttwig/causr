package com.example.logprocessor.security;

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
    if (path.startsWith("/actuator/")) {
      return true;
    }
    // Dev emit stays gated by profile; still require API key when configured.
    return !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String headerKey = request.getHeader(API_KEY_HEADER);
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    boolean ok = configuredApiKey.equals(headerKey);
    if (!ok && auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      ok = configuredApiKey.equals(auth.substring(7).trim());
    }
    if (ok) {
      filterChain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"unauthorized\"}");
  }
}
