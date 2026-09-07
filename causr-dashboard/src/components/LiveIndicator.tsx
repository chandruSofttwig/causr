import type { LiveStatus } from '../hooks/useLiveDashboard';

const LABELS: Record<LiveStatus, string> = {
  live: 'live',
  connecting: 'connecting',
  offline: 'offline',
};

export function LiveIndicator({ status }: { status: LiveStatus }) {
  return (
    <span className={`live-indicator live-${status}`} title={`WebSocket: ${status}`}>
      <span className="live-dot" aria-hidden />
      {LABELS[status]}
    </span>
  );
}
