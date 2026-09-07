import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchRecentLogs } from '../api/processor';
import { DataTable } from '../components/DataTable';
import { ErrorBanner } from '../components/ErrorBanner';
import { LogsIcon } from '../components/icons';
import { PageTitle } from '../components/PageTitle';
import type { LogRow } from '../types/dashboard';

const POLL_MS = 2000;

function levelClass(level?: string): string {
  const l = (level ?? '').toUpperCase();
  if (l === 'ERROR' || l === 'FATAL') return 'level-error';
  if (l === 'WARN' || l === 'WARNING') return 'level-warn';
  return '';
}

function shortTraceId(traceId?: string): string {
  if (!traceId) return '—';
  if (traceId.length <= 12) return traceId;
  return `${traceId.slice(0, 8)}…${traceId.slice(-4)}`;
}

export function LogsPage() {
  const [rows, setRows] = useState<LogRow[]>([]);
  const [service, setService] = useState('');
  const [level, setLevel] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const data = await fetchRecentLogs();
      setRows(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load logs');
    }
  };

  useEffect(() => {
    void load();
    const t = setInterval(() => void load(), POLL_MS);
    return () => clearInterval(t);
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((x) => {
      const sn = String(x.service_name ?? '');
      const lv = String(x.log_level ?? '');
      if (service && !sn.toLowerCase().includes(service.toLowerCase())) return false;
      if (level && !lv.toLowerCase().includes(level.toLowerCase())) return false;
      return true;
    });
  }, [rows, service, level]);

  return (
    <>
      <div className="page-header">
        <PageTitle icon={<LogsIcon width={18} height={18} />} title="Logs" />
        <span className="page-meta">poll {POLL_MS / 1000}s · {filtered.length} rows</span>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => void load()} />}

      <div className="filter-row">
        <input
          placeholder="service"
          value={service}
          onChange={(e) => setService(e.target.value)}
        />
        <input
          placeholder="level"
          value={level}
          onChange={(e) => setLevel(e.target.value)}
        />
      </div>

      <div className="panel">
        <DataTable
          rows={filtered}
          rowKey={(_, i) => String(i)}
          emptyMessage="No logs"
          columns={[
            {
              key: 'time',
              header: 'Time',
              className: 'mono',
              render: (r) => r.timestamp ?? '—',
            },
            { key: 'svc', header: 'Service', render: (r) => r.service_name ?? '—' },
            {
              key: 'lvl',
              header: 'Level',
              render: (r) => (
                <span className={levelClass(r.log_level)}>{r.log_level ?? '—'}</span>
              ),
            },
            {
              key: 'latency',
              header: 'Latency',
              className: 'mono',
              render: (r) =>
                r.duration_ms != null ? `${r.duration_ms} ms` : '—',
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
              key: 'cluster',
              header: 'Cluster',
              className: 'mono',
              render: (r) => r.cluster_id || '—',
            },
            {
              key: 'score',
              header: 'Score',
              className: 'mono',
              render: (r) =>
                r.anomaly_score != null ? String(r.anomaly_score) : '—',
            },
            {
              key: 'msg',
              header: 'Message',
              className: 'truncate mono',
              render: (r) => r.message ?? '—',
            },
          ]}
        />
      </div>
    </>
  );
}
