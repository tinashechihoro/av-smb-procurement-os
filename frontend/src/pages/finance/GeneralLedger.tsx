import { useEffect, useState } from 'react';
import { journalApi, chartOfAccountsApi } from '../../api/services';
import type { ChartOfAccount, Journal } from '../../types';
import toast from 'react-hot-toast';

export default function GeneralLedger() {
  const [accounts, setAccounts] = useState<ChartOfAccount[]>([]);
  const [journals, setJournals] = useState<Journal[]>([]);
  const [filter, setFilter] = useState('');
  const [activeTab, setActiveTab] = useState<'accounts' | 'journals'>('accounts');
  const [showJournalForm, setShowJournalForm] = useState(false);
  const [journalForm, setJournalForm] = useState({ journalType: 'MANUAL', description: '', lines: [{ accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] });

  useEffect(() => { chartOfAccountsApi.list().then(r => setAccounts(r.data)).catch(() => {}); journalApi.list().then(r => setJournals(r.data)).catch(() => {}); }, []);

  const handlePostJournal = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await journalApi.create({ journalType: journalForm.journalType, description: journalForm.description, lines: journalForm.lines.filter(l => l.accountId && (l.debitAmount > 0 || l.creditAmount > 0)) }); toast.success('Journal created'); setShowJournalForm(false); setJournalForm({ journalType: 'MANUAL', description: '', lines: [{ accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] }); journalApi.list().then(r => setJournals(r.data)); } catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); }
  };
  const handlePost = async (id: string) => { try { await journalApi.post(id); toast.success('Journal posted'); journalApi.list().then(r => setJournals(r.data)); chartOfAccountsApi.list().then(r => setAccounts(r.data)); } catch (err: any) { toast.error(err.response?.data?.message || 'Failed'); } };
  const addLine = () => setJournalForm({ ...journalForm, lines: [...journalForm.lines, { accountId: '', description: '', debitAmount: 0, creditAmount: 0 }] });
  const removeLine = (idx: number) => setJournalForm({ ...journalForm, lines: journalForm.lines.filter((_, i) => i !== idx) });
  const updateLine = (idx: number, field: string, value: any) => { const lines = [...journalForm.lines]; (lines[idx] as any)[field] = field === 'accountId' ? value : +value; setJournalForm({ ...journalForm, lines }); };

  const totalDebits = journalForm.lines.reduce((s, l) => s + l.debitAmount, 0);
  const totalCredits = journalForm.lines.reduce((s, l) => s + l.creditAmount, 0);
  const balanced = totalDebits === totalCredits && totalDebits > 0;
  const filteredAccounts = accounts.filter(a => `${a.accountCode} ${a.accountName}`.toLowerCase().includes(filter.toLowerCase()));
  const typeColor = (t: string) => ({ ASSET:'info', LIABILITY:'warning', EQUITY:'accent', REVENUE:'success', EXPENSE:'danger' }[t] || 'neutral');

  return (
    <div>
      <div className="page-head">
        <div><h2>General Ledger</h2><p>Chart of accounts, double-entry journals, and trial balance. All journals must balance before posting.</p></div>
        {activeTab === 'journals' && <div className="head-actions"><button className="primary-btn" onClick={() => setShowJournalForm(true)}>+ New Journal</button></div>}
      </div>
      <div className="tabs" style={{ marginBottom: 14 }}>
        <button className={`tab-btn ${activeTab === 'accounts' ? 'active' : ''}`} onClick={() => setActiveTab('accounts')}>Chart of Accounts</button>
        <button className={`tab-btn ${activeTab === 'journals' ? 'active' : ''}`} onClick={() => setActiveTab('journals')}>Journals</button>
      </div>
      {activeTab === 'accounts' && (<>
        <div className="finance-cards" style={{ marginBottom: 14 }}>
          {['ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'].map(type => (
            <div className="finance-card" key={type}><span>{type}</span><strong>${accounts.filter(a => a.accountType === type).reduce((s,a) => s + Math.abs(a.currentBalance), 0).toLocaleString()}</strong></div>
          ))}
        </div>
        <div className="filter-bar context-filter-bar">
          <input className="filter-input" placeholder="Search account code or name…" value={filter} onChange={e => setFilter(e.target.value)} />
          <span className="filter-count muted">{filteredAccounts.length} accounts</span>
        </div>
        <div className="panel">
          <div className="table-wrap">
            <table className="data-table">
              <thead><tr><th>Code</th><th>Account Name</th><th>Type</th><th>Normal</th><th>Balance</th></tr></thead>
              <tbody>
                {filteredAccounts.map(a => (
                  <tr key={a.id}>
                    <td><strong style={{ color: '#000' }}>{a.accountCode}</strong></td>
                    <td>{a.accountName}</td>
                    <td><span className={`status ${typeColor(a.accountType)}`}>{a.accountType}</span></td>
                    <td><span className="chip">{a.normalBalance}</span></td>
                    <td className="amount">${a.currentBalance?.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </>)}
      {activeTab === 'journals' && (
        <div className="panel">
          <div className="table-wrap">
            <table className="data-table">
              <thead><tr><th>Journal</th><th>Type</th><th>Description</th><th>Debits</th><th>Credits</th><th>Status</th><th>Action</th></tr></thead>
              <tbody>
                {journals.map(j => (
                  <tr key={j.id}>
                    <td><div className="row-main"><div className="record-icon">▥</div><div className="row-title">{j.journalNumber}</div></div></td>
                    <td><span className="chip">{j.journalType}</span></td>
                    <td>{j.description || '—'}</td>
                    <td className="amount">${j.totalDebit?.toLocaleString()}</td>
                    <td className="amount">${j.totalCredit?.toLocaleString()}</td>
                    <td><span className={`status ${j.status === 'POSTED' ? 'success' : j.status === 'REVERSED' ? 'danger' : 'neutral'}`}>{j.status}</span></td>
                    <td>{j.status === 'DRAFT' && <button className="ghost-btn" onClick={() => handlePost(j.id)}>Post</button>}</td>
                  </tr>
                ))}
                {journals.length === 0 && <tr><td colSpan={7} style={{ textAlign: 'center', padding: 30, color: '#111' }}>No journals yet</td></tr>}
              </tbody>
            </table>
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
                <div className="form-section-title">Journal Lines — must balance (debits = credits)</div>
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
