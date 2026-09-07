-- Run once on existing ClickHouse DBs (column added to schema.sql for new installs).
ALTER TABLE observability.anomalies
    ADD COLUMN IF NOT EXISTS rca_generated_at Nullable(DateTime) AFTER rca_text;
