import { useEffect, useState } from 'react';
import { supplierPoApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';

export default function SupplierPOs() {
  const [pos, setPos] = useState<any[]>([]);
  useEffect(() => { supplierPoApi.list().then(r => setPos(r.data)).catch(() => {}); }, []);

  const table = useTable(pos, { searchKeys: ['poNumber', 'status'], defaultSort: { key: 'poNumber', direction: 'desc' }, pageSize: 25 });

  const columns = [
    { key: 'poNumber', label: 'PO', render: (p: any) => <div className="row-main"><div className="record-icon">▣</div><div className="row-title">{p.poNumber}</div></div> },
    { key: 'status', label: 'Status', render: (p: any) => <span className="status info">{p.status}</span> },
    { key: 'orderDate', label: 'Date' },
    { key: 'items', label: 'Items', align: 'right' as const, render: (p: any) => p.items?.length || 0 },
    { key: 'subtotal', label: 'Subtotal', align: 'right' as const, render: (p: any) => <span className="amount">${p.subtotal?.toLocaleString()}</span> },
    { key: 'freightTotal', label: 'Freight', align: 'right' as const, render: (p: any) => <span className="muted">${p.freightTotal?.toLocaleString()}</span> },
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (p: any) => <span className="amount">${p.totalAmount?.toLocaleString()}</span> },
  ];

  const csvColumns = [{ key: 'poNumber', label: 'PO' }, { key: 'status', label: 'Status' }, { key: 'orderDate', label: 'Date' }, { key: 'subtotal', label: 'Subtotal' }, { key: 'freightTotal', label: 'Freight' }, { key: 'totalAmount', label: 'Total' }];

  return (
    <div>
      <div className="page-head"><div><h2>Supplier Purchase Orders</h2><p>SMB confidential — supplier sourcing and costs.</p></div></div>
      <TableToolbar search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search PO…" total={table.total} entityName="POs" activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters} onExportCSV={() => table.exportCSV('supplier-pos', csvColumns)} />
      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(p: any) => p.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
    </div>
  );
}
