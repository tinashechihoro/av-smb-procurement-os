import { useEffect, useState } from 'react';
import { quotationApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { Quotation } from '../../types';
import toast from 'react-hot-toast';

export default function Quotations() {
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [selected, setSelected] = useState<Quotation | null>(null);

  const load = () => quotationApi.list().then(r => setQuotations(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(quotations, {
    searchKeys: ['quotationNumber', 'title', 'status'],
    defaultSort: { key: 'quotationNumber', direction: 'desc' },
    pageSize: 25,
  });

  const handleApprove = async (id: string) => { try { await quotationApi.approve(id); toast.success('Approved'); load(); } catch { toast.error('Failed'); } };
  const handleConvert = async (id: string) => { try { await quotationApi.convertToOrder(id); toast.success('Converted to order'); load(); } catch { toast.error('Failed'); } };

  const statusClass = (s: string) => ({ DRAFT:'neutral', SUBMITTED:'info', UNDER_NEGOTIATION:'warning', REVISED:'warning', APPROVED:'success', CONVERTED_TO_ORDER:'success', REJECTED:'danger', EXPIRED:'danger' }[s]||'neutral');
  const statusOptions = ['DRAFT','SUBMITTED','UNDER_NEGOTIATION','REVISED','APPROVED','REJECTED','CONVERTED_TO_ORDER','EXPIRED'].map(s => ({ label: s, value: s }));

  const totalQuoted = quotations.reduce((s,q) => s + q.totalAmount, 0);
  const approvedValue = quotations.filter(q => ['APPROVED','CONVERTED_TO_ORDER'].includes(q.status)).reduce((s,q) => s + q.totalAmount, 0);
  const pendingCount = quotations.filter(q => ['SUBMITTED','UNDER_NEGOTIATION','REVISED'].includes(q.status)).length;

  const columns = [
    { key: 'quotationNumber', label: 'Quotation', render: (q: Quotation) => <div className="row-main"><div className="record-icon">◫</div><div className="row-title">{q.quotationNumber}</div></div> },
    { key: 'status', label: 'Status', render: (q: Quotation) => <span className={`status ${statusClass(q.status)}`}>{q.status}</span> },
    { key: 'currentVersion', label: 'Version', render: (q: Quotation) => <span className="chip">v{q.currentVersion}</span> },
    { key: 'subtotal', label: 'Subtotal', align: 'right' as const, render: (q: Quotation) => <span className="amount">${q.subtotal?.toLocaleString()}</span> },
    { key: 'taxAmount', label: 'Tax', align: 'right' as const, render: (q: Quotation) => <span className="muted">${q.taxAmount?.toLocaleString()}</span> },
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (q: Quotation) => <span className="amount" style={{ fontWeight: 800 }}>${q.totalAmount?.toLocaleString()}</span> },
    { key: 'items', label: 'Lines', align: 'right' as const, render: (q: Quotation) => q.items?.length || 0 },
    { key: 'actions', label: 'Actions', sortable: false, render: (q: Quotation) => (
      <div style={{ display: 'flex', gap: 6 }}>
        {q.status === 'SUBMITTED' && <button className="ghost-btn" onClick={(e) => { e.stopPropagation(); handleApprove(q.id); }}>Approve</button>}
        {q.status === 'APPROVED' && <button className="primary-btn" onClick={(e) => { e.stopPropagation(); handleConvert(q.id); }}>→ Order</button>}
      </div>
    )},
  ];

  const csvColumns = [
    { key: 'quotationNumber', label: 'Quotation' }, { key: 'status', label: 'Status' },
    { key: 'currentVersion', label: 'Version' }, { key: 'subtotal', label: 'Subtotal' },
    { key: 'taxAmount', label: 'Tax' }, { key: 'totalAmount', label: 'Total' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Quotations</h2><p>Review, negotiate, and approve supplier quotations with version tracking.</p></div></div>

      <div className="kpi-grid" style={{ marginBottom: 14 }}>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Total Quoted</span><span className="kpi-icon">◫</span></div><div className="kpi-value">${totalQuoted.toLocaleString()}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Approved Value</span><span className="kpi-icon">✓</span></div><div className="kpi-value" style={{ color: '#0b6e52' }}>${approvedValue.toLocaleString()}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Pending Review</span><span className="kpi-icon">!</span></div><div className="kpi-value" style={{ color: '#9b6715' }}>{pendingCount}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Total Quotations</span><span className="kpi-icon">◫</span></div><div className="kpi-value">{quotations.length}</div></div>
      </div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search quotation…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="quotations"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('quotations', csvColumns)}
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(q) => setSelected(q)} keyExtractor={(q) => q.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.quotationNumber || ''} subtitle="QUOTATION" icon="◫"
        fields={[
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Version', value: selected ? `v${selected.currentVersion}` : '' },
          { label: 'Subtotal', value: selected ? `$${selected.subtotal?.toLocaleString()}` : '' },
          { label: 'Tax', value: selected ? `$${selected.taxAmount?.toLocaleString()}` : '' },
          { label: 'Total', value: selected ? <strong style={{ fontSize: 14 }}>${selected.totalAmount?.toLocaleString()}</strong> : '' },
        ]}
      >
        {selected?.items && selected.items.length > 0 && (
          <div>
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Quotation Lines</h3>
            <table className="data-table" style={{ minWidth: 'auto' }}>
              <thead><tr><th>#</th><th>Part</th><th>Description</th><th>Qty</th><th>Unit $</th><th>Line Total</th></tr></thead>
              <tbody>{selected.items.map((item: any, i: number) => (
                <tr key={i}><td>{item.lineNumber}</td><td>{item.partNumber || '—'}</td><td>{item.description}</td><td>{item.quantity}</td><td className="amount">${item.unitPrice?.toLocaleString()}</td><td className="amount">${item.lineTotal?.toLocaleString()}</td></tr>
              ))}</tbody>
              <tfoot><tr><td colSpan={5} style={{ textAlign: 'right', fontWeight: 700 }}>Total</td><td className="amount" style={{ fontWeight: 800 }}>${selected.totalAmount?.toLocaleString()}</td></tr></tfoot>
            </table>
          </div>
        )}
      </DetailDrawer>
    </div>
  );
}
