import { useEffect, useState } from 'react';
import { supplierApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { Supplier } from '../../types';
import toast from 'react-hot-toast';

export default function Suppliers() {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', contactPerson: '', email: '', phone: '' });

  const load = () => supplierApi.list().then(r => setSuppliers(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(suppliers, { searchKeys: ['name', 'contactPerson', 'email', 'phone'], defaultSort: { key: 'name', direction: 'asc' }, pageSize: 25 });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await supplierApi.create(form); toast.success('Created'); setShowForm(false); setForm({ name: '', contactPerson: '', email: '', phone: '' }); load(); } catch { toast.error('Failed'); }
  };

  const columns = [
    { key: 'name', label: 'Supplier', render: (s: Supplier) => <div className="row-main"><div className="record-icon">▣</div><div className="row-title">{s.name}</div></div> },
    { key: 'contactPerson', label: 'Contact' },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Phone' },
    { key: 'isActive', label: 'Status', render: (s: Supplier) => <span className={`status ${s.isActive ? 'success' : 'neutral'}`}>{s.isActive ? 'Active' : 'Inactive'}</span> },
  ];

  const csvColumns = [{ key: 'name', label: 'Name' }, { key: 'contactPerson', label: 'Contact' }, { key: 'email', label: 'Email' }, { key: 'phone', label: 'Phone' }, { key: 'isActive', label: 'Active' }];

  return (
    <div>
      <div className="page-head"><div><h2>Suppliers</h2><p>External supplier directory. SMB confidential.</p></div></div>
      <TableToolbar search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search supplier…" total={table.total} entityName="suppliers" activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters} onExportCSV={() => table.exportCSV('suppliers', csvColumns)} onCreateClick={() => setShowForm(true)} createLabel="+ Add Supplier" />
      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(s) => s.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>Add Supplier</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Name *</label><input required value={form.name} onChange={e => setForm({...form, name: e.target.value})} /></div>
                  <div className="form-field"><label>Contact</label><input value={form.contactPerson} onChange={e => setForm({...form, contactPerson: e.target.value})} /></div>
                  <div className="form-field"><label>Email</label><input type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} /></div>
                  <div className="form-field"><label>Phone</label><input value={form.phone} onChange={e => setForm({...form, phone: e.target.value})} /></div>
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
