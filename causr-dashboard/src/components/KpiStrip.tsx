import type { DashboardKpis } from '../types/dashboard';

interface KpiStripProps {
  kpis?: DashboardKpis;
}

function dirClass(direction?: string): string {
  if (direction === 'up') return 'kpi-dir-up';
  if (direction === 'down') return 'kpi-dir-down';
  return 'kpi-dir-flat';
}

function fmtNum(n: unknown, digits = 1): string {
  if (n == null || n === '') return '—';
  const v = Number(n);
  if (Number.isNaN(v)) return String(n);
  return v.toFixed(digits);
}

export function KpiStrip({ kpis }: KpiStripProps) {
  const er = kpis?.errorRate;
  const p99 = kpis?.p99LatencyMs;
  const rpm = kpis?.requestsPerMinute;
  const svc = kpis?.servicesHealthy;
  const ttd = kpis?.timeToDetect;

  return (
    <div className="kpi-strip">
      <div className="kpi-cell">
        <div className="kpi-label">Error rate</div>
        <div className="kpi-value">{fmtNum(er?.currentPercent, 2)}%</div>
        <div className={`kpi-sub ${dirClass(er?.direction)}`}>
          vs {fmtNum(er?.previousPercent, 2)}% · {er?.window ?? '5m'}
        </div>
      </div>
      <div className="kpi-cell">
        <div className="kpi-label">P99 latency</div>
        <div className="kpi-value">{fmtNum(p99?.value, 0)} ms</div>
        <div className={`kpi-sub ${dirClass(p99?.direction)}`}>
          was {fmtNum(p99?.previousValue, 0)} ms · {p99?.health ?? '—'}
        </div>
      </div>
      <div className="kpi-cell">
        <div className="kpi-label">Req/min</div>
        <div className="kpi-value">{fmtNum(rpm?.current, 0)}</div>
        <div className={`kpi-sub ${dirClass(rpm?.direction)}`}>
          prev {fmtNum(rpm?.previous, 0)} · {rpm?.window ?? '1m'}
        </div>
      </div>
      <div className="kpi-cell">
        <div className="kpi-label">Services healthy</div>
        <div className="kpi-value">
          {svc?.healthy ?? '—'}/{svc?.total ?? '—'}
        </div>
        <div className="kpi-sub">{svc?.severity ?? '—'}</div>
      </div>
      <div className="kpi-cell">
        <div className="kpi-label">Time to detect</div>
        <div className="kpi-value">
          {ttd?.seconds != null ? `${fmtNum(ttd.seconds, 0)}s` : '—'}
        </div>
        <div className="kpi-sub">last incident</div>
      </div>
    </div>
  );
}
