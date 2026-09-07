import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchAnomalyDetail, fetchAnomalyLogs, fetchSummary } from '../api/bff';
import { AnomalyDetailPanel } from '../components/AnomalyDetailPanel';
import { AnomalyScoreBadge } from '../components/AnomalyScoreBadge';
import { ErrorBanner } from '../components/ErrorBanner';
import { AnomaliesIcon } from '../components/icons';
import { LiveIndicator } from '../components/LiveIndicator';
import { PageTitle } from '../components/PageTitle';
import { useLiveDashboard, type AnomalyAlertPush } from '../hooks/useLiveDashboard';
import type { AnomalyRow, LogRow } from '../types/dashboard';
import { formatWindowRange } from '../utils/anomaly';

const FALLBACK_POLL_MS = 60_000;

function upsertAnomaly(list: AnomalyRow[], row: AnomalyRow): AnomalyRow[] {
  if (!row.id) return [row, ...list];
  const idx = list.findIndex((a) => String(a.id) === String(row.id));
  if (idx < 0) return [row, ...list];
  const next = [...list];
  next[idx] = { ...next[idx], ...row };
  return next;
}

export function AnomaliesPage() {
  const [rows, setRows] = useState<AnomalyRow[]>([]);
  const [openId, setOpenId] = useState<string | null>(null);
  const [detail, setDetail] = useState<AnomalyRow | null>(null);
  const [logs, setLogs] = useState<LogRow[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const summary = await fetchSummary();
      setRows(summary.anomalies ?? []);
      setLastUpdated(summary.generatedAt ?? new Date().toISOString());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load anomalies');
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
    onSummary: (summary) => {
      setRows(summary.anomalies ?? []);
      setLastUpdated(summary.generatedAt ?? new Date().toISOString());
      setError(null);
      setLoading(false);
    },
    onAnomaly: (row, push: AnomalyAlertPush) => {
      setRows((prev) => upsertAnomaly(prev, row));
      setLastUpdated(new Date().toISOString());
      setLoading(false);
      if (openId && row.id && String(openId) === String(row.id)) {
        setDetail((prev) => ({ ...(prev ?? {}), ...row }));
        if (push.type === 'rca-ready' || push.rcaText) {
          void fetchAnomalyDetail(String(row.id)).then(setDetail).catch(() => undefined);
        }
      }
    },
  });

  const sorted = useMemo(() => {
    return [...rows].sort((a, b) => {
      const as = Number(a.anomaly_score ?? 0);
      const bs = Number(b.anomaly_score ?? 0);
      return as - bs;
    });
  }, [rows]);

  const toggleRow = async (id: string) => {
    if (openId === id) {
      setOpenId(null);
      setDetail(null);
      setLogs([]);
      return;
    }
    setOpenId(id);
    setDetailLoading(true);
    setDetail(null);
    setLogs([]);
    try {
      const [row, windowLogs] = await Promise.all([
        fetchAnomalyDetail(id),
        fetchAnomalyLogs(id),
      ]);
      setDetail(row);
      setLogs(windowLogs);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load anomaly detail');
      setOpenId(null);
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <PageTitle icon={<AnomaliesIcon width={18} height={18} />} title="Anomalies" />
        <div className="page-header-actions">
          <LiveIndicator status={status} />
          {lastUpdated && (
            <span className="page-meta">updated {lastUpdated}</span>
          )}
          <span className="page-meta">{sorted.length} in last hour</span>
          <button type="button" className="btn-refresh" onClick={() => void load()}>
            Refresh
          </button>
        </div>
      </div>

      <p className="hint">
        AI-detected service windows (last hour). Lower score = more anomalous. Click a row for
        metrics and related logs.
      </p>

      {error && <ErrorBanner message={error} onRetry={() => void load()} />}

      {loading && sorted.length === 0 ? (
        <div className="hint">Loading…</div>
      ) : sorted.length === 0 ? (
        <div className="anomaly-empty panel">
          <p className="hint">No anomalies in the last hour.</p>
          <p className="hint">
            Ensure <code>log-processor-service</code> runs with{' '}
            <code>SPRING_PROFILES_ACTIVE=dev</code> and <code>ai-service</code> is up, or emit a
            test anomaly:
          </p>
          <pre className="anomaly-rca mono">
            curl -X POST &apos;http://localhost:8080/api/dev/emit-anomaly?serviceName=payment-service&amp;environment=staging&apos;
          </pre>
        </div>
      ) : (
        <ul className="anomaly-list">
          {sorted.map((r, i) => {
            const id = String(r.id ?? i);
            const isOpen = openId === id;
            return (
              <li key={id} className={`anomaly-item${isOpen ? ' anomaly-item-open' : ''}`}>
                <button
                  type="button"
                  className="anomaly-item-button"
                  onClick={() => void toggleRow(id)}
                  aria-expanded={isOpen}
                >
                  <div className="anomaly-item-head">
                    <span className="anomaly-item-service">
                      {r.service_name ?? '—'}
                      <span className="anomaly-item-env">{r.environment ?? '—'}</span>
                    </span>
                    <AnomalyScoreBadge score={Number(r.anomaly_score)} />
                  </div>
                  <div className="anomaly-item-meta">
                    {formatWindowRange(r.window_start, r.window_end)}
                    {r.created_at ? ` · detected ${r.created_at}` : ''}
                  </div>
                </button>
                {isOpen && (
                  <AnomalyDetailPanel
                    row={detail ?? r}
                    logs={logs}
                    loading={detailLoading}
                  />
                )}
              </li>
            );
          })}
        </ul>
      )}

      <p className="hint">
        <Link to="/">Back to dashboard</Link>
      </p>
    </>
  );
}
