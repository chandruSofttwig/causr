import './landing-page.css';
import datadogLogoUrl from '../../assets/logo/datadog.svg';
import openTelemetryLogoUrl from '../../assets/logo/OpenTelemetry.svg';
import prometheusLogoUrl from '../../assets/logo/Prometheus.svg';
import grafanaLogoUrl from '../../assets/logo/Grafana.svg';
import pagerDutyLogoUrl from '../../assets/logo/pagerduty-ar21.svg';
import slackLogoUrl from '../../assets/logo/Slack.svg';
import kubernetesLogoUrl from '../../assets/logo/Kubernetes.svg';
import lokiLogoUrl from '../../assets/logo/grafana-loki.svg';
import appLogoUrl from '../../assets/logo/causr_logo.png';

export function LandingPage() {
  return (
    <>
      {/* NAV */}
      <nav>
        <div className='nav-inner'>
          <a href='#' className='nav-logo'>
            <img src={appLogoUrl} alt='Cursr' className='app-logo' />
          </a>
          <ul className='nav-links'>
            <li>
              <a href='#features'>Features</a>
            </li>
            <li>
              <a href='#integrations'>Integrations</a>
            </li>
            <li>
              <a href='#how'>How it works</a>
            </li>
            <li>
              <a href='#pricing'>Pricing</a>
            </li>
            <li>
              <a href='#docs'>Docs</a>
            </li>
          </ul>
          <div className='nav-cta'>
            <a className='btn btn-ghost' href='http://localhost:5173'>
              Sign in
            </a>
            <a className='btn btn-primary' href='http://localhost:5173'>
              Get started free
            </a>
          </div>
        </div>
      </nav>

      {/* HERO */}
      <section className='hero' id='hero'>
        <div className='grid-bg fade-b'></div>
        <div className='hero-glow'></div>
        <div className='page'>
          <div className='hero-layout'>
            <div className='hero-left'>
              <div className='hero-eyebrow'>
                <span className='eyebrow-dot'></span>
                Incident Intelligence Platform
              </div>
              <h1>
                Stop guessing.<br />
                <em>Start understanding.</em>
              </h1>
              <p className='hero-functional'>
                Cursr reconstructs incidents automatically from your logs, metrics,
                deploys, and alerts — so you know what broke and why in minutes.
              </p>
              <p className='hero-sub'>
                Engineers spend <strong>33% of their time firefighting.</strong>{' '}
                Cursr shows you what actually happened the moment something breaks —
                without the tab-switching, log-hunting, and Slack threads.
              </p>
              <div className='hero-actions'>
                <button className='btn btn-primary btn-xl'>Get started free</button>
                <button className='btn btn-ghost btn-lg'>
                  <svg width='14' height='14' viewBox='0 0 14 14' fill='none'>
                    <circle
                      cx='7'
                      cy='7'
                      r='6'
                      stroke='currentColor'
                      strokeWidth='1.2'
                    />
                    <path d='M5.5 4.8 L9.5 7 L5.5 9.2 Z' fill='currentColor' />
                  </svg>
                  View demo
                </button>
              </div>
              <div className='hero-social'>
                <div className='avatar-stack'>
                  <div className='avatar a1'>S</div>
                  <div className='avatar a2'>K</div>
                  <div className='avatar a3'>M</div>
                  <div className='avatar a4'>R</div>
                </div>
                Trusted by engineering teams running microservices
              </div>
            </div>

            {/* HERO UI MOCKUP */}
            <div className='hero-ui'>
              <div className='ui-window'>
                <div className='ui-titlebar'>
                  <div className='traffic'>
                    <div className='tr tr-r'></div>
                    <div className='tr tr-y'></div>
                    <div className='tr tr-g'></div>
                  </div>
                  <div className='ui-title-text'>
                    cursr — incident · checkout-service · 03:47 UTC
                  </div>
                </div>
                <div className='ui-body'>
                  <div className='inc-header'>
                    <div>
                      <div className='inc-title'>Checkout service degraded</div>
                      <div className='inc-sub'>
                        payment-api · prod-us-east-1 · started 03:47
                      </div>
                    </div>
                    <div className='inc-badge'>
                      <div className='inc-badge-dot'></div>
                      ACTIVE
                    </div>
                  </div>

                  <div className='ai-panel'>
                    <div className='ai-label'>Cursr AI · root cause</div>
                    <div className='ai-text'>
                      Database connection pool exhausted after deploy at 03:44.
                      Upstream timeout in payment-api caused cascade. 3 of 12 pods
                      affected. No data loss.
                    </div>
                  </div>

                  <div className='timeline'>
                    <div className='tl-item'>
                      <div className='tl-dot critical'></div>
                      <div className='tl-time'>03:44</div>
                      <div className='tl-content'>
                        <div className='tl-event'>Deploy: payment-api v2.3.1</div>
                        <div className='tl-detail'>
                          connection_pool_size: 10 → 5
                        </div>
                      </div>
                    </div>
                    <div className='tl-item'>
                      <div className='tl-dot warn'></div>
                      <div className='tl-time'>03:46</div>
                      <div className='tl-content'>
                        <div className='tl-event'>
                          p99 latency spike: 120ms → 4.2s
                        </div>
                        <div className='tl-detail'>
                          checkout-api · 847 requests affected
                        </div>
                      </div>
                    </div>
                    <div className='tl-item'>
                      <div className='tl-dot critical'></div>
                      <div className='tl-time'>03:47</div>
                      <div className='tl-content'>
                        <div className='tl-event'>PagerDuty alert fired</div>
                        <div className='tl-detail'>CheckoutErrorRate &gt; 5%</div>
                      </div>
                    </div>
                    <div className='tl-item'>
                      <div className='tl-dot info'></div>
                      <div className='tl-time'>03:49</div>
                      <div className='tl-content'>
                        <div className='tl-event'>You opened Cursr</div>
                        <div className='tl-detail'>timeline generated in 1.2s</div>
                      </div>
                    </div>
                  </div>

                  <div className='metrics-row'>
                    <div className='metric-mini'>
                      <div className='metric-mini-label'>error rate</div>
                      <div className='metric-mini-val red'>8.4%</div>
                      <div className='sparkline'>
                        <div className='spark-bar' style={{ height: '25%' }}></div>
                        <div className='spark-bar' style={{ height: '20%' }}></div>
                        <div className='spark-bar' style={{ height: '30%' }}></div>
                        <div className='spark-bar' style={{ height: '25%' }}></div>
                        <div className='spark-bar hi' style={{ height: '100%' }}></div>
                        <div className='spark-bar hi' style={{ height: '90%' }}></div>
                        <div className='spark-bar hi' style={{ height: '85%' }}></div>
                      </div>
                    </div>
                    <div className='metric-mini'>
                      <div className='metric-mini-label'>p99 latency</div>
                      <div className='metric-mini-val amber'>4.2s</div>
                      <div className='sparkline'>
                        <div className='spark-bar' style={{ height: '15%' }}></div>
                        <div className='spark-bar' style={{ height: '18%' }}></div>
                        <div className='spark-bar' style={{ height: '20%' }}></div>
                        <div className='spark-bar md' style={{ height: '70%' }}></div>
                        <div className='spark-bar md' style={{ height: '90%' }}></div>
                        <div className='spark-bar md' style={{ height: '100%' }}></div>
                        <div className='spark-bar md' style={{ height: '95%' }}></div>
                      </div>
                    </div>
                    <div className='metric-mini'>
                      <div className='metric-mini-label'>pods affected</div>
                      <div className='metric-mini-val red'>3/12</div>
                      <div className='sparkline'>
                        <div className='spark-bar' style={{ height: '10%' }}></div>
                        <div className='spark-bar' style={{ height: '10%' }}></div>
                        <div className='spark-bar' style={{ height: '10%' }}></div>
                        <div className='spark-bar hi' style={{ height: '60%' }}></div>
                        <div className='spark-bar hi' style={{ height: '65%' }}></div>
                        <div className='spark-bar hi' style={{ height: '60%' }}></div>
                        <div className='spark-bar hi' style={{ height: '62%' }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* SOCIAL PROOF */}
      <div className='social-proof'>
        <div className='social-proof-inner'>
          <div className='social-proof-label'>Trusted by teams building</div>
          <div className='social-proof-tags'>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#3b82f6' }}></span>
              Fintech
            </div>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#8b5cf6' }}></span>
              SaaS
            </div>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#10b981' }}></span>
              E-commerce
            </div>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#f59e0b' }}></span>
              AI Infrastructure
            </div>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#ef4444' }}></span>
              Developer Tools
            </div>
            <div className='sp-tag'>
              <span className='sp-tag-dot' style={{ background: '#06b6d4' }}></span>
              Cloud Infrastructure
            </div>
          </div>
        </div>
      </div>

      <div className='section-divider'></div>

      {/* STATS BAR */}
      <section className='stats-section'>
        <div className='page'>
          <div className='stats-bar'>
            <div className='stat-item'>
              <div className='stat-value'>&lt;2min</div>
              <div className='stat-label'>Avg time to first incident insight</div>
              <div className='stat-note'>from alert to root cause</div>
            </div>
            <div className='stat-item'>
              <div className='stat-value'>40%</div>
              <div className='stat-label'>
                Reduction in MTTR reported by teams
              </div>
              <div className='stat-note'>vs. manual log investigation</div>
            </div>
            <div className='stat-item'>
              <div className='stat-value'>12+</div>
              <div className='stat-label'>Data sources correlated automatically</div>
              <div className='stat-note'>logs · metrics · deploys · alerts</div>
            </div>
            <div className='stat-item'>
              <div className='stat-value'>5min</div>
              <div className='stat-label'>Time to first timeline after setup</div>
              <div className='stat-note'>no agent, no config changes</div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* PROBLEM SECTION */}
      <section className='problem-section' id='problem'>
        <div className='page'>
          <div className='section-label'>The problem</div>
          <h2 className='section-h2'>
            It's Tuesday at 3am.<br />
            <em>Your checkout is down.</em>
          </h2>
          <p className='section-lead'>
            Your monitoring fired 47 alerts. Three of them mattered. Your on-call
            engineer dismissed the one that took down checkout for 22 minutes —
            because it looked like everything else.
          </p>

          <div className='tuesday-card'>
            <div className='tuesday-top'>
              <div className='traffic'>
                <div className='tr tr-r'></div>
                <div className='tr tr-y'></div>
                <div className='tr tr-g'></div>
              </div>
              <div className='tuesday-title'>pagerduty — on-call alerts · tonight</div>
              <div className='tuesday-alert-badge'>47 alerts · 2 actionable</div>
            </div>
            <div className='alert-list'>
              <div className='alert-row'>
                <div className='alert-icon red'>⚠</div>
                <div className='alert-text'>
                  <div className='alert-name'>High CPU — worker-node-07</div>
                  <div className='alert-meta'>prod-us-east-1 · cpu &gt; 80% for 5m</div>
                </div>
                <div className='alert-time'>03:41</div>
              </div>
              <div className='alert-row'>
                <div className='alert-icon amber'>⚠</div>
                <div className='alert-text'>
                  <div className='alert-name'>Memory pressure — search-service</div>
                  <div className='alert-meta'>heap &gt; 1.8GB · auto-resolved 3x today</div>
                </div>
                <div className='alert-time'>03:42</div>
              </div>
              <div className='alert-row'>
                <div className='alert-icon red'>🔴</div>
                <div className='alert-text'>
                  <div className='alert-name'>
                    CheckoutErrorRate &gt; 5%{' '}
                    <span
                      style={{
                        color: '#f87171',
                        fontSize: '11px',
                        fontFamily: 'var(--font-mono)',
                        marginLeft: '6px',
                      }}
                    >
                      ← this one mattered
                    </span>
                  </div>
                  <div className='alert-meta'>payment-api · 847 failed transactions</div>
                </div>
                <div className='alert-time'>03:47</div>
              </div>
              <div className='alert-row'>
                <div className='alert-icon blue'>ℹ</div>
                <div className='alert-text'>
                  <div className='alert-name'>Disk usage — log-aggregator-02</div>
                  <div className='alert-meta'>73% used · not critical</div>
                </div>
                <div className='alert-time'>03:48</div>
              </div>
              <div className='alert-row'>
                <div className='alert-icon amber'>⚠</div>
                <div className='alert-text'>
                  <div className='alert-name'>Pod restart loop — metrics-collector</div>
                  <div className='alert-meta'>OOMKilled · seen 8x this week</div>
                </div>
                <div className='alert-time'>03:49</div>
              </div>
              <div className='alert-row' style={{ justifyContent: 'center', padding: '10px' }}>
                <span
                  style={{
                    fontSize: '11.5px',
                    color: 'var(--text2)',
                    fontFamily: 'var(--font-mono)',
                  }}
                >
                  + 42 more alerts
                </span>
              </div>
            </div>
          </div>

          <div className='problem-stats'>
            <div className='p-stat'>
              <div className='p-stat-val r'>50</div>
              <div className='p-stat-label'>alerts per week, per on-call engineer</div>
            </div>
            <div className='p-stat'>
              <div className='p-stat-val a'>2–5%</div>
              <div className='p-stat-label'>of alerts actually require human action</div>
            </div>
            <div className='p-stat'>
              <div className='p-stat-val b'>45min</div>
              <div className='p-stat-label'>average time to understand an incident</div>
            </div>
            <div className='p-stat'>
              <div className='p-stat-val g'>33%</div>
              <div className='p-stat-label'>of engineering time lost to firefighting</div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* POSITIONING STATEMENT */}
      <section className='positioning-section'>
        <div className='page'>
          <p className='positioning-line'>
            <span className='dim'>Observability tools show data.</span>
            <br />
            Cursr shows <em>what happened.</em>
          </p>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* BEFORE / AFTER */}
      <section className='ba-section'>
        <div className='page'>
          <div className='section-label'>Before / After</div>
          <h2 className='section-h2'>
            Debugging today vs.
            <br />
            <em>debugging with Cursr</em>
          </h2>

          <div className='ba-grid'>
            {/* BEFORE */}
            <div className='ba-card before'>
              <div className='ba-header'>
                <svg viewBox='0 0 14 14' fill='none'>
                  <circle cx='7' cy='7' r='6' stroke='#606070' strokeWidth='1.2' />
                  <path
                    d='M4 7 L7 4 L10 7 M7 4 L7 10'
                    stroke='#606070'
                    strokeWidth='1.2'
                    strokeLinecap='round'
                    strokeLinejoin='round'
                  />
                </svg>
                Debugging today
              </div>
              <div className='ba-rows'>
                <div className='ba-row'>
                  <div className='ba-row-icon'>🔍</div>
                  <div className='ba-row-text'>Open Datadog, search logs with no context</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>📊</div>
                  <div className='ba-row-text'>Switch to Grafana, look at 6 dashboards</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>🔎</div>
                  <div className='ba-row-text'>Open Kibana, write a query you half-remember</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>💬</div>
                  <div className='ba-row-text'>Post in Slack: "did anyone see anything weird?"</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>🤔</div>
                  <div className='ba-row-text'>Trace through microservices manually</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>😓</div>
                  <div className='ba-row-text'>Finally understand the root cause</div>
                  <div className='ba-row-time'>45 min</div>
                </div>
              </div>
              <div className='ba-footer'>
                <span>total resolution time</span>
                <span className='ba-footer-val'>~67 minutes</span>
              </div>
            </div>

            {/* AFTER */}
            <div className='ba-card after'>
              <div className='ba-header'>
                <svg viewBox='0 0 14 14' fill='none'>
                  <circle cx='7' cy='7' r='6' stroke='#9d6eff' strokeWidth='1.2' />
                  <path
                    d='M4.5 7 L6.5 9 L9.5 5'
                    stroke='#9d6eff'
                    strokeWidth='1.4'
                    strokeLinecap='round'
                    strokeLinejoin='round'
                  />
                </svg>
                Debugging with Cursr
              </div>
              <div className='ba-rows'>
                <div className='ba-row'>
                  <div className='ba-row-icon'>⚡</div>
                  <div className='ba-row-text'>Open Cursr — incident timeline loads instantly</div>
                  <div className='ba-row-time'>0:08</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>🧠</div>
                  <div className='ba-row-text'>
                    AI summary shows root cause and affected services
                  </div>
                  <div className='ba-row-time'>0:45</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>📍</div>
                  <div className='ba-row-text'>Pinpoint: deploy at 03:44 changed pool size</div>
                  <div className='ba-row-time'>2:10</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>🔧</div>
                  <div className='ba-row-text'>Rollback deploy, monitor recovery</div>
                  <div className='ba-row-time'>4:30</div>
                </div>
                <div className='ba-row'>
                  <div className='ba-row-icon'>✅</div>
                  <div className='ba-row-text'>Incident resolved. Runbook updated.</div>
                  <div className='ba-row-time'>7:15</div>
                </div>
                <div className='ba-row' style={{ opacity: 0.3 }}>
                  <div className='ba-row-icon'>😴</div>
                  <div className='ba-row-text'>Back to sleep</div>
                </div>
              </div>
              <div className='ba-footer'>
                <span>total resolution time</span>
                <span className='ba-footer-val'>~12 minutes</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* HOW IT WORKS */}
      <section className='how-section' id='how'>
        <div className='page'>
          <div className='section-label'>How it works</div>
          <h2 className='section-h2'>
            Three steps from alert
            <br />
            to <em>understanding</em>
          </h2>
          <p className='section-lead'>
            Cursr connects to your existing observability stack. No rip-and-replace.
            No migration weekend. Works with your OTel setup from day one.
          </p>

          <div className='steps-layout'>
            <div className='step'>
              <div className='step-num'>01</div>
              <div className='step-icon'>
                <svg
                  viewBox='0 0 18 18'
                  fill='none'
                  stroke='currentColor'
                  strokeWidth='1.5'
                >
                  <path
                    d='M9 2 L9 6 M14 4 L11.5 6.5 M16 9 L12 9 M14 14 L11.5 11.5 M9 16 L9 12 M4 14 L6.5 11.5 M2 9 L6 9 M4 4 L6.5 6.5'
                    strokeLinecap='round'
                  />
                </svg>
              </div>
              <div className='step-title'>Cursr collects signals</div>
              <div className='step-desc'>
                Connect your existing tools — Prometheus, Loki, OTel, PagerDuty,
                Datadog. Cursr listens to the stream without replacing your existing
                setup.
              </div>
            </div>
            <div className='step'>
              <div className='step-num'>02</div>
              <div className='step-icon'>
                <svg
                  viewBox='0 0 18 18'
                  fill='none'
                  stroke='currentColor'
                  strokeWidth='1.5'
                >
                  <circle cx='4' cy='9' r='2' />
                  <circle cx='14' cy='5' r='2' />
                  <circle cx='14' cy='13' r='2' />
                  <path
                    d='M6 9 L12 5.5 M6 9 L12 12.5'
                    strokeLinecap='round'
                  />
                </svg>
              </div>
              <div className='step-title'>Cursr connects related events</div>
              <div className='step-desc'>
                Deploys, config changes, error spikes, latency shifts — Cursr
                correlates events across services and surfaces the causal chain, not
                just the symptoms.
              </div>
            </div>
            <div className='step'>
              <div className='step-num'>03</div>
              <div className='step-icon'>
                <svg
                  viewBox='0 0 18 18'
                  fill='none'
                  stroke='currentColor'
                  strokeWidth='1.5'
                >
                  <rect x='2' y='4' width='14' height='10' rx='2' />
                  <path
                    d='M5 8 L8 11 L13 7'
                    strokeLinecap='round'
                    strokeLinejoin='round'
                  />
                </svg>
              </div>
              <div className='step-title'>Cursr builds the incident timeline</div>
              <div className='step-desc'>
                You open Cursr and immediately see: what changed, what broke, what's
                affected, and what to do first. The story is already written when
                you arrive.
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* PRODUCT UI SECTION */}
      <section className='product-section' id='product'>
        <div className='page'>
          <div className='section-label'>Product</div>
          <h2 className='section-h2'>
            The interface engineers
            <br />
            <em>actually want to open</em>
          </h2>
          <p className='section-lead'>
            No dashboards built by someone who left the team 6 months ago. No
            queries required to get started. Just the incident, clearly laid out.
          </p>

          <div className='product-ui-wrap'>
            <div className='product-titlebar'>
              <div className='traffic'>
                <div className='tr tr-r'></div>
                <div className='tr tr-y'></div>
                <div className='tr tr-g'></div>
              </div>
              <div className='product-tabs'>
                <div className='product-tab active'>Timeline</div>
                <div className='product-tab'>Logs</div>
                <div className='product-tab'>Metrics</div>
                <div className='product-tab'>Traces</div>
              </div>
            </div>

            <div className='product-layout'>
              {/* SIDEBAR */}
              <div className='product-sidebar'>
                <div className='sidebar-heading'>Recent incidents</div>
                <div className='incident-row active'>
                  <div className='incident-row-dot crit'></div>
                  <div>
                    <div className='incident-row-title'>Checkout service degraded</div>
                    <div className='incident-row-meta'>03:47 · payment-api · active</div>
                  </div>
                </div>
                <div className='incident-row'>
                  <div className='incident-row-dot ok'></div>
                  <div>
                    <div className='incident-row-title'>Search latency spike</div>
                    <div className='incident-row-meta'>yesterday · 22min · resolved</div>
                  </div>
                </div>
                <div className='incident-row'>
                  <div className='incident-row-dot warn'></div>
                  <div>
                    <div className='incident-row-title'>Worker pod OOMKilled</div>
                    <div className='incident-row-meta'>2 days ago · 8min · resolved</div>
                  </div>
                </div>
                <div className='incident-row'>
                  <div className='incident-row-dot ok'></div>
                  <div>
                    <div className='incident-row-title'>DB connection timeout</div>
                    <div className='incident-row-meta'>4 days ago · 14min · resolved</div>
                  </div>
                </div>
                <div className='incident-row'>
                  <div className='incident-row-dot ok'></div>
                  <div>
                    <div className='incident-row-title'>API gateway 503s</div>
                    <div className='incident-row-meta'>1 week ago · 5min · resolved</div>
                  </div>
                </div>
              </div>

              {/* MAIN */}
              <div className='product-main'>
                <div>
                  <div className='product-inc-title'>
                    Checkout service degraded — payment-api
                  </div>
                  <div className='product-inc-meta'>
                    Incident started 03:47 · prod-us-east-1 · 847 requests affected ·
                    duration: 12m
                  </div>
                </div>

                <div className='ai-summary-full'>
                  <div className='ai-summary-label'>
                    <svg width='10' height='10' viewBox='0 0 10 10' fill='none'>
                      <rect
                        x='1'
                        y='1'
                        width='3.5'
                        height='3.5'
                        rx='0.7'
                        fill='currentColor'
                        opacity='0.7'
                      />
                      <rect
                        x='5.5'
                        y='1'
                        width='3.5'
                        height='3.5'
                        rx='0.7'
                        fill='currentColor'
                      />
                      <rect
                        x='1'
                        y='5.5'
                        width='3.5'
                        height='3.5'
                        rx='0.7'
                        fill='currentColor'
                      />
                      <rect
                        x='5.5'
                        y='5.5'
                        width='3.5'
                        height='3.5'
                        rx='0.7'
                        fill='currentColor'
                        opacity='0.7'
                      />
                    </svg>
                    Cursr AI · root cause analysis
                  </div>
                  <div className='ai-summary-text'>
                    Deploy{' '}
                    <code
                      style={{
                        background: 'rgba(255,255,255,0.07)',
                        padding: '1px 5px',
                        borderRadius: '3px',
                        fontSize: '11px',
                      }}
                    >
                      payment-api v2.3.1
                    </code>{' '}
                    at 03:44 reduced{' '}
                    <code
                      style={{
                        background: 'rgba(255,255,255,0.07)',
                        padding: '1px 5px',
                        borderRadius: '3px',
                        fontSize: '11px',
                      }}
                    >
                      connection_pool_size
                    </code>{' '}
                    from 10 to 5. Under normal load this would be fine — but a batch
                    job running on Tuesday nights at 03:45 creates a burst of 8
                    simultaneous connections. Pool exhaustion caused upstream timeout
                    in checkout-api.{' '}
                    <strong style={{ color: '#c4b5fd' }}>
                      Rollback payment-api to v2.3.0 to resolve.
                    </strong>
                  </div>
                </div>

                <div className='product-metrics'>
                  <div className='prod-metric'>
                    <div className='prod-metric-label'>error rate</div>
                    <div className='prod-metric-val r'>8.4%</div>
                    <div className='fake-chart'>
                      <div className='fc' style={{ height: '15%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '12%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '18%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '14%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '16%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '15%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '80%', background: '#ef4444', opacity: 0.7 }}></div>
                      <div className='fc' style={{ height: '90%', background: '#ef4444', opacity: 0.8 }}></div>
                      <div className='fc' style={{ height: '85%', background: '#ef4444', opacity: 0.7 }}></div>
                    </div>
                  </div>
                  <div className='prod-metric'>
                    <div className='prod-metric-label'>p99 latency</div>
                    <div className='prod-metric-val a'>4.2s</div>
                    <div className='fake-chart'>
                      <div className='fc' style={{ height: '10%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '12%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '10%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '11%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '12%', background: 'var(--border1)' }}></div>
                      <div className='fc' style={{ height: '60%', background: '#f59e0b', opacity: 0.7 }}></div>
                      <div className='fc' style={{ height: '90%', background: '#f59e0b', opacity: 0.8 }}></div>
                      <div className='fc' style={{ height: '95%', background: '#f59e0b', opacity: 0.8 }}></div>
                      <div className='fc' style={{ height: '88%', background: '#f59e0b', opacity: 0.7 }}></div>
                    </div>
                  </div>
                  <div className='prod-metric'>
                    <div className='prod-metric-label'>checkout RPM</div>
                    <div className='prod-metric-val g'>847</div>
                    <div className='fake-chart'>
                      <div className='fc' style={{ height: '70%', background: 'var(--border2)' }}></div>
                      <div className='fc' style={{ height: '75%', background: 'var(--border2)' }}></div>
                      <div className='fc' style={{ height: '72%', background: 'var(--border2)' }}></div>
                      <div className='fc' style={{ height: '74%', background: 'var(--border2)' }}></div>
                      <div className='fc' style={{ height: '76%', background: 'var(--border2)' }}></div>
                      <div className='fc' style={{ height: '20%', background: '#4ade80', opacity: 0.5 }}></div>
                      <div className='fc' style={{ height: '15%', background: '#4ade80', opacity: 0.5 }}></div>
                      <div className='fc' style={{ height: '18%', background: '#4ade80', opacity: 0.5 }}></div>
                      <div className='fc' style={{ height: '16%', background: '#4ade80', opacity: 0.5 }}></div>
                    </div>
                  </div>
                </div>

                <div>
                  <div
                    style={{
                      fontSize: '11px',
                      fontFamily: 'var(--font-mono)',
                      color: 'var(--text2)',
                      textTransform: 'uppercase',
                      letterSpacing: '0.07em',
                      marginBottom: '10px',
                    }}
                  >
                    Incident timeline
                  </div>
                  <div className='timeline-full'>
                    <div className='tl-full-item'>
                      <div className='tl-full-dot info'></div>
                      <div className='tl-full-body'>
                        <div className='tl-full-event'>Deploy: payment-api v2.3.1</div>
                        <div className='tl-full-time'>
                          03:44:12 — triggered by CI pipeline · commit 3f7a1b
                        </div>
                        <div className='tl-full-tag info'>config change</div>
                      </div>
                    </div>
                    <div className='tl-full-item'>
                      <div className='tl-full-dot warn'></div>
                      <div className='tl-full-body'>
                        <div className='tl-full-event'>
                          Batch job started: nightly-invoice-reconciliation
                        </div>
                        <div className='tl-full-time'>
                          03:45:00 — scheduled · 8 DB connections opened
                        </div>
                      </div>
                    </div>
                    <div className='tl-full-item'>
                      <div className='tl-full-dot crit'></div>
                      <div className='tl-full-body'>
                        <div className='tl-full-event'>
                          Connection pool exhausted — payment-api
                        </div>
                        <div className='tl-full-time'>
                          03:45:34 — pool_size=5, connections=5, waiting=3
                        </div>
                        <div className='tl-full-tag crit'>root cause</div>
                      </div>
                    </div>
                    <div className='tl-full-item'>
                      <div className='tl-full-dot crit'></div>
                      <div className='tl-full-body'>
                        <div className='tl-full-event'>
                          checkout-api upstream timeout cascade begins
                        </div>
                        <div className='tl-full-time'>
                          03:46:01 — p99 latency: 120ms → 4.2s
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* FEATURE GRID */}
      <section className='features-section' id='features'>
        <div className='page'>
          <div className='section-label'>What you get</div>
          <h2 className='section-h2'>
            What you see in the
            <br />
            <em>first 5 minutes</em>
          </h2>
          <p className='section-lead'>
            Open Cursr during an incident. Here's everything that's already waiting
            for you.
          </p>

          <div className='feature-grid'>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <circle cx='8' cy='8' r='6' />
                    <path
                      d='M8 4 L8 8 L11 10'
                      strokeLinecap='round'
                      strokeLinejoin='round'
                    />
                  </svg>
                </div>
                <div className='feature-name'>Incident Timeline</div>
              </div>
              <div className='feature-desc'>
                A chronological reconstruction of everything that changed — deploys,
                config diffs, alerts, spikes — stitched together automatically.
              </div>
              <div className='feature-tag'>See what changed</div>
            </div>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <path d='M8 2 L14 12 L2 12 Z' strokeLinejoin='round' />
                    <path d='M8 7 L8 9' strokeLinecap='round' />
                    <circle cx='8' cy='11' r='0.5' fill='currentColor' />
                  </svg>
                </div>
                <div className='feature-name'>AI Root Cause</div>
              </div>
              <div className='feature-desc'>
                Cursr's AI reads your signals and surfaces the most likely cause in
                plain language. Not a dashboard. An answer.
              </div>
              <div className='feature-tag'>Likely cause</div>
            </div>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <rect x='1' y='4' width='6' height='8' rx='1.2' />
                    <rect x='9' y='2' width='6' height='5' rx='1.2' />
                    <rect x='9' y='9' width='6' height='5' rx='1.2' />
                    <path d='M7 8 L9 8' strokeLinecap='round' />
                  </svg>
                </div>
                <div className='feature-name'>Affected Services</div>
              </div>
              <div className='feature-desc'>
                A live blast radius map. Which services are impacted, which are
                degraded, and which are upstream of the failure.
              </div>
              <div className='feature-tag'>Blast radius</div>
            </div>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <path
                      d='M2 12 L6 6 L10 9 L14 4'
                      strokeLinecap='round'
                      strokeLinejoin='round'
                    />
                  </svg>
                </div>
                <div className='feature-name'>Error &amp; Latency</div>
              </div>
              <div className='feature-desc'>
                Error rate, p50/p99 latency, and throughput — correlated with the
                incident start time so you can see impact at a glance.
              </div>
              <div className='feature-tag'>Impact</div>
            </div>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <path
                      d='M3 8 L6 11 L13 4'
                      strokeLinecap='round'
                      strokeLinejoin='round'
                    />
                  </svg>
                </div>
                <div className='feature-name'>Deploys &amp; Config</div>
              </div>
              <div className='feature-desc'>
                Every deploy, config change, and feature flag flip that happened in
                the window leading up to the incident — with diffs.
              </div>
              <div className='feature-tag'>What changed</div>
            </div>
            <div className='feature-card'>
              <div className='feature-icon-row'>
                <div className='feature-icon'>
                  <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                    <path d='M2 4 L14 4 M5 8 L11 8 M7 12 L9 12' strokeLinecap='round' />
                  </svg>
                </div>
                <div className='feature-name'>Suggested Actions</div>
              </div>
              <div className='feature-desc'>
                Concrete next steps: rollback candidate, config to revert, which team
                to loop in. Actionable, not academic.
              </div>
              <div className='feature-tag'>What to do</div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* INTEGRATIONS */}
      <section className='integrations-section' id='integrations'>
        <div className='page'>
          <div className='section-label'>Integrations</div>
          <h2 className='section-h2'>
            Works with your
            <br />
            <em>existing stack</em>
          </h2>

          <div className='integrations-inner'>
            <div className='integrations-card'>
              <div className='integrations-header'>
                <p className='integrations-lede'>
                  No rip-and-replace. Cursr connects to the tools you're already
                  running. If you have OTel, Prometheus, or Datadog, you're set up
                  in under 5 minutes.
                </p>
                <div className='integrations-badge'>✓ No agent required</div>
              </div>
              <div className='integrations-grid'>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={prometheusLogoUrl} alt='Prometheus' />
                  </div>
                  <div className='integration-name'>Prometheus</div>
                  <div className='integration-type'>Metrics</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={grafanaLogoUrl} alt='Grafana' />
                  </div>
                  <div className='integration-name'>Grafana</div>
                  <div className='integration-type'>Dashboards</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={lokiLogoUrl} alt='Loki' />
                  </div>
                  <div className='integration-name'>Loki</div>
                  <div className='integration-type'>Logs</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={openTelemetryLogoUrl} alt='OpenTelemetry' />
                  </div>
                  <div className='integration-name'>OpenTelemetry</div>
                  <div className='integration-type'>Traces &amp; Metrics</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={datadogLogoUrl} alt='Datadog' />
                  </div>
                  <div className='integration-name'>Datadog</div>
                  <div className='integration-type'>APM &amp; Logs</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={pagerDutyLogoUrl} alt='PagerDuty' />
                  </div>
                  <div className='integration-name'>PagerDuty</div>
                  <div className='integration-type'>Alerting</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={slackLogoUrl} alt='Slack' />
                  </div>
                  <div className='integration-name'>Slack</div>
                  <div className='integration-type'>Notifications</div>
                </div>
                <div className='integration-item'>
                  <div className='integration-logo'>
                    <img src={kubernetesLogoUrl} alt='Kubernetes' />
                  </div>
                  <div className='integration-name'>Kubernetes</div>
                  <div className='integration-type'>Orchestration</div>
                </div>
              </div>
              <div className='integrations-note'>
                More integrations shipping soon.{' '}
                <strong>
                  AWS CloudWatch · GCP Operations · New Relic · Honeycomb · Sentry
                </strong>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* TESTIMONIALS */}
      <section className='testimonials-section' id='testimonials'>
        <div className='page'>
          <div className='section-label'>From the trenches</div>
          <h2 className='section-h2'>
            Engineers who've been
            <br />
            <em>paged at 3am</em>
          </h2>

          <div className='testimonials-grid'>
            <div className='testimonial-card'>
              <div className='testimonial-stars'>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
              </div>
              <p className='testimonial-quote'>
                "We had a cascade failure that took down checkout for 40 minutes. We
                were three Slack threads deep, five dashboards open.{' '}
                <strong>
                  Cursr opened and just told us it was a connection pool change in
                  the last deploy.
                </strong>{' '}
                We rolled it back in four minutes."
              </p>
              <div className='testimonial-author'>
                <div className='testimonial-avatar' style={{ background: '#7c3aed' }}>
                  S
                </div>
                <div>
                  <div className='testimonial-name'>Shreya K.</div>
                  <div className='testimonial-role'>
                    Staff Engineer · Fintech startup · Series B
                  </div>
                </div>
              </div>
            </div>

            <div className='testimonial-card'>
              <div className='testimonial-stars'>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
              </div>
              <p className='testimonial-quote'>
                "I was on-call for a service I'd never touched before.{' '}
                <strong>
                  Cursr's root cause summary was so clear I didn't need to ping the
                  original author.
                </strong>{' '}
                I escalated the right thing to the right team immediately. That
                alone is worth it."
              </p>
              <div className='testimonial-author'>
                <div className='testimonial-avatar' style={{ background: '#2563eb' }}>
                  M
                </div>
                <div>
                  <div className='testimonial-name'>Marcus T.</div>
                  <div className='testimonial-role'>
                    Backend Engineer · SaaS platform · 60 engineers
                  </div>
                </div>
              </div>
            </div>

            <div className='testimonial-card'>
              <div className='testimonial-stars'>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
                <span>★</span>
              </div>
              <p className='testimonial-quote'>
                "We had Datadog, Grafana, and PagerDuty. We weren't missing data —
                we were missing <strong>context.</strong> Cursr is the first tool
                that connects it into a story I can actually act on without a
                20-minute archaeology dig."
              </p>
              <div className='testimonial-author'>
                <div className='testimonial-avatar' style={{ background: '#059669' }}>
                  R
                </div>
                <div>
                  <div className='testimonial-name'>Rafi O.</div>
                  <div className='testimonial-role'>
                    Engineering Manager · E-commerce · 120 engineers
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* WHO IT'S FOR */}
      <section className='for-section'>
        <div className='page'>
          <div className='section-label'>Who it's for</div>
          <h2 className='section-h2'>
            Built for teams running
            <br />
            <em>real production systems</em>
          </h2>
          <p className='section-lead'>
            You have microservices. You have an on-call rotation. You've set up
            Grafana and it works — until it doesn't.
          </p>

          <div className='for-grid'>
            <div className='for-card'>
              <div className='for-icon'>
                <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                  <rect x='1' y='4' width='6' height='8' rx='1.2' />
                  <rect x='9' y='2' width='6' height='5' rx='1.2' />
                  <rect x='9' y='9' width='6' height='5' rx='1.2' />
                  <path d='M7 8 L9 8' strokeLinecap='round' />
                </svg>
              </div>
              <div className='for-title'>Teams running microservices</div>
              <div className='for-desc'>
                When a request touches 12 services, "check the logs" doesn't help.
                Cursr traces the path automatically and shows you where it broke.
              </div>
            </div>
            <div className='for-card'>
              <div className='for-icon'>
                <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                  <path
                    d='M2 12 L6 6 L10 9 L14 4'
                    strokeLinecap='round'
                    strokeLinejoin='round'
                  />
                </svg>
              </div>
              <div className='for-title'>Teams currently using Grafana</div>
              <div className='for-desc'>
                Grafana shows you what you configured. Cursr shows you what happened.
                If your dashboards are only readable by the person who built them,
                you're ready for Cursr.
              </div>
            </div>
            <div className='for-card'>
              <div className='for-icon'>
                <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                  <circle cx='8' cy='6' r='3' />
                  <path
                    d='M2.5 14 C2.5 11.5 5 9.5 8 9.5 C11 9.5 13.5 11.5 13.5 14'
                    strokeLinecap='round'
                  />
                </svg>
              </div>
              <div className='for-title'>Teams with on-call rotations</div>
              <div className='for-desc'>
                65% of engineers report burnout. On-call is a leading cause. Cursr
                is designed for the person who gets paged — not the person who set
                up the monitoring.
              </div>
            </div>
            <div className='for-card'>
              <div className='for-icon'>
                <svg viewBox='0 0 16 16' fill='none' stroke='currentColor' strokeWidth='1.4'>
                  <path
                    d='M8 2 L10 6 L14.5 6.5 L11 10 L12 14.5 L8 12.5 L4 14.5 L5 10 L1.5 6.5 L6 6 Z'
                    strokeLinejoin='round'
                  />
                </svg>
              </div>
              <div className='for-title'>Startups and mid-size companies</div>
              <div className='for-desc'>
                Too big for "just check the logs." Not big enough to justify a $500K
                Datadog bill. Cursr is built for 10–150 engineer teams who need power
                without the price tag.
              </div>
            </div>
          </div>

          <div className='not-for'>
            <div>
              <div className='not-for-label'>Not for (yet)</div>
              <div className='not-for-text'>
                If you need enterprise SIEM, compliance-grade audit logging, or
                military-grade security tooling — we're not there yet. We'll tell
                you that instead of pretending. Cursr is the honest choice for teams
                that need incident clarity, not enterprise checkbox compliance.
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* GRAFANA GRADUATION */}
      <section className='grafana-section'>
        <div className='page'>
          <div className='section-label'>The Grafana graduation</div>
          <h2 className='section-h2'>
            Many teams start with Grafana.
            <br />
            <em>Most hit a ceiling.</em>
          </h2>

          <div className='grafana-card'>
            <div className='grafana-top'>
              <p
                style={{
                  fontSize: '15px',
                  color: 'var(--text1)',
                  lineHeight: 1.7,
                  maxWidth: '640px',
                }}
              >
                Grafana is a great tool. It helped your team get visibility when you
                had no visibility. But there's a moment — you know the one — when
                the on-call engineer opens their laptop during an incident and
                stares at 12 dashboards built by someone who left the team 8 months
                ago.
              </p>
            </div>
            <div className='grafana-bottom'>
              <div className='grafana-col'>
                <div className='grafana-col-label'>
                  <svg width='12' height='12' viewBox='0 0 12 12' fill='none'>
                    <path
                      d='M6 2 L6 6 L9 9'
                      stroke='currentColor'
                      strokeWidth='1.4'
                      strokeLinecap='round'
                    />
                    <circle
                      cx='6'
                      cy='6'
                      r='5'
                      stroke='currentColor'
                      strokeWidth='1.2'
                    />
                  </svg>
                  Grafana is good at
                </div>
                <div className='grafana-point'>
                  Visualizing metrics you've already decided to track
                </div>
                <div className='grafana-point'>
                  Showing historical data in configurable dashboards
                </div>
                <div className='grafana-point'>
                  Working with Prometheus and Loki out of the box
                </div>
                <div className='grafana-point'>Being free and open-source</div>
              </div>
              <div className='grafana-col'>
                <div className='grafana-col-label'>
                  <svg width='12' height='12' viewBox='0 0 12 12' fill='none'>
                    <path
                      d='M2.5 9.5 L9.5 2.5 M2.5 2.5 L9.5 9.5'
                      stroke='currentColor'
                      strokeWidth='1.4'
                      strokeLinecap='round'
                    />
                  </svg>
                  Where Grafana stops working
                </div>
                <div className='grafana-point'>
                  Incidents that cross multiple dashboards and services
                </div>
                <div className='grafana-point'>
                  Engineers on-call for services they didn't build
                </div>
                <div className='grafana-point'>
                  Answering "what changed?" not "what is the value?"
                </div>
                <div className='grafana-point'>
                  Correlating a deploy with a latency spike automatically
                </div>
              </div>
            </div>
          </div>

          <div style={{ textAlign: 'center', marginTop: '2rem' }}>
            <p style={{ fontSize: '14px', color: 'var(--text1)', marginBottom: '1.25rem' }}>
              Cursr works alongside Grafana. Your existing dashboards keep working.{' '}
              <strong style={{ color: 'var(--text0)' }}>
                You add incident comprehension on top.
              </strong>
            </p>
            <button className='btn btn-ghost btn-lg'>
              Read the Grafana migration guide →
            </button>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* FAQ */}
      <section className='faq-section' id='faq'>
        <div className='page'>
          <div className='section-label'>Common questions</div>
          <h2 className='section-h2'>
            Things teams ask
            <br />
            <em>before they sign up</em>
          </h2>

          <div className='faq-grid'>
            <div className='faq-item'>
              <div className='faq-q'>Do I have to replace Grafana or Datadog?</div>
              <div className='faq-a'>
                No. Cursr works <strong>alongside</strong> your existing stack. It
                reads from Prometheus, Grafana, Loki, Datadog, and OTel — it doesn't
                replace any of them. Your dashboards keep working exactly as they do
                today.
              </div>
            </div>

            <div className='faq-item'>
              <div className='faq-q'>How long does setup actually take?</div>
              <div className='faq-a'>
                If you have OTel or Prometheus running, setup is under 5 minutes.
                Point Cursr at your data sources, and your first incident timeline is
                generated automatically. <strong>No agent to install.</strong>
              </div>
            </div>

            <div className='faq-item'>
              <div className='faq-q'>Does Cursr store my logs and metrics?</div>
              <div className='faq-a'>
                Cursr reads from your existing sources — it doesn't become your log
                store. <strong>Your data stays where it is.</strong> We only store
                incident metadata and the reconstructed timeline, not raw log
                contents.
              </div>
            </div>

            <div className='faq-item'>
              <div className='faq-q'>
                What if my stack isn't listed in the integrations?
              </div>
              <div className='faq-a'>
                If you're sending to OTel, you're already compatible. We're shipping
                new native integrations monthly. <strong>Tell us what you're running</strong>{' '}
                — early teams directly influence our integration roadmap.
              </div>
            </div>

            <div className='faq-item'>
              <div className='faq-q'>Is this useful for teams under 10 engineers?</div>
              <div className='faq-a'>
                Yes, especially if everyone is on-call for everything. Smaller teams
                often feel incident pain more acutely. The Free plan is designed
                specifically <strong>for small teams who can't afford downtime</strong>{' '}
                but can't afford Datadog either.
              </div>
            </div>

            <div className='faq-item'>
              <div className='faq-q'>How accurate is the AI root cause?</div>
              <div className='faq-a'>
                Cursr's AI identifies the correct root cause in the majority of
                incidents involving deploys, config changes, or resource exhaustion.
                For novel failure modes, it surfaces the most relevant signals and
                lets you investigate from there. It <strong>never hides data</strong>{' '}
                — you can always drill into the raw timeline.
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* PRICING */}
      <section className='pricing-section' id='pricing'>
        <div className='page'>
          <div className='section-label'>Pricing</div>
          <h2 className='section-h2'>
            Simple pricing,
            <br />
            <em>no surprises</em>
          </h2>
          <p className='section-lead'>
            Start free. Scale when you need it. No per-seat surprises, no hidden
            data ingestion fees.
          </p>

          <div className='pricing-grid'>
            <div className='pricing-card'>
              <div className='pricing-plan'>Free</div>
              <div className='pricing-for'>For small teams getting started</div>
              <div className='pricing-price'>
                $0<span>/mo</span>
              </div>
              <div className='pricing-period'>forever free</div>
              <div className='pricing-divider'></div>
              <ul className='pricing-features'>
                <li>Up to 3 services</li>
                <li>7-day incident history</li>
                <li>1 integration</li>
                <li>AI root cause (5/month)</li>
                <li>Community support</li>
              </ul>
              <button className='pricing-cta'>Get started free</button>
            </div>

            <div className='pricing-card featured'>
              <div className='pricing-featured-badge'>MOST POPULAR</div>
              <div className='pricing-plan'>Startup</div>
              <div className='pricing-for'>For growing engineering teams</div>
              <div className='pricing-price'>
                $49<span>/mo</span>
              </div>
              <div className='pricing-period'>billed monthly · cancel anytime</div>
              <div className='pricing-divider'></div>
              <ul className='pricing-features'>
                <li>Up to 20 services</li>
                <li>90-day incident history</li>
                <li>All integrations</li>
                <li>Unlimited AI root cause</li>
                <li>Slack notifications</li>
                <li>Email support</li>
              </ul>
              <button className='pricing-cta'>Start free trial</button>
            </div>

            <div className='pricing-card'>
              <div className='pricing-plan'>Growth</div>
              <div className='pricing-for'>For scaling systems &amp; larger orgs</div>
              <div className='pricing-price'>
                $199<span>/mo</span>
              </div>
              <div className='pricing-period'>billed monthly</div>
              <div className='pricing-divider'></div>
              <ul className='pricing-features'>
                <li>Unlimited services</li>
                <li>1-year incident history</li>
                <li>All integrations + webhooks</li>
                <li>Post-mortem automation</li>
                <li>Custom alert routing</li>
                <li>Priority support</li>
              </ul>
              <button className='pricing-cta'>Start free trial</button>
            </div>

            <div className='pricing-card'>
              <div className='pricing-plan'>Enterprise</div>
              <div className='pricing-for'>For large orgs with complex needs</div>
              <div className='pricing-price' style={{ fontSize: '20px', paddingTop: '4px' }}>
                Custom
              </div>
              <div className='pricing-period'>annual contract</div>
              <div className='pricing-divider'></div>
              <ul className='pricing-features'>
                <li>Everything in Growth</li>
                <li>SSO / SAML</li>
                <li>Audit logs &amp; compliance</li>
                <li>Custom data retention</li>
                <li>Dedicated Slack channel</li>
                <li>SLA &amp; uptime guarantee</li>
              </ul>
              <button className='pricing-cta'>Talk to sales</button>
            </div>
          </div>

          <p className='pricing-note'>
            All plans include a <strong>14-day free trial</strong>. No credit card
            required to start.
          </p>
        </div>
      </section>

      <div className='section-divider'></div>

      {/* CTA */}
      <section className='cta-section'>
        <div className='cta-glow'></div>
        <div
          className='grid-bg'
          style={{
            opacity: 0.4,
            maskImage:
              'radial-gradient(ellipse 50% 80% at 50% 50%, black, transparent)',
          }}
        ></div>
        <div className='page' style={{ position: 'relative', zIndex: 1 }}>
          <h2 className='cta-h2'>
            Understand incidents
            <br />
            in <em>minutes, not hours</em>
          </h2>
          <p className='cta-sub'>
            Connect your existing OTel setup. No credit card required.
            <br />
            Your first incident timeline in under 5 minutes.
          </p>
          <div className='cta-actions'>
            <button className='btn btn-primary btn-xl'>Get started free</button>
            <button className='btn btn-ghost btn-xl'>Read the docs</button>
          </div>
          <div className='cta-note'>
            No credit card required
            <span>·</span>
            Works with your existing OTel setup
            <span>·</span>
            Grafana still works alongside it
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer>
        <div className='footer-grid'>
          <div>
            <a href='#' className='nav-logo footer-logo'>
              <img src={appLogoUrl} alt='Cursr' className='app-logo' />
            </a>
            <p className='footer-brand-desc'>
              Incident intelligence for engineering teams. Know what broke and why in
              minutes, not hours.
            </p>
          </div>

          <div>
            <div className='footer-col-label'>Product</div>
            <ul className='footer-col-links'>
              <li>
                <a href='#features'>Features</a>
              </li>
              <li>
                <a href='#integrations'>Integrations</a>
              </li>
              <li>
                <a href='#how'>How it works</a>
              </li>
              <li>
                <a href='#pricing'>Pricing</a>
              </li>
              <li>
                <a href='#'>Changelog</a>
              </li>
            </ul>
          </div>

          <div>
            <div className='footer-col-label'>Resources</div>
            <ul className='footer-col-links'>
              <li>
                <a href='#' id='docs'>
                  Docs
                </a>
              </li>
              <li>
                <a href='#'>GitHub</a>
              </li>
              <li>
                <a href='#'>Blog</a>
              </li>
              <li>
                <a href='#'>Status</a>
              </li>
              <li>
                <a href='#'>Grafana guide</a>
              </li>
            </ul>
          </div>

          <div>
            <div className='footer-col-label'>Company</div>
            <ul className='footer-col-links'>
              <li>
                <a href='#'>About</a>
              </li>
              <li>
                <a href='#'>Contact</a>
              </li>
              <li>
                <a href='#'>Careers</a>
              </li>
              <li>
                <a href='#'>Security</a>
              </li>
            </ul>
          </div>
        </div>
        <div className='footer-bottom'>
          <div className='footer-bottom-copy'>
            © 2026 Cursr · Incident Intelligence Platform
          </div>
          <ul className='footer-bottom-links'>
            <li>
              <a href='#'>Privacy</a>
            </li>
            <li>
              <a href='#'>Terms</a>
            </li>
            <li>
              <a href='#'>Cookie Policy</a>
            </li>
          </ul>
        </div>
      </footer>
    </>
  );
}

