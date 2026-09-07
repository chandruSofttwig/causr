package com.cursr.backend.security;

public final class TenantContext {

  private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

  private TenantContext() {}

  public static void set(String tenantId) {
    TENANT.set(normalize(tenantId));
  }

  public static String get() {
    String t = TENANT.get();
    return t == null ? "default" : t;
  }

  public static void clear() {
    TENANT.remove();
  }

  public static String normalize(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "default";
    }
    return tenantId.trim();
  }
}
