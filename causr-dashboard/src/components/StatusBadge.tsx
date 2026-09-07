interface StatusBadgeProps {
  status: string;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const normalized = status.toUpperCase();
  let cls = 'status-badge ';
  if (normalized === 'GREEN') cls += 'status-green';
  else if (normalized === 'AMBER') cls += 'status-amber';
  else if (normalized === 'RED') cls += 'status-red';
  else cls += 'status-amber';

  return <span className={cls}>{normalized}</span>;
}
