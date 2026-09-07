import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchSummary } from '../api/bff';
import { AnomalyScoreBadge } from '../components/AnomalyScoreBadge';
import { DataTable } from '../components/DataTable';
import { ErrorBanner } from '../components/ErrorBanner';
import { AnomaliesIcon, DashboardIcon, ServiceHealthIcon, TopErrorsIcon } from '../components/icons';
import { KpiStrip } from '../components/KpiStrip';
import { LiveIndicator } from '../components/LiveIndicator';
import { PageTitle } from '../components/PageTitle';
import { StatusBadge } from '../components/StatusBadge';
import { useLiveDashboard } from '../hooks/useLiveDashboard';
import type { AnomalyRow, DashboardSummary, TopErrorRow } from '../types/dashboard';
import { formatWindowRange } from '../utils/anomaly';

/** Slow REST fallback when WebSocket is down or for cache warm. */
const FALLBACK_POLL_MS = 60_000;

function trendClass(trend?: string): string {
  if (trend === 'up') return 'trend-up';
  if (trend === 'down') return 'trend-down';
  return 'trend-flat';
}

function upsertAnomaly(list: AnomalyRow[], row: AnomalyRow): AnomalyRow[] {
  if (!row.id) return [row, ...list];
  const idx = list.findIndex((a) => String(a.id) === String(row.id));
  if (idx < 0) return [row, ...list];
  const next = [...list];
  next[idx] = { ...next[idx], ...row };
  return next;
}

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const data = await fetchSummary();
      setSummary(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load summary');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const t = setInterval(() => void load(), FALLBACK_POLL_MS);
    return () => clearInterval(t);
  }, [load]);

  const { status } = useLiveDashboard({
    onSummary: (s) => {
      setSummary(s);
      setError(null);
      setLoading(false);
    },
    onAnomaly: (row) => {
      setSummary((prev) => {
        if (!prev) {
          return {
            generatedAt: new Date().toISOString(),
            anomalies: [row],
          };
        }
        return {
          ...prev,
          anomalies: upsertAnomaly(prev.anomalies ?? [], row),
          generatedAt: new Date().toISOString(),
        };
      });
    },
  });

  const topErrors = (summary?.topErrors ?? []).slice(0, 10);
  const recentAnomalies = [...(summary?.anomalies ?? [])]
    .sort((a, b) => Number(a.anomaly_score ?? 0) - Number(b.anomaly_score ?? 0))
    .slice(0, 5);

  return (
    <>
      <div className="page-header">
        <PageTitle icon={<DashboardIcon width={18} height={18} />} title="Dashboard" />
        <div className="page-header-actions">
          <LiveIndicator status={status} />
          {summary?.generatedAt && (
            <span className="page-meta">{summary.generatedAt}</span>
          )}
        </div>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => void load()} />}

      {loading && !summary ? (
        <div className="kpi-strip">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="kpi-cell">
              <div className="skeleton" style={{ width: '60%', marginBottom: 4 }} />
              <div className="skeleton" style={{ width: '40%' }} />
            </div>
          ))}
        </div>
      ) : (
        <KpiStrip kpis={summary?.kpis} />
      )}

      <div className="panel">
        <div className="panel-head">
          <ServiceHealthIcon />
          <span>Service health</span>
        </div>
        <DataTable
          rows={summary?.serviceHealth ?? []}
          rowKey={(r) => String(r.service_name ?? Math.random())}
          emptyMessage="No service health data"
          columns={[
            { key: 'svc', header: 'Service', render: (r) => r.service_name ?? '—' },
            {
              key: 'err',
              header: 'Error %',
              className: 'mono',
              render: (r) =>
                r.error_percent != null ? Number(r.error_percent).toFixed(2) : '—',
            },
            {
              key: 'p99',
              header: 'P99 ms',
              className: 'mono',
              render: (r) => (r.p99_ms != null ? Number(r.p99_ms).toFixed(0) : '—'),
            },
            {
              key: 'rps',
              header: 'RPS',
              className: 'mono',
              render: (r) => (r.rps != null ? Number(r.rps).toFixed(2) : '—'),
            },
            {
              key: 'status',
              header: 'Status',
              render: (r) => <StatusBadge status={String(r.status ?? 'AMBER')} />,
            },
          ]}
        />
      </div>

      <div className="panel">
        <div className="panel-head">
          <TopErrorsIcon />
          <span>Top errors (30m)</span>
        </div>
        <DataTable<TopErrorRow>
          rows={topErrors}
          rowKey={(r, i) => `${r.service_name}-${i}`}
          emptyMessage="No errors in window"
          columns={[
            { key: 'svc', header: 'Service', render: (r) => r.service_name ?? '—' },
            {
              key: 'msg',
              header: 'Message',
              className: 'truncate mono',
              render: (r) => r.message ?? '—',
            },
            {
              key: 'cnt',
              header: 'Count',
              className: 'mono',
              render: (r) => String(r.count ?? r.error_count ?? '—'),
            },
            {
              key: 'trend',
              header: 'Trend',
              render: (r) => (
                <span className={trendClass(r.trend)}>{r.trend ?? '—'}</span>
              ),
            },
          ]}
        />
      </div>

      <div className="panel">
        <div className="panel-head panel-head-split">
          <span className="panel-head-title">
            <AnomaliesIcon />
            <span>Recent anomalies (1h)</span>
          </span>
          <Link className="panel-head-link" to="/anomalies">
            View all
          </Link>
        </div>
        <DataTable<AnomalyRow>
          rows={recentAnomalies}
          rowKey={(r, i) => String(r.id ?? i)}
          emptyMessage="No anomalies in the last hour"
          columns={[
            { key: 'svc', header: 'Service', render: (r) => r.service_name ?? '—' },
            { key: 'env', header: 'Env', render: (r) => r.environment ?? '—' },
            {
              key: 'score',
              header: 'Score',
              render: (r) => <AnomalyScoreBadge score={Number(r.anomaly_score)} />,
            },
            {
              key: 'window',
              header: 'Window',
              className: 'mono truncate',
              render: (r) => formatWindowRange(r.window_start, r.window_end),
            },
          ]}
        />
      </div>
    </>
  );
}
