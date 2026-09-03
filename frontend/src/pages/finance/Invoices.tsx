import { useEffect, useState } from 'react';
import { invoiceApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { Invoice } from '../../types';
import toast from 'react-hot-toast';

export default function Invoices() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [selected, setSelected] = useState<Invoice | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ fromEntity: '', toEntity: '', taxRate: 0, dueDate: '', notes: '', items: [{ description: '', quantity: 1, unitPrice: 0 }] });

  const load = () => invoiceApi.list().then(r => setInvoices(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(invoices, {
    searchKeys: ['invoiceNumber', 'fromEntity', 'toEntity', 'status'],
    defaultSort: { key: 'invoiceNumber', direction: 'desc' },
    pageSize: 25,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await invoiceApi.create({ ...form, taxRate: +form.taxRate, items: form.items.filter(i => i.description) }); toast.success('Created'); setShowForm(false); setForm({ fromEntity: '', toEntity: '', taxRate: 0, dueDate: '', notes: '', items: [{ description: '', quantity: 1, unitPrice: 0 }] }); load(); } catch { toast.error('Failed'); }
  };
  const addLine = () => setForm({ ...form, items: [...form.items, { description: '', quantity: 1, unitPrice: 0 }] });
  const removeLine = (idx: number) => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) });
  const updateLine = (idx: number, field: string, value: any) => { const items = [...form.items]; (items[idx] as any)[field] = value; setForm({ ...form, items }); };

  const statusClass = (s: string) => ({ DRAFT:'neutral', SUBMITTED:'info', APPROVED:'accent', SENT:'info', PARTIAL_PAYMENT:'warning', PAID:'success', OVERDUE:'danger', CANCELLED:'danger' }[s]||'neutral');
  const statusOptions = ['DRAFT','SUBMITTED','APPROVED','SENT','PARTIAL_PAYMENT','PAID','OVERDUE','CANCELLED'].map(s => ({ label: s, value: s }));

  const totalInvoiced = invoices.reduce((s,i) => s + i.totalAmount, 0);
  const totalPaid = invoices.reduce((s,i) => s + i.amountPaid, 0);
  const totalOutstanding = totalInvoiced - totalPaid;

  const columns = [
    { key: 'invoiceNumber', label: 'Invoice', render: (i: Invoice) => <div className="row-main"><div className="record-icon">▤</div><div className="row-title">{i.invoiceNumber}</div></div> },
    { key: 'fromEntity', label: 'From' },
    { key: 'toEntity', label: 'To' },
    { key: 'status', label: 'Status', render: (i: Invoice) => <span className={`status ${statusClass(i.status)}`}>{i.status}</span> },
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (i: Invoice) => <span className="amount">${i.totalAmount?.toLocaleString()}</span> },
    { key: 'amountPaid', label: 'Paid', align: 'right' as const, render: (i: Invoice) => <span className="amount" style={{ color: '#0b6e52' }}>${i.amountPaid?.toLocaleString()}</span> },
    { key: 'balanceDue', label: 'Balance', align: 'right' as const, render: (i: Invoice) => <span className="amount" style={{ color: i.balanceDue > 0 ? '#9b6715' : '#0b6e52' }}>${i.balanceDue?.toLocaleString()}</span> },
    { key: 'invoiceDate', label: 'Date' },
  ];

  const csvColumns = [
    { key: 'invoiceNumber', label: 'Invoice' }, { key: 'fromEntity', label: 'From' }, { key: 'toEntity', label: 'To' },
    { key: 'status', label: 'Status' }, { key: 'totalAmount', label: 'Total' }, { key: 'amountPaid', label: 'Paid' },
    { key: 'balanceDue', label: 'Balance' }, { key: 'invoiceDate', label: 'Date' },
  ];

  return (
    <div>
      <div className="page-head">
        <div><h2>Invoices</h2><p>Billing with three-way verification. Click rows for details.</p></div>
      </div>

      <div className="finance-cards" style={{ marginBottom: 14 }}>
        <div className="finance-card"><span>Total Invoiced</span><strong>${totalInvoiced.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Total Paid</span><strong style={{ color: '#0b6e52' }}>${totalPaid.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Outstanding</span><strong style={{ color: '#9b6715' }}>${totalOutstanding.toLocaleString()}</strong></div>
        <div className="finance-card"><span>Invoices</span><strong>{invoices.length}</strong></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search invoice number, entity…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="invoices"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('invoices', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ New Invoice"
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(i) => setSelected(i)} keyExtractor={(i) => i.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.invoiceNumber || ''} subtitle="INVOICE" icon="▤"
        fields={[
          { label: 'From', value: selected?.fromEntity }, { label: 'To', value: selected?.toEntity },
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Date', value: selected?.invoiceDate }, { label: 'Due Date', value: selected?.dueDate || '—' },
          { label: 'Total', value: selected ? `$${selected.totalAmount?.toLocaleString()}` : '' },
          { label: 'Paid', value: selected ? `$${selected.amountPaid?.toLocaleString()}` : '' },
          { label: 'Balance', value: selected ? `$${selected.balanceDue?.toLocaleString()}` : '' },
        ]}
      >
        {selected?.items && selected.items.length > 0 && (
          <div>
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Line Items</h3>
            <table className="data-table" style={{ minWidth: 'auto' }}>
              <thead><tr><th>#</th><th>Description</th><th>Qty</th><th>Unit $</th><th>Total</th></tr></thead>
              <tbody>{selected.items.map((item, i) => (
                <tr key={i}><td>{item.lineNumber}</td><td>{item.description}</td><td>{item.quantity}</td><td className="amount">${item.unitPrice?.toLocaleString()}</td><td className="amount">${item.lineTotal?.toLocaleString()}</td></tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </DetailDrawer>

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" style={{ width: 'min(900px, 96vw)' }} onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>New Invoice</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>From *</label><input required value={form.fromEntity} onChange={e => setForm({...form, fromEntity: e.target.value})} /></div>
                  <div className="form-field"><label>To *</label><input required value={form.toEntity} onChange={e => setForm({...form, toEntity: e.target.value})} /></div>
                  <div className="form-field"><label>Tax %</label><input type="number" step="0.01" value={form.taxRate} onChange={e => setForm({...form, taxRate: +e.target.value})} /></div>
                  <div className="form-field"><label>Due Date</label><input type="date" value={form.dueDate} onChange={e => setForm({...form, dueDate: e.target.value})} /></div>
                </div>
                <div className="form-section-title">Line Items</div>
                {form.items.map((item, idx) => (
                  <div className="part-row" key={idx}>
                    <div className="form-field"><label>Description *</label><input required value={item.description} onChange={e => updateLine(idx,'description',e.target.value)} /></div>
                    <div className="form-field"><label>Qty</label><input type="number" min="1" value={item.quantity} onChange={e => updateLine(idx,'quantity',+e.target.value)} /></div>
                    <div className="form-field"><label>Unit $</label><input type="number" min="0" step="0.01" value={item.unitPrice||''} onChange={e => updateLine(idx,'unitPrice',+e.target.value)} /></div>
                    <div className="form-field"><label>Total</label><input value={`$${(item.quantity * item.unitPrice).toLocaleString()}`} readOnly style={{ background: '#f3f6fb', fontWeight: 700 }} /></div>
                    {form.items.length > 1 && <button type="button" className="icon-btn" onClick={() => removeLine(idx)}>×</button>}
                  </div>
                ))}
                <div className="line-actions"><button type="button" className="ghost-btn" onClick={addLine}>+ Add line</button></div>
                <div className="modal-actions"><button type="button" className="ghost-btn" onClick={() => setShowForm(false)}>Cancel</button><button type="submit" className="primary-btn">Create</button></div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
