import { PROCESSOR_API_BASE, authHeaders } from '../config';
import type { LogRow } from '../types/dashboard';
import { fetchRecentLogsViaBff } from './bff';

/** Prefer BFF gateway; fall back to processor if BFF recent-logs fails. */
export async function fetchRecentLogs(): Promise<LogRow[]> {
  try {
    return await fetchRecentLogsViaBff();
  } catch {
    const res = await fetch(`${PROCESSOR_API_BASE}/api/logs/recent`, {
      headers: authHeaders(),
    });
    if (!res.ok) {
      throw new Error(`Processor logs failed: ${res.status} ${res.statusText}`);
    }
    return (await res.json()) as LogRow[];
  }
}
