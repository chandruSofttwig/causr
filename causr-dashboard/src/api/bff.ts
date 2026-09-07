import { BFF_API_BASE, authHeaders } from '../config';
import type { AnomalyRow, DashboardSummary, LogRow } from '../types/dashboard';
import type { TraceView } from '../types/trace';

async function bffGet<T>(path: string): Promise<T> {
  const res = await fetch(`${BFF_API_BASE}${path}`, { headers: authHeaders() });
  if (!res.ok) {
    throw new Error(`BFF request failed: ${res.status} ${res.statusText}`);
  }
  return (await res.json()) as T;
}

export async function fetchSummary(): Promise<DashboardSummary> {
  return bffGet<DashboardSummary>('/api/dashboard/summary');
}

export async function fetchAnomalyDetail(id: string): Promise<AnomalyRow> {
  return bffGet<AnomalyRow>(`/api/dashboard/anomalies/${encodeURIComponent(id)}`);
}

export async function fetchAnomalyLogs(id: string, limit = 50): Promise<LogRow[]> {
  return bffGet<LogRow[]>(
    `/api/dashboard/anomalies/${encodeURIComponent(id)}/logs?limit=${limit}`,
  );
}

export async function fetchTrace(traceId: string): Promise<TraceView> {
  return bffGet<TraceView>(`/api/dashboard/trace/${encodeURIComponent(traceId)}`);
}

export async function fetchRecentLogsViaBff(limit = 100): Promise<LogRow[]> {
  return bffGet<LogRow[]>(`/api/dashboard/recent-logs?limit=${limit}`);
}
