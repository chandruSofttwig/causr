"use client";

import { API_BASE } from "@/lib/api";
import { useEffect, useState } from "react";

type Row = Record<string, unknown>;

export default function ClustersPage() {
  const [rows, setRows] = useState<Row[]>([]);

  useEffect(() => {
    void (async () => {
      const r = await fetch(`${API_BASE}/api/clusters`);
      if (r.ok) setRows((await r.json()) as Row[]);
    })();
  }, []);

  return (
    <main>
      <h1 className="text-xl font-semibold mb-4">Clusters</h1>
      <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-4">
        SummingMergeTree rollup from ClickHouse (hot tier). Sparklines = placeholder in V1.
      </p>
      <div className="overflow-x-auto border border-zinc-200 dark:border-zinc-800 rounded">
        <table className="w-full text-sm">
          <thead className="bg-zinc-100 dark:bg-zinc-900">
            <tr>
              <th className="text-left p-2">Tenant</th>
              <th className="text-left p-2">Cluster</th>
              <th className="text-left p-2">Count</th>
              <th className="text-left p-2">Template / message</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i} className="border-t border-zinc-100 dark:border-zinc-800">
                <td className="p-2">{String(r.tenant_id ?? "")}</td>
                <td className="p-2">
                  <span className="rounded bg-amber-100 dark:bg-amber-900/30 px-1.5 py-0.5 text-xs">
                    {String(r.cluster_id ?? "")}
                  </span>
                </td>
                <td className="p-2">{String(r.event_count ?? "")}</td>
                <td className="p-2 max-w-lg truncate">{String(r.representative_message ?? "")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
