import { useEffect, useState } from 'react';
import { journalApi, chartOfAccountsApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { ChartOfAccount, Journal } from '../../types';
import toast from 'react-hot-toast';

export default function GeneralLedger() {
  const [accounts, setAccounts] = useState<ChartOfAccount[]>([]);
  const [journals, setJournals] = useState<Journal[]>([]);
  const [activeTab, setActiveTab] = useState<'accounts' | 'journals' | 'trial'>('accounts');
  const [showJournalForm, setShowJournalForm] = useState(false);
  const [journalForm, setJournalForm] = useState({ journalType: 'MANUAL', description: '', lines: [{ accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] });

  useEffect(() => { chartOfAccountsApi.list().then(r => setAccounts(r.data)).catch(() => {}); journalApi.list().then(r => setJournals(r.data)).catch(() => {}); }, []);

  const table = useTable(accounts, {
    searchKeys: ['accountCode', 'accountName', 'accountType', 'subType'],
    defaultSort: { key: 'accountCode', direction: 'asc' },
    pageSize: 50,
  });

  const journalTable = useTable(journals, {
    searchKeys: ['journalNumber', 'journalType', 'description', 'status'],
    defaultSort: { key: 'journalNumber', direction: 'desc' },
    pageSize: 25,
  });

  const handlePostJournal = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await journalApi.create({ journalType: journalForm.journalType, description: journalForm.description, lines: journalForm.lines.filter(l => l.accountId && (l.debitAmount > 0 || l.creditAmount > 0)) }); toast.success('Journal created'); setShowJournalForm(false); setJournalForm({ journalType: 'MANUAL', description: '', lines: [{ accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] }); journalApi.list().then(r => setJournals(r.data)); } catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); }
  };
  const handlePost = async (id: string) => { try { await journalApi.post(id); toast.success('Posted'); journalApi.list().then(r => setJournals(r.data)); chartOfAccountsApi.list().then(r => setAccounts(r.data)); } catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); } };
  const addLine = () => setJournalForm({ ...journalForm, lines: [...journalForm.lines, { accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] });
  const removeLine = (idx: number) => setJournalForm({ ...journalForm, lines: journalForm.lines.filter((_, i) => i !== idx) });
  const updateLine = (idx: number, field: string, value: any) => { const lines = [...journalForm.lines]; (lines[idx] as any)[field] = field === 'accountId' ? value : +value; setJournalForm({ ...journalForm, lines }); };

  const totalDebits = journalForm.lines.reduce((s, l) => s + l.debitAmount, 0);
  const totalCredits = journalForm.lines.reduce((s, l) => s + l.creditAmount, 0);
  const balanced = totalDebits === totalCredits && totalDebits > 0;

  const typeColor = (t: string) => ({ ASSET:'info', LIABILITY:'warning', EQUITY:'accent', REVENUE:'success', EXPENSE:'danger' }[t] || 'neutral');
  const typeOptions = ['ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'].map(s => ({ label: s, value: s }));

  // Trial balance
  const totalDebitBalance = accounts.filter(a => a.normalBalance === 'DEBIT').reduce((s,a) => s + a.currentBalance, 0);
  const totalCreditBalance = accounts.filter(a => a.normalBalance === 'CREDIT').reduce((s,a) => s + Math.abs(a.currentBalance), 0);
  const tbBalanced = Math.abs(totalDebitBalance - totalCreditBalance) < 0.01;

  const accountColumns = [
    { key: 'accountCode', label: 'Code', render: (a: ChartOfAccount) => <strong style={{ color: '#000' }}>{a.accountCode}</strong> },
    { key: 'accountName', label: 'Account Name' },
    { key: 'accountType', label: 'Type', render: (a: ChartOfAccount) => <span className={`status ${typeColor(a.accountType)}`}>{a.accountType}</span> },
    { key: 'subType', label: 'Sub-Type', render: (a: ChartOfAccount) => <span className="chip">{a.subType || '—'}</span> },
    { key: 'normalBalance', label: 'Normal', render: (a: ChartOfAccount) => <span className="chip">{a.normalBalance}</span> },
    { key: 'currentBalance', label: 'Balance', align: 'right' as const, render: (a: ChartOfAccount) => <span className="amount" style={{ fontWeight: 700 }}>${a.currentBalance?.toLocaleString()}</span> },
  ];

  const journalColumns = [
    { key: 'journalNumber', label: 'Journal', render: (j: Journal) => <div className="row-main"><div className="record-icon">▥</div><div className="row-title">{j.journalNumber}</div></div> },
    { key: 'journalType', label: 'Type', render: (j: Journal) => <span className="chip">{j.journalType}</span> },
    { key: 'description', label: 'Description' },
    { key: 'totalDebit', label: 'Debits', align: 'right' as const, render: (j: Journal) => <span className="amount">${j.totalDebit?.toLocaleString()}</span> },
    { key: 'totalCredit', label: 'Credits', align: 'right' as const, render: (j: Journal) => <span className="amount">${j.totalCredit?.toLocaleString()}</span> },
    { key: 'status', label: 'Status', render: (j: Journal) => <span className={`status ${j.status === 'POSTED' ? 'success' : j.status === 'REVERSED' ? 'danger' : 'neutral'}`}>{j.status}</span> },
    { key: 'actions', label: 'Action', sortable: false, render: (j: Journal) => j.status === 'DRAFT' ? <button className="ghost-btn" onClick={(e) => { e.stopPropagation(); handlePost(j.id); }}>Post</button> : null },
  ];

  const csvColumns = [
    { key: 'accountCode', label: 'Code' }, { key: 'accountName', label: 'Account' },
    { key: 'accountType', label: 'Type' }, { key: 'normalBalance', label: 'Normal' },
    { key: 'currentBalance', label: 'Balance' },
  ];

  return (
    <div>
      <div className="page-head">
        <div><h2>General Ledger</h2><p>Chart of accounts, double-entry journals, and trial balance.</p></div>
        {activeTab === 'journals' && <div className="head-actions"><button className="primary-btn" onClick={() => setShowJournalForm(true)}>+ New Journal</button></div>}
      </div>

      <div className="tabs" style={{ marginBottom: 14 }}>
        <button className={`tab-btn ${activeTab === 'accounts' ? 'active' : ''}`} onClick={() => setActiveTab('accounts')}>Chart of Accounts</button>
        <button className={`tab-btn ${activeTab === 'journals' ? 'active' : ''}`} onClick={() => setActiveTab('journals')}>Journals</button>
        <button className={`tab-btn ${activeTab === 'trial' ? 'active' : ''}`} onClick={() => setActiveTab('trial')}>Trial Balance</button>
      </div>

      {activeTab === 'accounts' && (<>
        <div className="finance-cards" style={{ marginBottom: 14 }}>
          {['ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'].map(type => (
            <div className="finance-card" key={type}><span>{type}</span><strong>${accounts.filter(a => a.accountType === type).reduce((s,a) => s + Math.abs(a.currentBalance), 0).toLocaleString()}</strong></div>
          ))}
        </div>
        <TableToolbar search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search code, name…"
          filters={[{ key: 'accountType', label: 'Type', type: 'select', options: typeOptions, value: table.filters.accountType || '', onChange: (v) => table.setFilter('accountType', v) }]}
          total={table.total} entityName="accounts" activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
          onExportCSV={() => table.exportCSV('chart-of-accounts', csvColumns)} />
        <DataTable columns={accountColumns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(a) => a.id} />
        <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={50} onPageChange={table.setPage} />
      </>)}

      {activeTab === 'journals' && (<>
        <TableToolbar search={journalTable.search} onSearchChange={journalTable.setSearch} searchPlaceholder="Search journal…"
          total={journalTable.total} entityName="journals" activeFilterCount={journalTable.activeFilterCount} onClearFilters={journalTable.clearFilters}
          onExportCSV={() => journalTable.exportCSV('journals', [{ key: 'journalNumber', label: 'Journal' }, { key: 'journalType', label: 'Type' }, { key: 'description', label: 'Description' }, { key: 'totalDebit', label: 'Debits' }, { key: 'totalCredit', label: 'Credits' }, { key: 'status', label: 'Status' }])}
          onCreateClick={() => setShowJournalForm(true)} createLabel="+ New Journal" />
        <DataTable columns={journalColumns} data={journalTable.data} sort={journalTable.sort} onSort={journalTable.toggleSort} keyExtractor={(j) => j.id} />
        <Pagination page={journalTable.page} totalPages={journalTable.totalPages} total={journalTable.total} pageSize={25} onPageChange={journalTable.setPage} />
      </>)}

      {activeTab === 'trial' && (
        <div>
          <div className="finance-cards" style={{ marginBottom: 14 }}>
            <div className="finance-card"><span>Total Debits</span><strong>${totalDebitBalance.toLocaleString()}</strong></div>
            <div className="finance-card"><span>Total Credits</span><strong>${totalCreditBalance.toLocaleString()}</strong></div>
            <div className="finance-card"><span>Status</span><strong><span className={`status ${tbBalanced ? 'success' : 'danger'}`}>{tbBalanced ? 'Balanced ✓' : 'Out of Balance'}</span></strong></div>
            <div className="finance-card"><span>Difference</span><strong style={{ color: tbBalanced ? '#0b6e52' : '#7f1d2d' }}>${Math.abs(totalDebitBalance - totalCreditBalance).toLocaleString()}</strong></div>
          </div>
          <div className="panel">
            <div className="table-wrap">
              <table className="data-table">
                <thead><tr><th>Code</th><th>Account</th><th>Type</th><th>Normal</th><th style={{ textAlign: 'right' }}>Debit</th><th style={{ textAlign: 'right' }}>Credit</th></tr></thead>
                <tbody>
                  {accounts.filter(a => a.currentBalance !== 0).map(a => (
                    <tr key={a.id}>
                      <td><strong style={{ color: '#000' }}>{a.accountCode}</strong></td>
                      <td>{a.accountName}</td>
                      <td><span className={`status ${typeColor(a.accountType)}`}>{a.accountType}</span></td>
                      <td><span className="chip">{a.normalBalance}</span></td>
                      <td className="amount" style={{ textAlign: 'right' }}>{a.normalBalance === 'DEBIT' && a.currentBalance > 0 ? `$${a.currentBalance.toLocaleString()}` : ''}</td>
                      <td className="amount" style={{ textAlign: 'right' }}>{a.normalBalance === 'CREDIT' && a.currentBalance > 0 ? `$${a.currentBalance.toLocaleString()}` : ''}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr style={{ borderTop: '2px solid #000' }}>
                    <td colSpan={4} style={{ fontWeight: 800, textAlign: 'right' }}>TOTALS</td>
                    <td className="amount" style={{ textAlign: 'right', fontWeight: 800 }}>${totalDebitBalance.toLocaleString()}</td>
                    <td className="amount" style={{ textAlign: 'right', fontWeight: 800 }}>${totalCreditBalance.toLocaleString()}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      )}

      {showJournalForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowJournalForm(false)}>
          <div className="modal-card" style={{ width: 'min(900px, 96vw)' }} onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>New Manual Journal</h2></div><button className="icon-btn" onClick={() => setShowJournalForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handlePostJournal}>
                <div className="form-grid">
                  <div className="form-field"><label>Type</label><select value={journalForm.journalType} onChange={e => setJournalForm({...journalForm, journalType: e.target.value})}><option>MANUAL</option><option>CORRECTION</option><option>REVERSAL</option></select></div>
                  <div className="form-field"><label>Description</label><input value={journalForm.description} onChange={e => setJournalForm({...journalForm, description: e.target.value})} /></div>
                </div>
                <div className="form-section-title">Journal Lines — debits must equal credits</div>
                {journalForm.lines.map((line, idx) => (
                  <div className="part-row" key={idx}>
                    <div className="form-field"><label>Account *</label><select value={line.accountId} onChange={e => updateLine(idx,'accountId',e.target.value)} required><option value="">Select…</option>{accounts.filter(a => a.isPostable !== false).map(a => <option key={a.id} value={a.id}>{a.accountCode} — {a.accountName}</option>)}</select></div>
                    <div className="form-field"><label>Description</label><input value={line.description} onChange={e => updateLine(idx,'description',e.target.value)} /></div>
                    <div className="form-field"><label>Debit $</label><input type="number" min="0" step="0.01" value={line.debitAmount||''} onChange={e => updateLine(idx,'debitAmount',e.target.value)} /></div>
                    <div className="form-field"><label>Credit $</label><input type="number" min="0" step="0.01" value={line.creditAmount||''} onChange={e => updateLine(idx,'creditAmount',e.target.value)} /></div>
                    {journalForm.lines.length > 1 && <button type="button" className="icon-btn" onClick={() => removeLine(idx)}>×</button>}
                  </div>
                ))}
                <div className="line-actions"><button type="button" className="ghost-btn" onClick={addLine}>+ Add line</button></div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: 12, border: '1px solid #e3e8f0', borderRadius: 11, marginTop: 8, background: '#f8faff' }}>
                  <div>Debits: <strong className="amount">${totalDebits.toLocaleString()}</strong> · Credits: <strong className="amount">${totalCredits.toLocaleString()}</strong></div>
                  <span className={`status ${balanced ? 'success' : 'danger'}`}>{balanced ? 'Balanced ✓' : 'Out of Balance'}</span>
                </div>
                <div className="modal-actions"><button type="button" className="ghost-btn" onClick={() => setShowJournalForm(false)}>Cancel</button><button type="submit" className="primary-btn" disabled={!balanced}>Create Journal</button></div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
