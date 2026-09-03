import { useEffect, useState } from 'react';
import { requisitionApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { Requisition } from '../../types';
import toast from 'react-hot-toast';

export default function Requisitions() {
  const [reqs, setReqs] = useState<Requisition[]>([]);
  const [selected, setSelected] = useState<Requisition | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: '', priority: 'NORMAL', notes: '', items: [{ partNumber: '', description: '', quantity: 1, estimatedCost: 0 }] });

  const load = () => requisitionApi.list().then(r => setReqs(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(reqs, {
    searchKeys: ['requisitionNumber', 'title', 'status', 'priority'],
    defaultSort: { key: 'requisitionNumber', direction: 'desc' },
    pageSize: 25,
  });

  const handleSubmit = async (id: string) => {
    try { await requisitionApi.submit(id); toast.success('Submitted'); load(); } catch { toast.error('Failed'); }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await requisitionApi.create({ title: form.title, priority: form.priority, notes: form.notes, items: form.items.filter(i => i.description) });
      toast.success('Created'); setShowForm(false);
      setForm({ title: '', priority: 'NORMAL', notes: '', items: [{ partNumber: '', description: '', quantity: 1, estimatedCost: 0 }] });
      load();
    } catch { toast.error('Failed'); }
  };

  const addLine = () => setForm({ ...form, items: [...form.items, { partNumber: '', description: '', quantity: 1, estimatedCost: 0 }] });
  const removeLine = (idx: number) => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) });
  const updateLine = (idx: number, field: string, value: any) => { const items = [...form.items]; (items[idx] as any)[field] = value; setForm({ ...form, items }); };

  const statusClass = (s: string) => {
    const v = s.toLowerCase();
    if (v.includes('approved') || v.includes('converted')) return 'success';
    if (v.includes('cancel')) return 'danger';
    if (v.includes('clarification') || v.includes('draft')) return 'warning';
    if (v.includes('submit') || v.includes('review') || v.includes('quoted')) return 'info';
    return 'neutral';
  };

  const statusOptions = ['DRAFT','SUBMITTED','UNDER_REVIEW','CLARIFICATION','QUOTED','APPROVED','CONVERTED_TO_ORDER','CLOSED','CANCELLED'].map(s => ({ label: s, value: s }));

  const columns = [
    { key: 'requisitionNumber', label: 'Requisition', render: (r: Requisition) => (
      <div className="row-main"><div className="record-icon">≡</div><div className="row-title">{r.requisitionNumber}</div></div>
    )},
    { key: 'title', label: 'Title' },
    { key: 'status', label: 'Status', render: (r: Requisition) => <span className={`status ${statusClass(r.status)}`}>{r.status}</span> },
    { key: 'priority', label: 'Priority', render: (r: Requisition) => <span className="chip">{r.priority}</span> },
    { key: 'items', label: 'Lines', align: 'right' as const, render: (r: Requisition) => r.items?.length || 0 },
    { key: 'totalEstimate', label: 'Estimate', align: 'right' as const, render: (r: Requisition) => <span className="amount">${r.totalEstimate?.toLocaleString()}</span> },
    { key: 'actions', label: 'Action', sortable: false, render: (r: Requisition) => r.status === 'DRAFT' ? <button className="ghost-btn" onClick={(e) => { e.stopPropagation(); handleSubmit(r.id); }}>Submit</button> : null },
  ];

  const csvColumns = [
    { key: 'requisitionNumber', label: 'Number' },
    { key: 'title', label: 'Title' },
    { key: 'status', label: 'Status' },
    { key: 'priority', label: 'Priority' },
    { key: 'totalEstimate', label: 'Estimate' },
  ];

  return (
    <div>
      <div className="page-head">
        <div><h2>Requisitions</h2><p>Spare parts requests with amendment workflow. Click rows for details.</p></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch}
        searchPlaceholder="Search number, title…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="requisitions"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('requisitions', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ New Requisition"
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(r) => setSelected(r)} keyExtractor={(r) => r.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.requisitionNumber || ''} subtitle="REQUISITION" icon="≡"
        fields={[
          { label: 'Title', value: selected?.title, full: true },
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Priority', value: selected?.priority },
          { label: 'Total Estimate', value: selected ? `$${selected.totalEstimate?.toLocaleString()}` : '' },
        ]}
      >
        {selected?.items && selected.items.length > 0 && (
          <div>
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Line Items</h3>
            <table className="data-table" style={{ minWidth: 'auto' }}>
              <thead><tr><th>#</th><th>Part</th><th>Description</th><th>Qty</th><th>Est. $</th></tr></thead>
              <tbody>
                {selected.items.map((item, i) => (
                  <tr key={i}>
                    <td>{item.lineNumber}</td>
                    <td>{item.partNumber || '—'}</td>
                    <td>{item.description}</td>
                    <td>{item.quantity}</td>
                    <td className="amount">${item.estimatedCost?.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DetailDrawer>

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" style={{ width: 'min(900px, 96vw)' }} onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>New Requisition</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Title *</label><input required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></div>
                  <div className="form-field"><label>Priority</label><select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}><option>LOW</option><option>NORMAL</option><option>HIGH</option><option>URGENT</option></select></div>
                  <div className="form-field full"><label>Notes</label><textarea value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></div>
                </div>
                <div className="form-section-title">Part Lines</div>
                {form.items.map((item, idx) => (
                  <div className="part-row" key={idx}>
                    <div className="form-field"><label>Part #</label><input value={item.partNumber} onChange={e => updateLine(idx, 'partNumber', e.target.value)} /></div>
                    <div className="form-field"><label>Qty</label><input type="number" min="1" value={item.quantity} onChange={e => updateLine(idx, 'quantity', +e.target.value)} /></div>
                    <div className="form-field"><label>Est. $</label><input type="number" min="0" step="0.01" value={item.estimatedCost || ''} onChange={e => updateLine(idx, 'estimatedCost', +e.target.value)} /></div>
                    <div className="form-field"><label>Description *</label><input required value={item.description} onChange={e => updateLine(idx, 'description', e.target.value)} /></div>
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
