"use client";

import { API_BASE } from "@/lib/api";
import { FormEvent, useState } from "react";

type Row = Record<string, unknown>;

export default function SearchPage() {
  const [q, setQ] = useState("");
  const [rows, setRows] = useState<Row[]>([]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!q.trim()) {
      setRows([]);
      return;
    }
    const r = await fetch(`${API_BASE}/api/search?q=${encodeURIComponent(q.trim())}`);
    if (r.ok) setRows((await r.json()) as Row[]);
  }

  return (
    <main>
      <h1 className="text-xl font-semibold mb-4">Search</h1>
      <form onSubmit={onSubmit} className="flex gap-2 mb-6">
        <input
          className="flex-1 border rounded px-3 py-2 text-sm bg-white dark:bg-zinc-900 border-zinc-300 dark:border-zinc-700"
          placeholder="Search log message (ILIKE)"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button
          type="submit"
          className="px-4 py-2 rounded bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 text-sm"
        >
          Search
        </button>
      </form>
      <ul className="space-y-2 text-sm">
        {rows.map((r, i) => (
          <li key={i} className="border border-zinc-200 dark:border-zinc-800 rounded p-3">
            <div className="text-xs text-zinc-500">{String(r.timestamp ?? "")}</div>
            <div className="font-medium">
              {String(r.service_name ?? "")} · {String(r.log_level ?? "")}
            </div>
            <div className="mt-1">{String(r.message ?? "")}</div>
          </li>
        ))}
      </ul>
    </main>
  );
}
