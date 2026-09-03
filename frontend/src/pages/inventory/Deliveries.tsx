import { useEffect, useState } from 'react';
import { deliveryApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';

export default function Deliveries() {
  const [deliveries, setDeliveries] = useState<any[]>([]);
  useEffect(() => { deliveryApi.list().then(r => setDeliveries(r.data)).catch(() => {}); }, []);

  const table = useTable(deliveries, { searchKeys: ['deliveryNumber', 'status', 'deliveredBy', 'vehicleReg'], defaultSort: { key: 'deliveryDate', direction: 'desc' }, pageSize: 25 });

  const columns = [
    { key: 'deliveryNumber', label: 'Delivery', render: (d: any) => <div className="row-main"><div className="record-icon">⇢</div><div className="row-title">{d.deliveryNumber}</div></div> },
    { key: 'status', label: 'Status', render: (d: any) => <span className="status info">{d.status}</span> },
    { key: 'deliveryDate', label: 'Date' },
    { key: 'deliveredBy', label: 'Delivered By' },
    { key: 'vehicleReg', label: 'Vehicle' },
  ];

  const csvColumns = [{ key: 'deliveryNumber', label: 'Delivery' }, { key: 'status', label: 'Status' }, { key: 'deliveryDate', label: 'Date' }, { key: 'deliveredBy', label: 'By' }, { key: 'vehicleReg', label: 'Vehicle' }];

  return (
    <div>
      <div className="page-head"><div><h2>Deliveries</h2><p>Outbound delivery notes and receipt tracking.</p></div></div>
      <TableToolbar search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search delivery…" total={table.total} entityName="deliveries" activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters} onExportCSV={() => table.exportCSV('deliveries', csvColumns)} />
      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(d: any) => d.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />
    </div>
  );
}
