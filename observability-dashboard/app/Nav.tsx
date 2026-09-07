import Link from "next/link";

export function Nav() {
  const links = [
    { href: "/", label: "Home" },
    { href: "/logs", label: "Logs" },
    { href: "/anomalies", label: "Anomalies" },
    { href: "/clusters", label: "Clusters" },
    { href: "/search", label: "Search" },
  ];
  return (
    <nav className="flex gap-4 border-b border-zinc-200 dark:border-zinc-800 pb-3 mb-6 text-sm">
      {links.map((l) => (
        <Link
          key={l.href}
          href={l.href}
          className="text-zinc-700 dark:text-zinc-300 hover:underline"
        >
          {l.label}
        </Link>
      ))}
    </nav>
  );
}
