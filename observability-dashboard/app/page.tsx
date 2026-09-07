import Link from "next/link";

export default function Home() {
  return (
    <main>
      <h1 className="text-2xl font-semibold mb-2">Observability dashboard</h1>
      <p className="text-zinc-600 dark:text-zinc-400 mb-6">
        Four views wired to the Spring Boot log-processor API (
        <code className="text-sm">NEXT_PUBLIC_API_BASE</code>).
      </p>
      <ul className="space-y-2 list-disc list-inside">
        <li>
          <Link className="underline" href="/logs">
            Live logs
          </Link>{" "}
          — poller + optional filters
        </li>
        <li>
          <Link className="underline" href="/anomalies">
            Anomalies
          </Link>{" "}
          — sorted by isolation score; expand RCA
        </li>
        <li>
          <Link className="underline" href="/clusters">
            Clusters
          </Link>{" "}
          — Drain3 / log_clusters rollups
        </li>
        <li>
          <Link className="underline" href="/search">
            Search
          </Link>{" "}
          — ClickHouse ILIKE on message
        </li>
      </ul>
    </main>
  );
}
