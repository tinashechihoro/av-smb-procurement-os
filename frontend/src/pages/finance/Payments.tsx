import { useEffect, useState } from 'react';
import { paymentApi, invoiceApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { Payment, Invoice } from '../../types';
import toast from 'react-hot-toast';

export default function Payments() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ invoiceId: '', paymentType: 'RECEIPT', amount: 0, paymentMethod: 'BANK_TRANSFER', reference: '', notes: '' });

  const load = () => { paymentApi.list().then(r => setPayments(r.data)).catch(() => {}); invoiceApi.list().then(r => setInvoices(r.data.filter((i: Invoice) => i.balanceDue > 0))).catch(() => {}); };
  useEffect(() => { load(); }, []);

  const table = useTable(payments, {
    searchKeys: ['paymentNumber', 'paymentType', 'paymentMethod', 'reference', 'status'],
    defaultSort: { key: 'paymentDate', direction: 'desc' },
    pageSize: 25,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await paymentApi.create({ ...form, amount: +form.amount, invoiceId: form.invoiceId || null }); toast.success('Recorded'); setShowForm(false); setForm({ invoiceId: '', paymentType: 'RECEIPT', amount: 0, paymentMethod: 'BANK_TRANSFER', reference: '', notes: '' }); load(); } catch { toast.error('Failed'); }
  };

  const totalReceived = payments.filter(p => p.paymentType === 'RECEIPT' && p.status === 'COMPLETED').reduce((s,p) => s + p.amount, 0);
  const totalRefunded = payments.filter(p => p.paymentType === 'REFUND').reduce((s,p) => s + p.amount, 0);

  const columns = [
    { key: 'paymentNumber', label: 'Payment', render: (p: Payment) => <div className="row-main"><div className="record-icon">↕</div><div className="row-title">{p.paymentNumber}</div></div> },
    { key: 'paymentType', label: 'Type', render: (p: Payment) => <span className={`status ${p.paymentType === 'RECEIPT' ? 'success' : 'warning'}`}>{p.paymentType}</span> },
    { key: 'paymentMethod', label: 'Method' },
    { key: 'amount', label: 'Amount', align: 'right' as const, render: (p: Payment) => <span className="amount">${p.amount?.toLocaleString()}</span> },
    { key: 'reference', label: 'Reference' },
    { key: 'status', label: 'Status', render: (p: Payment) => <span className={`status ${p.status === 'COMPLETED' ? 'success' : 'warning'}`}>{p.status}</span> },
    { key: 'paymentDate', label: 'Date' },
  ];

  const csvColumns = [
    { key: 'paymentNumber', label: 'Payment' }, { key: 'paymentType', label: 'Type' },
    { key: 'paymentMethod', label: 'Method' }, { key: 'amount', label: 'Amount' },
    { key: 'reference', label: 'Reference' }, { key: 'status', label: 'Status' }, { key: 'paymentDate', label: 'Date' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Payments</h2><p>Payment processing linked to invoices.</p></div></div>

      <div className="finance-cards" style={{ marginBottom: 14 }}>
        <div className="finance-card"><span>Received</span><strong style={{ color: '#0b6e52' }}>${totalReceived.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Refunded</span><strong style={{ color: '#7f1d2d' }}>${totalRefunded.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Transactions</span><strong>{payments.length}</strong></div>
        <div className="finance-card"><span>Completed</span><strong>{payments.filter(p => p.status === 'COMPLETED').length}</strong></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search payment…"
        total={table.total} entityName="payments"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('payments', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ Record Payment"
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(p) => p.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>Record Payment</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Type *</label><select value={form.paymentType} onChange={e => setForm({...form, paymentType: e.target.value})}><option>RECEIPT</option><option>REFUND</option></select></div>
                  <div className="form-field"><label>Amount *</label><input type="number" step="0.01" min="0.01" required value={form.amount||''} onChange={e => setForm({...form, amount:+e.target.value})} /></div>
                  <div className="form-field"><label>Method *</label><select value={form.paymentMethod} onChange={e => setForm({...form, paymentMethod: e.target.value})}><option>BANK_TRANSFER</option><option>CASH</option><option>CHEQUE</option><option>MOBILE_MONEY</option><option>CARD</option></select></div>
                  <div className="form-field"><label>Invoice</label><select value={form.invoiceId} onChange={e => setForm({...form, invoiceId: e.target.value})}><option value="">— None —</option>{invoices.map(i => <option key={i.id} value={i.id}>{i.invoiceNumber} Bal: ${i.balanceDue?.toLocaleString()}</option>)}</select></div>
                  <div className="form-field"><label>Reference</label><input value={form.reference} onChange={e => setForm({...form, reference: e.target.value})} /></div>
                  <div className="form-field full"><label>Notes</label><textarea value={form.notes} onChange={e => setForm({...form, notes: e.target.value})} /></div>
                </div>
                <div className="modal-actions"><button type="button" className="ghost-btn" onClick={() => setShowForm(false)}>Cancel</button><button type="submit" className="primary-btn">Record</button></div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
