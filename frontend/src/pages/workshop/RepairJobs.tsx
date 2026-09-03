import { useEffect, useState } from 'react';
import { repairJobApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { RepairJob } from '../../types';

const STAGES = ['INITIAL_ASSESSMENT','ESTIMATING','AWAITING_AUTHORITY','PANEL_REPAIR','PRIMER_PREP','PAINT_PREP','PAINTING','CURING','REASSEMBLY','QC_INSPECTION','DETAILING','COMPLETED'];

export default function RepairJobs() {
  const [jobs, setJobs] = useState<RepairJob[]>([]);
  const [selected, setSelected] = useState<RepairJob | null>(null);
  const [view, setView] = useState<'table' | 'kanban'>('table');

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

  const priorityColor = (p: string) => {
    if (p === 'URGENT') return '#7f1d2d';
    if (p === 'HIGH') return '#9b6715';
    if (p === 'NORMAL') return '#0a4fc5';
    return '#111';
  };

  const statusOptions = ['ASSESSMENT','ESTIMATING','IN_REPAIR','PAINTING','REASSEMBLY','QC','COMPLETED','HANDED_OVER'].map(s => ({ label: s, value: s }));
  const priorityOptions = ['LOW','NORMAL','HIGH','URGENT'].map(s => ({ label: s, value: s }));

  const columns = [
    { key: 'jobNumber', label: 'Job #', render: (j: RepairJob) => <div className="row-main"><div className="record-icon">◈</div><div className="row-title">{j.jobNumber}</div></div> },
    { key: 'title', label: 'Title' },
    { key: 'status', label: 'Status', render: (j: RepairJob) => <span className={`status ${statusClass(j.status)}`}>{j.status}</span> },
    { key: 'repairStage', label: 'Stage', render: (j: RepairJob) => <span className="chip">{j.repairStage?.replace(/_/g, ' ')}</span> },
    { key: 'priority', label: 'Priority', render: (j: RepairJob) => <span style={{ color: priorityColor(j.priority), fontWeight: 700, fontSize: 11 }}>{j.priority}</span> },
    { key: 'assignedTechnician', label: 'Technician' },
    { key: 'estimatedTotal', label: 'Estimated', align: 'right' as const, render: (j: RepairJob) => <span className="amount">${j.estimatedTotal?.toLocaleString()}</span> },
    { key: 'actualTotal', label: 'Actual', align: 'right' as const, render: (j: RepairJob) => <span className="amount">${j.actualTotal?.toLocaleString()}</span> },
  ];

  const csvColumns = [
    { key: 'jobNumber', label: 'Job' }, { key: 'title', label: 'Title' }, { key: 'status', label: 'Status' },
    { key: 'repairStage', label: 'Stage' }, { key: 'priority', label: 'Priority' },
    { key: 'assignedTechnician', label: 'Technician' },
    { key: 'estimatedTotal', label: 'Estimated' }, { key: 'actualTotal', label: 'Actual' },
  ];

  const stageCounts = STAGES.map(stage => ({
    stage,
    count: jobs.filter(j => j.repairStage === stage).length,
    jobs: jobs.filter(j => j.repairStage === stage),
  }));

  return (
    <div>
      <div className="page-head">
        <div><h2>Repair Jobs</h2><p>Panel shop case files with stage tracking, damage assessment, and QC gates.</p></div>
        <div className="head-actions">
          <button className={view === 'table' ? 'primary-btn' : 'ghost-btn'} onClick={() => setView('table')}>Table</button>
          <button className={view === 'kanban' ? 'primary-btn' : 'ghost-btn'} onClick={() => setView('kanban')}>Kanban</button>
        </div>
      </div>

      {view === 'table' ? (
        <>
          <TableToolbar
            search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search job #, title, technician…"
            filters={[
              { key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) },
              { key: 'priority', label: 'Priority', type: 'select', options: priorityOptions, value: table.filters.priority || '', onChange: (v) => table.setFilter('priority', v) },
            ]}
            total={table.total} entityName="jobs"
            activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
            onExportCSV={() => table.exportCSV('repair-jobs', csvColumns)}
          />
          <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(j) => setSelected(j)} keyExtractor={(j) => j.id} />
          <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
        </>
      ) : (
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 16 }}>
          {stageCounts.filter(s => s.count > 0 || STAGES.indexOf(s.stage) < 6).map(({ stage, count, jobs: stageJobs }) => (
            <div key={stage} style={{ minWidth: 200, flex: '0 0 200px', background: '#f8faff', border: '1px solid #e3e8f0', borderRadius: 12, padding: 10 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <strong style={{ fontSize: 11, color: '#000' }}>{stage.replace(/_/g, ' ')}</strong>
                <span className="chip">{count}</span>
              </div>
              {stageJobs.map(j => (
                <div key={j.id} onClick={() => setSelected(j)} style={{ background: '#fff', border: '1px solid #e3e8f0', borderRadius: 8, padding: 8, marginBottom: 6, cursor: 'pointer', transition: '.15s' }}
                  onMouseOver={e => (e.currentTarget.style.borderColor = '#0a4fc5')}
                  onMouseOut={e => (e.currentTarget.style.borderColor = '#e3e8f0')}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: '#000' }}>{j.jobNumber}</div>
                  <div style={{ fontSize: 10, color: '#111', marginTop: 2 }}>{j.title}</div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 10 }}>
                    <span style={{ color: priorityColor(j.priority), fontWeight: 700 }}>{j.priority}</span>
                    <span className="amount">${j.estimatedTotal?.toLocaleString()}</span>
                  </div>
                </div>
              ))}
              {count === 0 && <div style={{ fontSize: 10, color: '#111', textAlign: 'center', padding: 12 }}>No jobs</div>}
            </div>
          ))}
        </div>
      )}

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.jobNumber || ''} subtitle="REPAIR JOB" icon="◈"
        fields={[
          { label: 'Title', value: selected?.title, full: true },
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Stage', value: selected?.repairStage?.replace(/_/g, ' ') },
          { label: 'Priority', value: selected ? <span style={{ color: priorityColor(selected.priority), fontWeight: 700 }}>{selected.priority}</span> : '' },
          { label: 'Technician', value: selected?.assignedTechnician || '—' },
          { label: 'Estimated', value: selected ? `$${selected.estimatedTotal?.toLocaleString()}` : '' },
          { label: 'Actual', value: selected ? `$${selected.actualTotal?.toLocaleString()}` : '' },
          { label: 'Variance', value: selected ? <span style={{ color: ((selected.actualTotal||0) - (selected.estimatedTotal||0)) > 0 ? '#7f1d2d' : '#0b6e52' }}>${((selected.actualTotal||0) - (selected.estimatedTotal||0)).toLocaleString()}</span> : '' },
        ]}
      >
        {selected && (
          <div>
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Repair Stage Progress</h3>
            <div style={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
              {STAGES.map((stage, i) => {
                const currentIdx = STAGES.indexOf(selected.repairStage || '');
                const isComplete = i <= currentIdx;
                const isCurrent = stage === selected.repairStage;
                return (
                  <div key={stage} style={{
                    padding: '4px 8px', borderRadius: 6, fontSize: 9, fontWeight: 700,
                    background: isCurrent ? '#0a4fc5' : isComplete ? '#d8eee6' : '#f3f6fb',
                    color: isCurrent ? '#fff' : isComplete ? '#0b6e52' : '#111',
                    border: isCurrent ? '2px solid #0a4fc5' : '1px solid #e3e8f0',
                  }}>
                    {isComplete ? '✓ ' : ''}{stage.replace(/_/g, ' ')}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </DetailDrawer>
    </div>
  );
}
