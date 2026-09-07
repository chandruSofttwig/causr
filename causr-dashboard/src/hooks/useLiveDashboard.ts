import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { useEffect, useRef, useState } from 'react';
import { TENANT_ID, wsBrokerUrl } from '../config';
import type { AnomalyRow, DashboardSummary } from '../types/dashboard';

export type LiveStatus = 'connecting' | 'live' | 'offline';

export interface AnomalyAlertPush {
  id?: string;
  windowStartEpochMs?: number;
  windowEndEpochMs?: number;
  tenantId?: string;
  serviceName?: string;
  environment?: string;
  anomalyScore?: number;
  featureJson?: string;
  type?: string;
  rcaText?: string;
  rcaGeneratedAt?: string;
}

function epochMsToIso(ms?: number): string | undefined {
  if (ms == null || !Number.isFinite(ms)) return undefined;
  return new Date(ms).toISOString();
}

export function anomalyAlertToRow(push: AnomalyAlertPush): AnomalyRow {
  return {
    id: push.id,
    window_start: epochMsToIso(push.windowStartEpochMs),
    window_end: epochMsToIso(push.windowEndEpochMs),
    tenant_id: push.tenantId,
    service_name: push.serviceName,
    environment: push.environment,
    anomaly_score: push.anomalyScore,
    is_anomaly: 1,
    feature_json: push.featureJson,
    rca_text: push.rcaText,
    rca_generated_at: push.rcaGeneratedAt,
    created_at: new Date().toISOString(),
  };
}

function parseJson<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export interface UseLiveDashboardOptions {
  onSummary?: (summary: DashboardSummary) => void;
  onAnomaly?: (row: AnomalyRow, push: AnomalyAlertPush) => void;
  enabled?: boolean;
}

export function useLiveDashboard(options: UseLiveDashboardOptions = {}) {
  const { onSummary, onAnomaly, enabled = true } = options;
  const [status, setStatus] = useState<LiveStatus>('offline');
  const onSummaryRef = useRef(onSummary);
  const onAnomalyRef = useRef(onAnomaly);
  onSummaryRef.current = onSummary;
  onAnomalyRef.current = onAnomaly;

  useEffect(() => {
    if (!enabled) {
      setStatus('offline');
      return;
    }

    setStatus('connecting');
    const subscriptions: StompSubscription[] = [];

    const client = new Client({
      brokerURL: wsBrokerUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus('live');
        const onSummaryMsg = (msg: IMessage) => {
          const summary = parseJson<DashboardSummary>(msg.body);
          if (summary) onSummaryRef.current?.(summary);
        };
        subscriptions.push(
          client.subscribe(`/topic/dashboard/summary/${TENANT_ID}`, onSummaryMsg),
        );
        if (TENANT_ID === 'default') {
          subscriptions.push(client.subscribe('/topic/dashboard/summary', onSummaryMsg));
        }
        subscriptions.push(
          client.subscribe(`/topic/anomalies/${TENANT_ID}`, (msg: IMessage) => {
            const push = parseJson<AnomalyAlertPush>(msg.body);
            if (!push) return;
            onAnomalyRef.current?.(anomalyAlertToRow(push), push);
          }),
        );
      },
      onDisconnect: () => setStatus('offline'),
      onStompError: () => setStatus('offline'),
      onWebSocketClose: () => setStatus('offline'),
      onWebSocketError: () => setStatus('offline'),
    });

    client.activate();

    return () => {
      for (const sub of subscriptions) {
        try {
          sub.unsubscribe();
        } catch {
          /* ignore */
        }
      }
      void client.deactivate();
      setStatus('offline');
    };
  }, [enabled]);

  return { status };
}
