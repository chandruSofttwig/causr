import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageTitle } from '../components/PageTitle';
import { TracesIcon } from '../components/icons';

export function TraceLookupPage() {
  const [traceId, setTraceId] = useState('');
  const navigate = useNavigate();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const id = traceId.trim();
    if (!id) return;
    navigate(`/traces/${encodeURIComponent(id)}`);
  };

  return (
    <>
      <div className="page-header">
        <PageTitle icon={<TracesIcon width={18} height={18} />} title="Traces" />
      </div>
      <p className="hint">
        Open a trace by id (from Logs or anomaly-related log rows), or paste a hex trace id below.
      </p>
      <form className="filter-row" onSubmit={onSubmit}>
        <input
          placeholder="trace id"
          value={traceId}
          onChange={(e) => setTraceId(e.target.value)}
          className="mono"
          style={{ minWidth: 280, flex: 1 }}
        />
        <button type="submit" className="btn-refresh">
          Open
        </button>
      </form>
    </>
  );
}
