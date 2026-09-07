# Plan sheet: simplify Causr local setup further

**Goal:** Get from clone → working dashboard in **one command and under ~5 minutes**, without five Maven terminals or deep infra knowledge.

**Current state:** Two paths exist (full Compose profile vs host JVMs). Still heavy: many containers, Java+Maven+Node, several env vars, and optional RCA/Slack.

---

## Target experience

```bash
git clone <repo> && cd causr
./scripts/dev-up.sh
# → prints Dashboard URL + API key; health-checks until green
```

No manual ordering. No separate `npm`/`mvn` unless hacking a single service.

---

## Principles

1. **One entrypoint** — single script or Compose project that owns the happy path.
2. **Sensible defaults** — API key, tenant, ports baked for local; override only when needed.
3. **Fewer moving parts in default profile** — demote optional deps (Slack, Anthropic, legacy UI).
4. **Hide Kafka/Redis/CH** behind the stack for beginners; keep them accessible for power users.
5. **Fail loud** — health script that names the broken service.

---

## Phased simplification

### Phase S1 — DX wrappers (1–2 days) — highest ROI

| Work | Outcome |
|------|---------|
| `scripts/dev-up.sh` | `compose --profile apps up -d --build`, wait for health, open/print URLs |
| `scripts/dev-down.sh` / `dev-logs.sh` | Tear down / follow logs for apps profile |
| `scripts/smoke.sh` | Curl summary + emit-anomaly + optional RCA check |
| Root `Makefile` targets | `make up`, `make down`, `make smoke`, `make ui` |
| Root README points only to [SETUP.md](SETUP.md) Path A | Reduce “which terminal?” confusion |

**Done when:** New contributor runs `make up && make smoke` successfully.

### Phase S2 — Collapse runtimes (3–5 days)

| Work | Outcome |
|------|---------|
| Default Compose profile = apps (or rename `apps` → default) | Infra-only becomes `--profile infra` |
| Pre-built images on GHCR (CI push) | `docker compose pull && up` without local Maven builds |
| Dashboard nginx proxies `/api` + `/ws` to BFF | Browser talks to one origin; drop dual Vite env bases for Docker path |
| Single `causr.env` documented in SETUP | One file; deprecate scattered per-service copies |

**Done when:** Fresh machine with only Docker can run the stack (no JDK/Node required for demo).

### Phase S3 — Optional “lite” profile (1 week)

| Work | Outcome |
|------|---------|
| `profile: lite` | Skip llm-router, Slack, maybe traces UI assets |
| Replace Kafka with in-process queue **or** Redpanda single binary for lite | Faster cold start (trade: not prod-parity) |
| Embed IsolationForest scoring in processor JVM **or** keep ai-service but document as required | One less “is AI up?” failure mode if merged |
| Remove / archive `observability-dashboard` from default docs | One UI only |

**Done when:** Lite profile boots in &lt;2 minutes on a laptop and shows KPIs + logs.

### Phase S4 — Product packaging (later)

| Work | Outcome |
|------|---------|
| Helm chart or `docker compose` prod overlay | Same mental model, different env |
| `causr doctor` CLI | Diagnoses collector/Kafka lag/CH schema/API key mismatch |
| Guided first-run TUI | Asks Slack/Anthropic once, writes `.env` |

---

## What not to simplify away

- ClickHouse (query model is core)
- OTLP ingest path (collector or equivalent)
- Tenant + API key (security baseline)
- Dev anomaly emit endpoint (demo reliability)

---

## Suggested decision defaults

| Choice | Default for simplification plan |
|--------|----------------------------------|
| Primary local path | Docker Compose apps profile + `make up` |
| Host Maven/Node | Power-user / service hacking only |
| Kafka | Keep for full profile; consider Redpanda only in lite |
| AI scorer | Keep as container until scoring is in-process |
| RCA | Opt-in via env; stack healthy without Anthropic |

---

## Success metrics

| Metric | Today (approx.) | Target |
|--------|-----------------|--------|
| Commands to first UI | 6–10 | 1–2 |
| Required local toolchains for demo | Docker + JDK + Maven + Node | Docker only |
| Time to first KPI row | 15–30+ min (cold) | &lt; 10 min with pulled images |
| Doc pages to read first | README + PROJECT + tribal | SETUP Path A only |

---

## Immediate next actions (when ready to implement)

1. Add `Makefile` + `scripts/dev-up.sh` / `smoke.sh`  
2. Proxy API through dashboard nginx in Compose  
3. Publish images from CI to skip `--build` on first run  
4. Demote lite extras and delete legacy UI from the happy-path docs  

---

*Plan sheet created for post-readiness DX work. Does not change runtime behavior by itself.*
