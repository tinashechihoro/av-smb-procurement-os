import { useEffect, useState } from 'react';
import { repairJobApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { RepairJob } from '../../types';

export default function RepairJobs() {
  const [jobs, setJobs] = useState<RepairJob[]>([]);
  const [selected, setSelected] = useState<RepairJob | null>(null);

  useEffect(() => { repairJobApi.list().then(r => setJobs(r.data)).catch(() => {}); }, []);

  const table = useTable(jobs, {
    searchKeys: ['jobNumber', 'title', 'status', 'repairStage', 'priority', 'assignedTechnician'],
    defaultSort: { key: 'jobNumber', direction: 'desc' },
    pageSize: 25,
  });

  const statusClass = (s: string) => {
    const v = s.toLowerCase();
    if (v.includes('completed') || v.includes('handed')) return 'success';
    if (v.includes('qc') || v.includes('painting')) return 'warning';
    if (v.includes('repair') || v.includes('reassembly')) return 'info';
    return 'neutral';
  };

  const statusOptions = ['ASSESSMENT','ESTIMATING','IN_REPAIR','PAINTING','REASSEMBLY','QC','COMPLETED','HANDED_OVER'].map(s => ({ label: s, value: s }));

  const columns = [
    { key: 'jobNumber', label: 'Job', render: (j: RepairJob) => <div className="row-main"><div className="record-icon">◈</div><div className="row-title">{j.jobNumber}</div></div> },
    { key: 'title', label: 'Title' },
    { key: 'status', label: 'Status', render: (j: RepairJob) => <span className={`status ${statusClass(j.status)}`}>{j.status}</span> },
    { key: 'repairStage', label: 'Stage', render: (j: RepairJob) => <span className="chip">{j.repairStage}</span> },
    { key: 'priority', label: 'Priority', render: (j: RepairJob) => <span className="chip">{j.priority}</span> },
    { key: 'assignedTechnician', label: 'Technician' },
    { key: 'estimatedTotal', label: 'Estimated', align: 'right' as const, render: (j: RepairJob) => <span className="amount">${j.estimatedTotal?.toLocaleString()}</span> },
    { key: 'actualTotal', label: 'Actual', align: 'right' as const, render: (j: RepairJob) => <span className="amount">${j.actualTotal?.toLocaleString()}</span> },
  ];

  const csvColumns = [
    { key: 'jobNumber', label: 'Job' }, { key: 'title', label: 'Title' }, { key: 'status', label: 'Status' },
    { key: 'repairStage', label: 'Stage' }, { key: 'priority', label: 'Priority' },
    { key: 'estimatedTotal', label: 'Estimated' }, { key: 'actualTotal', label: 'Actual' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Repair Jobs</h2><p>Panel shop case files with damage assessment, operations, and QC.</p></div></div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search job #, title, technician…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="jobs"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('repair-jobs', csvColumns)}
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(j) => setSelected(j)} keyExtractor={(j) => j.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.jobNumber || ''} subtitle="REPAIR JOB" icon="◈"
        fields={[
          { label: 'Title', value: selected?.title, full: true },
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Stage', value: selected?.repairStage }, { label: 'Priority', value: selected?.priority },
          { label: 'Technician', value: selected?.assignedTechnician || '—' },
          { label: 'Estimated', value: selected ? `$${selected.estimatedTotal?.toLocaleString()}` : '' },
          { label: 'Actual', value: selected ? `$${selected.actualTotal?.toLocaleString()}` : '' },
          { label: 'Variance', value: selected ? `$${((selected.actualTotal || 0) - (selected.estimatedTotal || 0)).toLocaleString()}` : '' },
        ]}
      />
    </div>
  );
}
