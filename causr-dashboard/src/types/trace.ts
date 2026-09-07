import type { LogRow } from './dashboard';

export interface TraceSpan {
  start_time?: string;
  trace_id?: string;
  span_id?: string;
  parent_span_id?: string;
  service_name?: string;
  span_name?: string;
  span_kind?: string;
  status_code?: string | number;
  duration_ms?: number;
  attributes?: string;
}

export interface TraceView {
  spans: TraceSpan[];
  logs: LogRow[];
}
