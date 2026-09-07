import { anomalySeverity, formatAnomalyScore } from '../utils/anomaly';

interface AnomalyScoreBadgeProps {
  score?: number;
}

export function AnomalyScoreBadge({ score }: AnomalyScoreBadgeProps) {
  const severity = anomalySeverity(score);
  const cls =
    severity === 'critical'
      ? 'anomaly-score-badge anomaly-score-critical'
      : severity === 'warning'
        ? 'anomaly-score-badge anomaly-score-warning'
        : 'anomaly-score-badge anomaly-score-normal';

  return <span className={cls}>{formatAnomalyScore(score)}</span>;
}
