import { useEffect, useState } from 'react';
import { reportApi } from '../../api/services';

export default function Reports() {
  const [activeReport, setActiveReport] = useState<string | null>(null);
  const [reportData, setReportData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  const reports = [
    { id: 'trial-balance', name: 'Trial Balance', group: 'Financial', desc: 'All account balances with debit/credit totals and balance verification.', endpoint: 'trialBalance' },
    { id: 'debtor-ageing', name: 'Debtor Ageing', group: 'Financial', desc: 'Outstanding receivables by age bucket with days outstanding.', endpoint: 'debtorAgeing' },
    { id: 'job-cost', name: 'Vehicle Job Cost', group: 'Operational', desc: 'Cost exposure per repair job with estimated vs actual variance.', endpoint: 'jobCost' },
    { id: 'inventory-valuation', name: 'Inventory Valuation', group: 'Operational', desc: 'Stock on hand at cost value with reorder level alerts.', endpoint: 'inventoryValuation' },
    { id: 'procurement-pipeline', name: 'Procurement Pipeline', group: 'Management', desc: 'Requisition → quotation → order status summary.', endpoint: 'procurementPipeline' },
  ];

  const csvExports = [
    { name: 'Invoices', url: '/api/reports/csv/invoices' },
    { name: 'Orders', url: '/api/reports/csv/orders' },
    { name: 'Inventory', url: '/api/reports/csv/inventory' },
    { name: 'Audit Trail', url: '/api/reports/csv/audit' },
  ];

  const loadReport = async (report: typeof reports[0]) => {
    setLoading(true); setActiveReport(report.id);
    try { const { data } = await (reportApi as any)[report.endpoint](); setReportData(data); } catch { setReportData(null); }
    setLoading(false);
  };

  const filtered = reports.filter(r => `${r.name} ${r.desc} ${r.group}`.toLowerCase().includes(search.toLowerCase()));

  return (
    <div>
      <div className="page-head">
        <div><h2>Reports</h2><p>Management and operational reporting hub. All reports generated from posted transaction data with source traceability.</p></div>
        <div className="head-actions">
          {csvExports.map(e => <a key={e.name} href={e.url} className="ghost-btn" style={{ textDecoration: 'none', fontSize: 10 }}>CSV: {e.name}</a>)}
        </div>
      </div>

      <div className="report-library-head">
        <div><strong>Report Library</strong><span>{filtered.length} templates available · CSV exports for all data sets</span></div>
      </div>

      <div className="filter-bar context-filter-bar" style={{ marginTop: 0 }}>
        <input className="filter-input" placeholder="Search reports…" value={search} onChange={e => setSearch(e.target.value)} />
        <span className="filter-count muted">{filtered.length} reports</span>
      </div>

      {!activeReport && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 12 }}>
          {filtered.map(r => (
            <div key={r.id} className="panel report-card" style={{ cursor: 'pointer' }} onClick={() => loadReport(r)}>
              <div className="panel-body">
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}><span className="chip">{r.group}</span></div>
                <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 4, color: '#000' }}>{r.name}</div>
                <div style={{ fontSize: 11, color: '#111', lineHeight: 1.5 }}>{r.desc}</div>
                <div style={{ marginTop: 10, fontSize: 10, color: '#0a4fc5', fontWeight: 700 }}>Generate Report →</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {activeReport && reportData && (
        <div>
          <button className="ghost-btn" onClick={() => { setActiveReport(null); setReportData(null); }} style={{ marginBottom: 14 }}>← Back to Reports</button>
          <div className="panel" style={{ marginBottom: 14 }}>
            <div className="panel-head"><div><h3>{reportData.reportTitle}</h3><p>Generated: {new Date(reportData.generatedAt).toLocaleString()}</p></div></div>
            <div className="panel-body">
              {activeReport === 'trial-balance' && (
                <div>
                  <div className="finance-cards" style={{ marginBottom: 14 }}>
                    <div className="finance-card"><span>Total Debits</span><strong>${Number(reportData.totalDebits).toLocaleString()}</strong></div>
                    <div className="finance-card"><span>Total Credits</span><strong>${Number(reportData.totalCredits).toLocaleString()}</strong></div>
                    <div className="finance-card"><span>Status</span><strong><span className={`status ${reportData.balanced ? 'success' : 'danger'}`}>{reportData.balanced ? 'Balanced ✓' : 'Out of Balance'}</span></strong></div>
                  </div>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead><tr><th>Code</th><th>Account</th><th>Type</th><th>Normal</th><th>Balance</th></tr></thead>
                      <tbody>{reportData.data.map((r: any, i: number) => (<tr key={i}><td><strong style={{ color: '#000' }}>{r.accountCode}</strong></td><td>{r.accountName}</td><td><span className="chip">{r.accountType}</span></td><td><span className="chip">{r.normalBalance}</span></td><td className="amount">${Number(r.balance).toLocaleString()}</td></tr>))}</tbody>
                    </table>
                  </div>
                </div>
              )}
              {activeReport === 'debtor-ageing' && (
                <div className="table-wrap">
                  <table className="data-table">
                    <thead><tr><th>Invoice</th><th>Entity</th><th>Date</th><th>Total</th><th>Paid</th><th>Balance</th><th>Days</th></tr></thead>
                    <tbody>{reportData.data.map((r: any, i: number) => (<tr key={i}><td><strong style={{ color: '#000' }}>{r.invoiceNumber}</strong></td><td>{r.entity}</td><td>{r.invoiceDate}</td><td className="amount">${Number(r.totalAmount).toLocaleString()}</td><td className="amount" style={{ color: '#0b6e52' }}>${Number(r.amountPaid).toLocaleString()}</td><td className="amount" style={{ color: '#9b6715' }}>${Number(r.balance).toLocaleString()}</td><td><span className={`status ${r.daysOutstanding > 90 ? 'danger' : r.daysOutstanding > 30 ? 'warning' : 'success'}`}>{r.daysOutstanding}d</span></td></tr>))}</tbody>
                  </table>
                </div>
              )}
              {activeReport === 'job-cost' && (
                <div className="table-wrap">
                  <table className="data-table">
                    <thead><tr><th>Job</th><th>Vehicle</th><th>Title</th><th>Status</th><th>Priority</th><th>Estimated</th><th>Actual</th><th>Variance</th></tr></thead>
                    <tbody>{reportData.data.map((r: any, i: number) => (<tr key={i}><td><strong style={{ color: '#000' }}>{r.jobNumber}</strong></td><td>{r.registration} — {r.vehicle}</td><td>{r.title}</td><td><span className="chip">{r.status}</span></td><td><span className="chip">{r.priority}</span></td><td className="amount">${Number(r.estimatedTotal).toLocaleString()}</td><td className="amount">${Number(r.actualTotal).toLocaleString()}</td><td className="amount" style={{ color: Number(r.variance) > 0 ? '#7f1d2d' : '#0b6e52' }}>{Number(r.variance) > 0 ? '+' : ''}${Number(r.variance).toLocaleString()}</td></tr>))}</tbody>
                  </table>
                </div>
              )}
              {activeReport === 'inventory-valuation' && (
                <div>
                  <div className="finance-card" style={{ marginBottom: 14 }}><span>Total Inventory Value</span><strong>${Number(reportData.totalValue).toLocaleString()}</strong></div>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead><tr><th>Part</th><th>Description</th><th>On Hand</th><th>Reserved</th><th>Available</th><th>Unit Cost</th><th>Total Value</th><th>Status</th></tr></thead>
                      <tbody>{reportData.data.map((r: any, i: number) => (<tr key={i}><td><strong style={{ color: '#000' }}>{r.partNumber}</strong></td><td>{r.description}</td><td>{r.onHand}</td><td>{r.reserved}</td><td className="amount">{r.available}</td><td className="amount">${Number(r.unitCost).toLocaleString()}</td><td className="amount">${Number(r.totalValue).toLocaleString()}</td><td><span className={`status ${r.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{r.status}</span></td></tr>))}</tbody>
                    </table>
                  </div>
                </div>
              )}
              {activeReport === 'procurement-pipeline' && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14 }}>
                  {['requisitions', 'quotations', 'orders'].map(type => (
                    <div className="panel" key={type}>
                      <div className="panel-head"><h3 style={{ textTransform: 'capitalize' }}>{type}</h3></div>
                      <div className="panel-body">
                        {Array.isArray(reportData[type]) && reportData[type].map((r: any, i: number) => (
                          <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #e3e8f0', fontSize: 11 }}>
                            <span className="chip">{r[0]}</span><span>Qty: {r[1]}</span><span className="amount">${Number(r[2]).toLocaleString()}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="report-governance-note">
            <strong>ⓘ</strong>
            <span>Reports are generated from posted/approved transaction data. All figures are source-traceable. CSV exports share the same query and filter definition as on-screen reports.</span>
          </div>
        </div>
      )}
      {activeReport && loading && <div style={{ padding: 40, textAlign: 'center', color: '#111' }}>Loading report…</div>}
    </div>
  );
}
