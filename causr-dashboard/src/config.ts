export const BFF_API_BASE =
  import.meta.env.VITE_BFF_API_BASE?.replace(/\/$/, '') || 'http://localhost:8090';

export const PROCESSOR_API_BASE =
  import.meta.env.VITE_PROCESSOR_API_BASE?.replace(/\/$/, '') || 'http://localhost:8080';

/** Native WebSocket STOMP endpoint on cursr_backend (no SockJS). */
export const BFF_WS_URL =
  import.meta.env.VITE_BFF_WS_URL?.replace(/\/$/, '') ||
  BFF_API_BASE.replace(/^http/, 'ws') + '/ws/anomalies';

export const TENANT_ID =
  import.meta.env.VITE_TENANT_ID?.trim() || 'default';

/** Shared with BFF/processor when app.security.api-key is set. */
export const API_KEY =
  import.meta.env.VITE_API_KEY?.trim() || 'causr-local-key';

export function authHeaders(extra?: HeadersInit): HeadersInit {
  return {
    'X-API-Key': API_KEY,
    'X-Tenant-Id': TENANT_ID,
    ...(extra ?? {}),
  };
}

export function wsBrokerUrl(): string {
  const url = new URL(BFF_WS_URL);
  url.searchParams.set('apiKey', API_KEY);
  url.searchParams.set('tenantId', TENANT_ID);
  return url.toString();
}
