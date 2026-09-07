import type { ReactNode } from 'react';

interface PageTitleProps {
  icon: ReactNode;
  title: string;
}

export function PageTitle({ icon, title }: PageTitleProps) {
  return (
    <div className="page-title-row">
      <span className="page-title-icon">{icon}</span>
      <h1 className="page-title">{title}</h1>
    </div>
  );
}
