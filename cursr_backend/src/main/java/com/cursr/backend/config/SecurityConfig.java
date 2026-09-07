package com.cursr.backend.config;

import com.cursr.backend.security.ApiKeyAuthFilter;
import com.cursr.backend.security.TenantContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter, TenantContextFilter tenantContextFilter)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info", "/ws/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(tenantContextFilter, ApiKeyAuthFilter.class);
    return http.build();
  }

  /** Avoid double registration of @Component filters via Servlet container. */
  @Bean
  public FilterRegistrationBean<ApiKeyAuthFilter> disableApiKeyServletRegistration(
      ApiKeyAuthFilter filter) {
    FilterRegistrationBean<ApiKeyAuthFilter> reg = new FilterRegistrationBean<>(filter);
    reg.setEnabled(false);
    return reg;
  }

  @Bean
  public FilterRegistrationBean<TenantContextFilter> disableTenantServletRegistration(
      TenantContextFilter filter) {
    FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(filter);
    reg.setEnabled(false);
    return reg;
  }
}
