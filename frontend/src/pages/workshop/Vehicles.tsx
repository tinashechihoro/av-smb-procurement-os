import { useEffect, useState } from 'react';
import { vehicleApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { Vehicle } from '../../types';
import toast from 'react-hot-toast';

export default function Vehicles() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [selected, setSelected] = useState<Vehicle | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ registration: '', make: '', model: '', year: 0, color: '' });

  const load = () => vehicleApi.list().then(r => setVehicles(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(vehicles, {
    searchKeys: ['registration', 'make', 'model', 'color', 'status'],
    defaultSort: { key: 'registration', direction: 'asc' },
    pageSize: 25,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await vehicleApi.create(form);
      toast.success('Vehicle created'); setShowForm(false);
      setForm({ registration: '', make: '', model: '', year: 0, color: '' });
      load();
    } catch { toast.error('Failed'); }
  };

  const statusClass = (s: string) => {
    const v = s.toLowerCase();
    if (v.includes('complete') || v.includes('deliver')) return 'success';
    if (v.includes('await')) return 'warning';
    if (v.includes('repair')) return 'info';
    return 'neutral';
  };

  const columns = [
    { key: 'registration', label: 'Registration', render: (v: Vehicle) => (
      <div className="row-main"><div className="record-icon">◇</div><div className="row-title">{v.registration}</div></div>
    )},
    { key: 'make', label: 'Make & Model', render: (v: Vehicle) => `${v.make} ${v.model}${v.year ? ` ${v.year}` : ''}` },
    { key: 'color', label: 'Colour' },
    { key: 'status', label: 'Status', render: (v: Vehicle) => <span className={`status ${statusClass(v.status)}`}>{v.status}</span> },
  ];

  const csvColumns = [
    { key: 'registration', label: 'Registration' },
    { key: 'make', label: 'Make' },
    { key: 'model', label: 'Model' },
    { key: 'year', label: 'Year' },
    { key: 'color', label: 'Colour' },
    { key: 'status', label: 'Status' },
  ];

  return (
    <div>
      <div className="page-head">
        <div><h2>Vehicles & Jobs</h2><p>Workshop vehicles and repair case files. Click any row to view details.</p></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch}
        searchPlaceholder="Search registration, make, model…"
        total={table.total} entityName="vehicles"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('vehicles', csvColumns)}
        onCreateClick={() => setShowForm(true)} createLabel="+ Add Vehicle"
      />

      <DataTable
        columns={columns} data={table.data}
        sort={table.sort} onSort={table.toggleSort}
        onRowClick={(v) => setSelected(v)}
        keyExtractor={(v) => v.id}
      />

      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.registration || ''} subtitle="VEHICLE DETAILS" icon="◇"
        fields={[
          { label: 'Registration', value: selected?.registration },
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Make', value: selected?.make },
          { label: 'Model', value: selected?.model },
          { label: 'Year', value: selected?.year },
          { label: 'Colour', value: selected?.color },
        ]}
      />

      {showForm && (
        <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={() => setShowForm(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head"><div><div className="eyebrow">CREATE</div><h2>Add Vehicle</h2></div><button className="icon-btn" onClick={() => setShowForm(false)}>×</button></div>
            <div className="modal-body">
              <form onSubmit={handleCreate}>
                <div className="form-grid">
                  <div className="form-field"><label>Registration *</label><input required value={form.registration} onChange={e => setForm({ ...form, registration: e.target.value })} /></div>
                  <div className="form-field"><label>Make *</label><input required value={form.make} onChange={e => setForm({ ...form, make: e.target.value })} /></div>
                  <div className="form-field"><label>Model *</label><input required value={form.model} onChange={e => setForm({ ...form, model: e.target.value })} /></div>
                  <div className="form-field"><label>Year</label><input type="number" value={form.year || ''} onChange={e => setForm({ ...form, year: +e.target.value })} /></div>
                  <div className="form-field"><label>Colour</label><input value={form.color} onChange={e => setForm({ ...form, color: e.target.value })} /></div>
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
