export type JsonRecord = Record<string, unknown>;

export interface KpiValue {
  currentPercent?: number;
  previousPercent?: number;
  value?: number;
  previousValue?: number;
  window?: string;
  compareWindow?: string;
  direction?: string;
  health?: string;
  current?: number;
  previous?: number;
  healthy?: number;
  total?: number;
  severity?: string;
  seconds?: number;
}

export interface DashboardKpis {
  errorRate?: KpiValue;
  p99LatencyMs?: KpiValue;
  requestsPerMinute?: KpiValue;
  servicesHealthy?: KpiValue;
  timeToDetect?: KpiValue;
}

export interface ServiceHealthRow {
  service_name?: string;
  error_percent?: number;
  p99_ms?: number;
  rps?: number;
  status?: string;
}

export interface TopErrorRow {
  service_name?: string;
  message?: string;
  count?: number;
  error_count?: number;
  trend?: string;
}

export interface AnomalyRow {
  id?: string;
  window_start?: string;
  window_end?: string;
  tenant_id?: string;
  service_name?: string;
  environment?: string;
  anomaly_score?: number;
  is_anomaly?: number;
  rca_text?: string;
  rca_generated_at?: string;
  feature_json?: string;
  created_at?: string;
}

export interface AnomalyFeatures {
  error_rate?: number;
  log_volume?: number;
  p99_latency_ms?: number;
  unique_error_types?: number;
  new_error_types?: number;
}

export interface DashboardSummary {
  summaryVersion?: number;
  generatedAt?: string;
  kpis?: DashboardKpis;
  serviceHealth?: ServiceHealthRow[];
  topErrors?: TopErrorRow[];
  anomalies?: AnomalyRow[];
}

export interface LogRow {
  timestamp?: string;
  service_name?: string;
  log_level?: string;
  message?: string;
  cluster_id?: string;
  anomaly_score?: number;
  duration_ms?: number;
  trace_id?: string;
  span_id?: string;
}
