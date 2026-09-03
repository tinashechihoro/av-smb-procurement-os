import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { dashboardApi } from '../api/services';

export default function Dashboard() {
  const { user, orgType } = useAuthStore();
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.stats().then(r => { setStats(r.data); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  if (loading || !stats) return <div style={{ padding: 40, textAlign: 'center', color: '#111' }}>Loading dashboard…</div>;

  const fmt = (n: any) => typeof n === 'number' ? n.toLocaleString() : n;
  const money = (n: any) => '$' + (typeof n === 'number' ? n.toLocaleString() : '0');
  const isAv = orgType === 'BUYER';

  const hero = isAv ? {
    title: 'Every vehicle. Every part. One accountable flow.',
    sub: 'Control requisitions, negotiations, approvals, outstanding parts and supplier invoices from one vehicle-centred command centre.',
    metrics: [['Open vehicle jobs', fmt(stats.activeJobs)], ['Parts outstanding', fmt(stats.pendingRequisitions)], ['Procurement value', money(stats.totalJobEstimated)]]
  } : {
    title: 'Turn every AV request into a profitable, traceable delivery.',
    sub: 'Quote faster, source smarter, manage supplier exposure and protect margin with a single operational and financial workspace.',
    metrics: [['Orders to source', fmt(stats.activeOrders)], ['Inventory value', money(stats.inventoryValue)], ['Outstanding debtors', money(stats.totalInvoiceValue - stats.totalPaid)]]
  };

  const kpis = isAv ? [
    ['Requisitions', fmt(stats.requisitions), '≡', `${fmt(stats.draftRequisitions)} draft`, 'var(--info)'],
    ['Quotations', fmt(stats.quotations), '◫', `${fmt(stats.pendingQuotations)} pending`, 'var(--av)'],
    ['Orders', fmt(stats.orders), '▣', `${fmt(stats.activeOrders)} active`, 'var(--success)'],
    ['Invoice Balance', money(stats.totalInvoiceValue - stats.totalPaid), '▤', `${fmt(stats.invoices)} total`, 'var(--warning)'],
  ] : [
    ['New Requisitions', fmt(stats.pendingRequisitions), '◈', 'From AV', 'var(--info)'],
    ['Inventory Items', fmt(stats.inventoryItems), '▦', `${fmt(stats.lowStockItems)} low stock`, 'var(--av)'],
    ['Deliveries', fmt(stats.deliveries), '⇢', 'Pending', 'var(--success)'],
    ['Cashbook Entries', fmt(stats.cashbookEntries), '↕', `${fmt(stats.journals)} journals`, 'var(--warning)'],
  ];

  const statusColor = (s: string) => {
    const v = s.toLowerCase();
    if (v.includes('approved') || v.includes('complete') || v.includes('paid') || v.includes('posted')) return 'success';
    if (v.includes('overdue') || v.includes('rejected') || v.includes('cancel')) return 'danger';
    if (v.includes('partial') || v.includes('negotiation') || v.includes('draft')) return 'warning';
    if (v.includes('await') || v.includes('review') || v.includes('sourcing') || v.includes('submitted')) return 'info';
    return 'neutral';
  };

  return (
    <div>
      {/* Hero Strip */}
      <div className="hero-strip">
        <div className="hero-main">
          <div className="eyebrow">LIVE OPERATING VIEW</div>
          <h2>{hero.title}</h2>
          <p>{hero.sub}</p>
          <div className="hero-actions">
            <button className="primary-btn">{isAv ? 'Create requisition' : 'Open requisition inbox'}</button>
            <button className="secondary-btn">{isAv ? 'View vehicle costs' : 'Review profitability'}</button>
          </div>
        </div>
        <div className="hero-side">
          {hero.metrics.map(([label, value], i) => (
            <div className="hero-metric" key={i}>
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </div>

      {/* KPI Grid */}
      <div className="kpi-grid">
        {kpis.map(([label, value, icon, foot, accent], i) => (
          <div className="kpi-card" key={i} style={{ '--kpi-accent': accent } as any}>
            <div className="kpi-top">
              <span className="kpi-label">{label}</span>
              <span className="kpi-icon">{icon}</span>
            </div>
            <div className="kpi-value">{value}</div>
            <div className="kpi-foot">● {foot}</div>
          </div>
        ))}
      </div>

      {/* Workflow Pipeline */}
      <div className="panel" style={{ marginBottom: 14 }}>
        <div className="panel-head">
          <div><h3>Procurement Workflow</h3><p>End-to-end pipeline status</p></div>
        </div>
        <div className="panel-body">
          <div className="workflow">
            {[
              ['Requisitions', stats.requisitions, stats.draftRequisitions],
              ['Review', stats.pendingRequisitions, 0],
              ['Quotations', stats.quotations, stats.pendingQuotations],
              ['Orders', stats.orders, stats.activeOrders],
              ['Deliveries', stats.deliveries, 0],
              ['Invoices', stats.invoices, 0],
            ].map(([label, count, sub]: any, i: number) => (
              <div key={label} className={`workflow-step ${count > 0 ? 'active' : ''}`}>
                <div className="step-num">{i + 1}</div>
                <strong>{label}</strong>
                <span>{fmt(count)} total · {fmt(sub)} open</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Status Breakdowns */}
      <div className="grid-2">
        <div className="panel">
          <div className="panel-head"><div><h3>Repair Job Status</h3><p>Current workshop pipeline</p></div></div>
          <div className="panel-body">
            {Array.isArray(stats.jobStatusBreakdown) && stats.jobStatusBreakdown.length > 0 ? (
              stats.jobStatusBreakdown.map(([s, c]: any, i: number) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--line)' }}>
                  <span className={`status ${statusColor(s)}`}>{s}</span>
                  <strong style={{ fontSize: 13 }}>{c}</strong>
                </div>
              ))
            ) : <div style={{ padding: 20, textAlign: 'center', color: '#111' }}>No jobs yet</div>}
          </div>
        </div>
        <div className="panel">
          <div className="panel-head"><div><h3>Invoice Status</h3><p>Billing overview</p></div></div>
          <div className="panel-body">
            {Array.isArray(stats.invoiceStatusBreakdown) && stats.invoiceStatusBreakdown.length > 0 ? (
              stats.invoiceStatusBreakdown.map(([s, c]: any, i: number) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--line)' }}>
                  <span className={`status ${statusColor(s)}`}>{s}</span>
                  <strong style={{ fontSize: 13 }}>{c}</strong>
                </div>
              ))
            ) : <div style={{ padding: 20, textAlign: 'center', color: '#111' }}>No invoices yet</div>}
          </div>
        </div>
      </div>

      {/* Financial Summary */}
      {isAv && (
        <div className="panel" style={{ marginBottom: 14 }}>
          <div className="panel-head"><div><h3>Financial Summary</h3><p>Cost exposure and payment tracking</p></div></div>
          <div className="panel-body">
            <div className="finance-cards">
              <div className="finance-card"><span>Total Estimated</span><strong>{money(stats.totalJobEstimated)}</strong><em>Repair jobs</em></div>
              <div className="finance-card"><span>Total Actual</span><strong>{money(stats.totalJobActual)}</strong><em>Current spend</em></div>
              <div className="finance-card"><span>Invoiced</span><strong>{money(stats.totalInvoiceValue)}</strong><em>{fmt(stats.invoices)} invoices</em></div>
              <div className="finance-card"><span>Outstanding</span><strong style={{ color: 'var(--danger)' }}>{money(stats.totalInvoiceValue - stats.totalPaid)}</strong><em>Balance due</em></div>
            </div>
          </div>
        </div>
      )}

      {/* Recent Activity */}
      <div className="panel">
        <div className="panel-head"><div><h3>Recent Activity</h3><p>Latest system events</p></div></div>
        <div className="panel-body">
          {Array.isArray(stats.recentAuditEvents) && stats.recentAuditEvents.length > 0 ? (
            <div className="activity-list">
              {stats.recentAuditEvents.slice(0, 8).map((e: any, i: number) => (
                <div className="activity" key={i}>
                  <div className="activity-icon">◎</div>
                  <div className="activity-copy">
                    <strong>{e[1]}</strong>
                    <span>{e[2]} · {e[3]?.substring(0, 8)}…</span>
                  </div>
                  <div className="activity-time">{new Date(e[3]).toLocaleDateString()}</div>
                </div>
              ))}
            </div>
          ) : <div style={{ padding: 20, textAlign: 'center', color: '#111' }}>No activity recorded yet. Create some records to see the audit trail.</div>}
        </div>
      </div>
    </div>
  );
}
