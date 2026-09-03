import { useEffect, useState } from 'react';
import { cashbookApi, cashAccountsApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { CashbookEntry } from '../../types';
import toast from 'react-hot-toast';

export default function Cashbook() {
  const [entries, setEntries] = useState<CashbookEntry[]>([]);
  const [cashAccounts, setCashAccounts] = useState<any[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ cashAccountId: '', entryType: 'RECEIPT', amount: 0, counterparty: '', narration: '', reference: '' });

  const load = () => { cashbookApi.list().then(r => setEntries(r.data)).catch(() => {}); cashAccountsApi.list().then(r => setCashAccounts(r.data)).catch(() => {}); };
  useEffect(() => { load(); }, []);

  const table = useTable(entries, {
    searchKeys: ['counterparty', 'narration', 'reference', 'entryType', 'reconciliationStatus'],
    defaultSort: { key: 'entryDate', direction: 'desc' },
    pageSize: 25,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await cashbookApi.create({ ...form, amount: +form.amount }); toast.success('Created'); setShowForm(false); setForm({ cashAccountId: '', entryType: 'RECEIPT', amount: 0, counterparty: '', narration: '', reference: '' }); load(); } catch { toast.error('Failed'); }
  };

  const receipts = entries.filter(e => e.entryType === 'RECEIPT').reduce((s, e) => s + e.amount, 0);
  const payments = entries.filter(e => e.entryType === 'PAYMENT').reduce((s, e) => s + e.amount, 0);

  const columns = [
    { key: 'entryDate', label: 'Date' },
    { key: 'entryType', label: 'Type', render: (e: CashbookEntry) => <span className={`status ${e.entryType === 'RECEIPT' ? 'success' : 'danger'}`}>{e.entryType}</span> },
    { key: 'counterparty', label: 'Counterparty' },
    { key: 'narration', label: 'Narration' },
    { key: 'amount', label: 'Amount', align: 'right' as const, render: (e: CashbookEntry) => <span className="amount" style={{ color: e.entryType === 'RECEIPT' ? '#0b6e52' : '#7f1d2d' }}>{e.entryType === 'RECEIPT' ? '+' : '-'}${e.amount?.toLocaleString()}</span> },
    { key: 'runningBalance', label: 'Balance', align: 'right' as const, render: (e: CashbookEntry) => <span className="amount">${e.runningBalance?.toLocaleString()}</span> },
    { key: 'reconciliationStatus', label: 'Status', render: (e: CashbookEntry) => <span className={`status ${e.reconciliationStatus === 'RECONCILED' ? 'success' : 'warning'}`}>{e.reconciliationStatus}</span> },
  ];

  const csvColumns = [
    { key: 'entryDate', label: 'Date' }, { key: 'entryType', label: 'Type' },
    { key: 'counterparty', label: 'Counterparty' }, { key: 'narration', label: 'Narration' },
    { key: 'amount', label: 'Amount' }, { key: 'runningBalance', label: 'Balance' },
    { key: 'reconciliationStatus', label: 'Status' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Cashbook</h2><p>Bank, cash, and mobile money transaction log.</p></div></div>

      <div className="finance-cards" style={{ marginBottom: 14 }}>
        <div className="finance-card"><span>Receipts</span><strong style={{ color: '#0b6e52' }}>${receipts.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Payments</span><strong style={{ color: '#7f1d2d' }}>${payments.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Net</span><strong style={{ color: receipts - payments >= 0 ? '#0b6e52' : '#7f1d2d' }}>${(receipts - payments).toLocaleString()}</strong></div>
        <div className="finance-card"><span>Unreconciled</span><strong style={{ color: '#9b6715' }}>{entries.filter(e => e.reconciliationStatus === 'UNRECONCILED').length}</strong></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search counterparty, narration…"
        total={table.total} entityName="entries"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('cashbook', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ New Entry"
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(e) => e.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>New Entry</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Account *</label><select required value={form.cashAccountId} onChange={e => setForm({...form, cashAccountId: e.target.value})}><option value="">Select…</option>{cashAccounts.map((a: any) => <option key={a.id} value={a.id}>{a.accountName}</option>)}</select></div>
                  <div className="form-field"><label>Type *</label><select value={form.entryType} onChange={e => setForm({...form, entryType: e.target.value})}><option>RECEIPT</option><option>PAYMENT</option><option>TRANSFER</option></select></div>
                  <div className="form-field"><label>Amount *</label><input type="number" step="0.01" min="0.01" required value={form.amount||''} onChange={e => setForm({...form, amount:+e.target.value})} /></div>
                  <div className="form-field"><label>Counterparty</label><input value={form.counterparty} onChange={e => setForm({...form, counterparty: e.target.value})} /></div>
                  <div className="form-field"><label>Reference</label><input value={form.reference} onChange={e => setForm({...form, reference: e.target.value})} /></div>
                  <div className="form-field full"><label>Narration</label><textarea value={form.narration} onChange={e => setForm({...form, narration: e.target.value})} /></div>
                </div>
                <div className="modal-actions"><button type="button" className="ghost-btn" onClick={() => setShowForm(false)}>Cancel</button><button type="submit" className="primary-btn">Create</button></div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
