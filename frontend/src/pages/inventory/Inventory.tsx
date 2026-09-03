import { useEffect, useState } from 'react';
import { inventoryApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { InventoryItem } from '../../types';
import toast from 'react-hot-toast';

export default function Inventory() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [selected, setSelected] = useState<InventoryItem | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ partNumber: '', description: '', quantityOnHand: 0, reorderLevel: 0, unitCost: 0, sellingPrice: 0, category: '' });

  const load = () => inventoryApi.list().then(r => setItems(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(items, {
    searchKeys: ['partNumber', 'description', 'category', 'status'],
    defaultSort: { key: 'partNumber', direction: 'asc' },
    pageSize: 25,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await inventoryApi.create(form); toast.success('Added'); setShowForm(false); setForm({ partNumber: '', description: '', quantityOnHand: 0, reorderLevel: 0, unitCost: 0, sellingPrice: 0, category: '' }); load(); } catch { toast.error('Failed'); }
  };

  const totalValue = items.reduce((s, i) => s + (i.quantityOnHand * (i.unitCost || 0)), 0);
  const lowStock = items.filter(i => i.quantityAvailable <= i.reorderLevel);

  const columns = [
    { key: 'partNumber', label: 'Part #', render: (i: InventoryItem) => <div className="row-main"><div className="record-icon">▦</div><div className="row-title">{i.partNumber}</div></div> },
    { key: 'description', label: 'Description' },
    { key: 'quantityOnHand', label: 'On Hand', align: 'right' as const },
    { key: 'quantityReserved', label: 'Reserved', align: 'right' as const },
    { key: 'quantityAvailable', label: 'Available', align: 'right' as const, render: (i: InventoryItem) => <span style={{ color: i.quantityAvailable <= i.reorderLevel ? '#9b6715' : '#0b6e52', fontWeight: 600 }}>{i.quantityAvailable}</span> },
    { key: 'reorderLevel', label: 'Reorder', align: 'right' as const },
    { key: 'unitCost', label: 'Unit Cost', align: 'right' as const, render: (i: InventoryItem) => <span className="amount">${i.unitCost?.toLocaleString()}</span> },
    { key: 'status', label: 'Status', render: (i: InventoryItem) => <span className={`status ${i.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{i.status}</span> },
  ];

  const csvColumns = [
    { key: 'partNumber', label: 'Part #' }, { key: 'description', label: 'Description' },
    { key: 'quantityOnHand', label: 'On Hand' }, { key: 'quantityReserved', label: 'Reserved' },
    { key: 'quantityAvailable', label: 'Available' }, { key: 'reorderLevel', label: 'Reorder' },
    { key: 'unitCost', label: 'Unit Cost' }, { key: 'status', label: 'Status' },
  ];

  return (
    <div>
      <div className="page-head">
        <div><h2>Inventory</h2><p>Stock levels, reservations, and parts management.</p></div>
      </div>

      <div className="kpi-grid" style={{ marginBottom: 14 }}>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Items</span><span className="kpi-icon">▦</span></div><div className="kpi-value">{items.length}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Value</span><span className="kpi-icon">$</span></div><div className="kpi-value">${totalValue.toLocaleString()}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Low Stock</span><span className="kpi-icon">!</span></div><div className="kpi-value" style={{ color: lowStock.length > 0 ? '#9b6715' : '#0b6e52' }}>{lowStock.length}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">On Hand</span><span className="kpi-icon">▦</span></div><div className="kpi-value">{items.reduce((s,i) => s + i.quantityOnHand, 0).toLocaleString()}</div></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search part #, description…"
        total={table.total} entityName="items"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('inventory', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ Add Item"
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(i) => setSelected(i)} keyExtractor={(i) => i.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.partNumber || ''} subtitle="INVENTORY ITEM" icon="▦"
        fields={[
          { label: 'Description', value: selected?.description, full: true },
          { label: 'On Hand', value: selected?.quantityOnHand }, { label: 'Reserved', value: selected?.quantityReserved },
          { label: 'Available', value: selected?.quantityAvailable }, { label: 'Reorder Level', value: selected?.reorderLevel },
          { label: 'Unit Cost', value: selected ? `$${selected.unitCost?.toLocaleString()}` : '' },
          { label: 'Selling Price', value: selected ? `$${selected.sellingPrice?.toLocaleString()}` : '' },
          { label: 'Category', value: selected?.category || '—' },
          { label: 'Status', value: selected ? <span className={`status ${selected.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{selected.status}</span> : '' },
        ]}
      />

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>Add Item</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Part # *</label><input required value={form.partNumber} onChange={e => setForm({...form, partNumber: e.target.value})} /></div>
                  <div className="form-field"><label>Description *</label><input required value={form.description} onChange={e => setForm({...form, description: e.target.value})} /></div>
                  <div className="form-field"><label>On Hand</label><input type="number" min="0" value={form.quantityOnHand||''} onChange={e => setForm({...form, quantityOnHand:+e.target.value})} /></div>
                  <div className="form-field"><label>Reorder Level</label><input type="number" min="0" value={form.reorderLevel||''} onChange={e => setForm({...form, reorderLevel:+e.target.value})} /></div>
                  <div className="form-field"><label>Unit Cost</label><input type="number" min="0" step="0.01" value={form.unitCost||''} onChange={e => setForm({...form, unitCost:+e.target.value})} /></div>
                  <div className="form-field"><label>Selling Price</label><input type="number" min="0" step="0.01" value={form.sellingPrice||''} onChange={e => setForm({...form, sellingPrice:+e.target.value})} /></div>
                  <div className="form-field"><label>Category</label><input value={form.category} onChange={e => setForm({...form, category: e.target.value})} /></div>
                </div>
                <div className="modal-actions"><button type="button" className="ghost-btn" onClick={() => setShowForm(false)}>Cancel</button><button type="submit" className="primary-btn">Add</button></div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
