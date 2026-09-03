import { useEffect, useState } from 'react';
import { quotationApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { Quotation } from '../../types';
import toast from 'react-hot-toast';

export default function Quotations() {
  const [quotations, setQuotations] = useState<Quotation[]>([]);

  const load = () => quotationApi.list().then(r => setQuotations(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);

  const table = useTable(quotations, {
    searchKeys: ['quotationNumber', 'title', 'status'],
    defaultSort: { key: 'quotationNumber', direction: 'desc' },
    pageSize: 25,
  });

  const handleApprove = async (id: string) => { try { await quotationApi.approve(id); toast.success('Approved'); load(); } catch { toast.error('Failed'); } };
  const handleConvert = async (id: string) => { try { await quotationApi.convertToOrder(id); toast.success('Converted'); load(); } catch { toast.error('Failed'); } };

  const statusClass = (s: string) => ({ DRAFT:'neutral', SUBMITTED:'info', UNDER_NEGOTIATION:'warning', APPROVED:'success', CONVERTED_TO_ORDER:'success', REJECTED:'danger' }[s]||'neutral');
  const statusOptions = ['DRAFT','SUBMITTED','UNDER_NEGOTIATION','REVISED','APPROVED','REJECTED','CONVERTED_TO_ORDER','EXPIRED'].map(s => ({ label: s, value: s }));

  const columns = [
    { key: 'quotationNumber', label: 'Quotation', render: (q: Quotation) => <div className="row-main"><div className="record-icon">◫</div><div className="row-title">{q.quotationNumber}</div></div> },
    { key: 'status', label: 'Status', render: (q: Quotation) => <span className={`status ${statusClass(q.status)}`}>{q.status}</span> },
    { key: 'currentVersion', label: 'Version', render: (q: Quotation) => <span className="chip">v{q.currentVersion}</span> },
    { key: 'subtotal', label: 'Subtotal', align: 'right' as const, render: (q: Quotation) => <span className="amount">${q.subtotal?.toLocaleString()}</span> },
    { key: 'taxAmount', label: 'Tax', align: 'right' as const, render: (q: Quotation) => <span className="muted">${q.taxAmount?.toLocaleString()}</span> },
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (q: Quotation) => <span className="amount">${q.totalAmount?.toLocaleString()}</span> },
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
      <div className="page-head"><div><h2>Quotations</h2><p>Review, negotiate, and approve supplier quotations.</p></div></div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search quotation…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="quotations"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('quotations', csvColumns)}
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(q) => q.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
    </div>
  );
}
