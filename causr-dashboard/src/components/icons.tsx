import type { ReactNode, SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement>;

const base: IconProps = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
};

function Icon(path: ReactNode, props: IconProps) {
  return (
    <svg {...base} {...props}>
      {path}
    </svg>
  );
}

export function DashboardIcon(props: IconProps) {
  return Icon(
    <>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </>,
    props,
  );
}

export function LogsIcon(props: IconProps) {
  return Icon(
    <>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="8" y1="13" x2="16" y2="13" />
      <line x1="8" y1="17" x2="13" y2="17" />
    </>,
    props,
  );
}

export function AnomaliesIcon(props: IconProps) {
  return Icon(
    <>
      <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </>,
    props,
  );
}

export function ServiceHealthIcon(props: IconProps) {
  return Icon(
    <>
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
    </>,
    props,
  );
}

export function TopErrorsIcon(props: IconProps) {
  return Icon(
    <>
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </>,
    props,
  );
}

export function TracesIcon(props: IconProps) {
  return Icon(
    <>
      <path d="M3 6h18" />
      <path d="M7 6v12" />
      <path d="M12 6v8" />
      <path d="M17 6v10" />
      <circle cx="7" cy="18" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="12" cy="14" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="17" cy="16" r="1.5" fill="currentColor" stroke="none" />
    </>,
    props,
  );
}
