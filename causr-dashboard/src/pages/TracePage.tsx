import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchTrace } from '../api/bff';
import { DataTable } from '../components/DataTable';
import { ErrorBanner } from '../components/ErrorBanner';
import { TracesIcon } from '../components/icons';
import { PageTitle } from '../components/PageTitle';
import type { LogRow } from '../types/dashboard';
import type { TraceSpan, TraceView } from '../types/trace';

function shortId(id?: string): string {
  if (!id) return '—';
  if (id.length <= 12) return id;
  return `${id.slice(0, 8)}…${id.slice(-4)}`;
}

function levelClass(level?: string): string {
  const l = (level ?? '').toUpperCase();
  if (l === 'ERROR' || l === 'FATAL') return 'level-error';
  if (l === 'WARN' || l === 'WARNING') return 'level-warn';
  return '';
}

export function TracePage() {
  const { traceId = '' } = useParams();
  const [data, setData] = useState<TraceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!traceId) {
      setError('Missing trace id');
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const view = await fetchTrace(traceId);
      setData(view);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load trace');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [traceId]);

  const spans = data?.spans ?? [];
  const logs = data?.logs ?? [];

  const maxDuration = useMemo(() => {
    let max = 1;
    for (const s of spans) {
      const d = Number(s.duration_ms ?? 0);
      if (d > max) max = d;
    }
    return max;
  }, [spans]);

  const empty = !loading && spans.length === 0 && logs.length === 0;

  return (
    <>
      <div className="page-header">
        <PageTitle icon={<TracesIcon width={18} height={18} />} title="Trace" />
        <span className="page-meta mono" title={traceId}>
          {shortId(traceId)}
        </span>
      </div>

      <p className="hint">
        <Link to="/logs">← Logs</Link>
        {' · '}
        <span className="mono">{traceId}</span>
      </p>

      {error && <ErrorBanner message={error} onRetry={() => void load()} />}

      {loading && <div className="hint">Loading trace…</div>}

      {empty && (
        <div className="panel">
          <p className="hint">
            No spans or logs for this trace id. Synthetic log traffic may not always emit matching
            spans — try another <code>trace_id</code> from the Logs page.
          </p>
        </div>
      )}

      {!loading && spans.length > 0 && (
        <div className="panel">
          <div className="panel-head">
            <span>Spans</span>
          </div>
          <DataTable<TraceSpan>
            rows={spans}
            rowKey={(r, i) => String(r.span_id ?? i)}
            emptyMessage="No spans"
            columns={[
              {
                key: 'svc',
                header: 'Service',
                render: (r) => r.service_name ?? '—',
              },
              {
                key: 'name',
                header: 'Span',
                className: 'truncate mono',
                render: (r) => r.span_name ?? '—',
              },
              {
                key: 'kind',
                header: 'Kind',
                render: (r) => r.span_kind ?? '—',
              },
              {
                key: 'status',
                header: 'Status',
                render: (r) => String(r.status_code ?? '—'),
              },
              {
                key: 'dur',
                header: 'Duration',
                className: 'mono',
                render: (r) =>
                  r.duration_ms != null ? `${Number(r.duration_ms).toFixed(0)} ms` : '—',
              },
              {
                key: 'bar',
                header: 'Relative',
                render: (r) => {
                  const d = Number(r.duration_ms ?? 0);
                  const pct = Math.max(2, Math.round((d / maxDuration) * 100));
                  return (
                    <div className="span-bar-track" title={`${d} ms`}>
                      <div className="span-bar-fill" style={{ width: `${pct}%` }} />
                    </div>
                  );
                },
              },
              {
                key: 'id',
                header: 'Span id',
                className: 'mono',
                render: (r) => shortId(r.span_id),
              },
            ]}
          />
        </div>
      )}

      {!loading && (
        <div className="panel">
          <div className="panel-head">
            <span>Related logs</span>
          </div>
          <DataTable<LogRow>
            rows={logs}
            rowKey={(_, i) => String(i)}
            emptyMessage="No logs for this trace"
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
                key: 'msg',
                header: 'Message',
                className: 'truncate mono',
                render: (r) => r.message ?? '—',
              },
            ]}
          />
        </div>
      )}
    </>
  );
}
