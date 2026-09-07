import type { AnomalyFeatures } from '../types/dashboard';

/** IsolationForest: lower score = more anomalous (typical threshold -0.4). */
export function anomalySeverity(score?: number): 'critical' | 'warning' | 'normal' {
  if (score == null || Number.isNaN(score)) {
    return 'normal';
  }
  if (score < -0.4) {
    return 'critical';
  }
  if (score < 0) {
    return 'warning';
  }
  return 'normal';
}

export function formatAnomalyScore(score?: number): string {
  if (score == null || Number.isNaN(score)) {
    return '—';
  }
  return score.toFixed(3);
}

export function parseAnomalyFeatures(featureJson?: string): AnomalyFeatures | null {
  if (!featureJson?.trim()) {
    return null;
  }
  try {
    const raw = JSON.parse(featureJson) as Record<string, unknown>;
    return {
      error_rate: toNum(raw.error_rate),
      log_volume: toNum(raw.log_volume),
      p99_latency_ms: toNum(raw.p99_latency_ms),
      unique_error_types: toNum(raw.unique_error_types),
      new_error_types: toNum(raw.new_error_types),
    };
  } catch {
    return null;
  }
}

function toNum(v: unknown): number | undefined {
  if (typeof v === 'number' && Number.isFinite(v)) {
    return v;
  }
  if (typeof v === 'string' && v.trim() !== '') {
    const n = Number(v);
    return Number.isFinite(n) ? n : undefined;
  }
  return undefined;
}

export function formatWindowRange(start?: string, end?: string): string {
  if (!start && !end) {
    return '—';
  }
  if (start && end) {
    return `${start} → ${end}`;
  }
  return start ?? end ?? '—';
}
