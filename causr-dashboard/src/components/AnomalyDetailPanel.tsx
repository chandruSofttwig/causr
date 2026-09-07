import { Link } from 'react-router-dom';
import { DataTable } from './DataTable';
import type { AnomalyRow, LogRow } from '../types/dashboard';
import { formatWindowRange, parseAnomalyFeatures } from '../utils/anomaly';

interface AnomalyDetailPanelProps {
  row: AnomalyRow;
  logs: LogRow[];
  loading: boolean;
}

function shortTraceId(traceId?: string): string {
  if (!traceId) return '—';
  if (traceId.length <= 12) return traceId;
  return `${traceId.slice(0, 8)}…${traceId.slice(-4)}`;
}

export function AnomalyDetailPanel({ row, logs, loading }: AnomalyDetailPanelProps) {
  const features = parseAnomalyFeatures(row.feature_json);

  return (
    <div className="anomaly-detail">
      <div className="anomaly-detail-grid">
        <div>
          <span className="anomaly-detail-label">Window</span>
          <span className="mono">{formatWindowRange(row.window_start, row.window_end)}</span>
        </div>
        <div>
          <span className="anomaly-detail-label">Detected</span>
          <span className="mono">{row.created_at ?? '—'}</span>
        </div>
        <div>
          <span className="anomaly-detail-label">Tenant</span>
          <span className="mono">{row.tenant_id ?? '—'}</span>
        </div>
        <div>
          <span className="anomaly-detail-label">ID</span>
          <span className="mono anomaly-detail-id">{row.id ?? '—'}</span>
        </div>
      </div>

      {features && (
        <div className="anomaly-features">
          <span className="anomaly-detail-label">Window metrics</span>
          <div className="anomaly-features-grid mono">
            {features.error_rate != null && (
              <span>error rate {(features.error_rate * 100).toFixed(1)}%</span>
            )}
            {features.log_volume != null && <span>volume {features.log_volume}</span>}
            {features.p99_latency_ms != null && (
              <span>p99 {features.p99_latency_ms.toFixed(0)} ms</span>
            )}
            {features.unique_error_types != null && (
              <span>error types {features.unique_error_types}</span>
            )}
            {features.new_error_types != null && (
              <span>new types {features.new_error_types}</span>
            )}
          </div>
        </div>
      )}

      <div className="anomaly-detail-section">
        <span className="anomaly-detail-label">RCA</span>
        <pre className="anomaly-rca">{row.rca_text?.trim() ? row.rca_text : '(no RCA yet)'}</pre>
      </div>

      <div className="anomaly-detail-section">
        <span className="anomaly-detail-label">Logs in window</span>
        {loading ? (
          <div className="hint">Loading logs…</div>
        ) : (
          <DataTable
            rows={logs}
            rowKey={(_, i) => String(i)}
            emptyMessage="No logs in this anomaly window"
            columns={[
              {
                key: 'time',
                header: 'Time',
                className: 'mono',
                render: (r) => r.timestamp ?? '—',
              },
              {
                key: 'lvl',
                header: 'Level',
                render: (r) => (
                  <span
                    className={
                      r.log_level === 'ERROR'
                        ? 'level-error'
                        : r.log_level === 'WARN'
                          ? 'level-warn'
                          : ''
                    }
                  >
                    {r.log_level ?? '—'}
                  </span>
                ),
              },
              {
                key: 'trace',
                header: 'Trace',
                className: 'mono',
                render: (r) =>
                  r.trace_id ? (
                    <Link to={`/traces/${r.trace_id}`} title={r.trace_id}>
                      {shortTraceId(r.trace_id)}
                    </Link>
                  ) : (
                    '—'
                  ),
              },
              {
                key: 'msg',
                header: 'Message',
                className: 'truncate mono',
                render: (r) => r.message ?? '—',
              },
            ]}
          />
        )}
      </div>
    </div>
  );
}
