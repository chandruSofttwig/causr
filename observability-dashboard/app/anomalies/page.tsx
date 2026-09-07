"use client";

import { API_BASE } from "@/lib/api";
import { useEffect, useState } from "react";

type Row = Record<string, unknown>;

export default function AnomaliesPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [open, setOpen] = useState<number | null>(null);

  useEffect(() => {
    void (async () => {
      const r = await fetch(`${API_BASE}/api/anomalies`);
      if (r.ok) setRows((await r.json()) as Row[]);
    })();
  }, []);

  const sorted = [...rows].sort((a, b) => {
    const as = Number(a.anomaly_score ?? 0);
    const bs = Number(b.anomaly_score ?? 0);
    return as - bs;
  });

  return (
    <main>
      <h1 className="text-xl font-semibold mb-4">Anomalies</h1>
      <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-4">
        Lower isolation score = more anomalous. Expand a row for RCA text.
      </p>
      <ul className="space-y-2">
        {sorted.map((r, i) => (
          <li
            key={i}
            className="border border-zinc-200 dark:border-zinc-800 rounded p-3 cursor-pointer"
            onClick={() => setOpen(open === i ? null : i)}
          >
            <div className="flex justify-between gap-2">
              <span className="font-medium">{String(r.service_name ?? "")}</span>
              <span className="text-sm text-zinc-500">score {String(r.anomaly_score ?? "")}</span>
            </div>
            <div className="text-xs text-zinc-500 mt-1">
              {String(r.window_start ?? "")} — env {String(r.environment ?? "")}
            </div>
            {open === i && (
              <pre className="mt-3 text-sm whitespace-pre-wrap bg-zinc-100 dark:bg-zinc-900 p-3 rounded">
                {String(r.rca_text ?? "(no RCA yet)")}
              </pre>
            )}
          </li>
        ))}
      </ul>
    </main>
  );
}
