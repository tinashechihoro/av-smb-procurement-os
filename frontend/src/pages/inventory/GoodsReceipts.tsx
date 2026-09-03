import { useEffect, useState } from 'react';
import { goodsReceiptApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';

export default function GoodsReceipts() {
  const [receipts, setReceipts] = useState<any[]>([]);
  useEffect(() => { goodsReceiptApi.list().then(r => setReceipts(r.data)).catch(() => {}); }, []);

  const table = useTable(receipts, { searchKeys: ['receiptNumber', 'status'], defaultSort: { key: 'receivedAt', direction: 'desc' }, pageSize: 25 });

  const columns = [
    { key: 'receiptNumber', label: 'Receipt', render: (r: any) => <div className="row-main"><div className="record-icon">✓</div><div className="row-title">{r.receiptNumber}</div></div> },
    { key: 'status', label: 'Status', render: (r: any) => <span className="status info">{r.status}</span> },
    { key: 'receivedBy', label: 'Received By', render: (r: any) => <span className="muted">{r.receivedBy?.substring(0, 8)}…</span> },
    { key: 'receivedAt', label: 'Date', render: (r: any) => new Date(r.receivedAt).toLocaleDateString() },
  ];

  const csvColumns = [{ key: 'receiptNumber', label: 'Receipt' }, { key: 'status', label: 'Status' }, { key: 'receivedAt', label: 'Date' }];

  return (
    <div>
      <div className="page-head"><div><h2>Goods Receipts</h2><p>Incoming stock receiving and inspection.</p></div></div>
      <TableToolbar search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search receipt…" total={table.total} entityName="receipts" activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters} onExportCSV={() => table.exportCSV('goods-receipts', csvColumns)} />
      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(r: any) => r.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
    </div>
  );
}
