import { useEffect, useState } from 'react';
import { orderApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import DetailDrawer from '../../components/common/DetailDrawer';
import type { Order } from '../../types';

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [selected, setSelected] = useState<Order | null>(null);

  useEffect(() => { orderApi.list().then(r => setOrders(r.data)).catch(() => {}); }, []);

  const table = useTable(orders, {
    searchKeys: ['orderNumber', 'status'],
    defaultSort: { key: 'orderNumber', direction: 'desc' },
    pageSize: 25,
  });

  const statusClass = (s: string) => ({ DRAFT:'neutral', CONFIRMED:'info', PARTIAL_DELIVERY:'warning', DELIVERED:'success', CLOSED:'success', CANCELLED:'danger' }[s]||'neutral');
  const statusOptions = ['DRAFT','SUBMITTED','CONFIRMED','PARTIAL_DELIVERY','DELIVERED','INVOICED','CLOSED','CANCELLED'].map(s => ({ label: s, value: s }));

  const columns = [
    { key: 'orderNumber', label: 'Order', render: (o: Order) => <div className="row-main"><div className="record-icon">▣</div><div className="row-title">{o.orderNumber}</div></div> },
    { key: 'status', label: 'Status', render: (o: Order) => <span className={`status ${statusClass(o.status)}`}>{o.status}</span> },
    { key: 'items', label: 'Items', align: 'right' as const, render: (o: Order) => o.items?.length || 0 },
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (o: Order) => <span className="amount">${o.totalAmount?.toLocaleString()}</span> },
  ];

  const csvColumns = [
    { key: 'orderNumber', label: 'Order' }, { key: 'status', label: 'Status' },
    { key: 'totalAmount', label: 'Total' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Orders</h2><p>Approved purchase orders and fulfilment tracking.</p></div></div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search order…"
        filters={[{ key: 'status', label: 'Status', type: 'select', options: statusOptions, value: table.filters.status || '', onChange: (v) => table.setFilter('status', v) }]}
        total={table.total} entityName="orders"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('orders', csvColumns)}
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} onRowClick={(o) => setSelected(o)} keyExtractor={(o) => o.id} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={25} onPageChange={table.setPage} />

      <DetailDrawer
        open={!!selected} onClose={() => setSelected(null)}
        title={selected?.orderNumber || ''} subtitle="ORDER" icon="▣"
        fields={[
          { label: 'Status', value: selected ? <span className={`status ${statusClass(selected.status)}`}>{selected.status}</span> : '' },
          { label: 'Total', value: selected ? `$${selected.totalAmount?.toLocaleString()}` : '' },
          { label: 'Items', value: selected?.items?.length || 0 },
        ]}
      >
        {selected?.items && selected.items.length > 0 && (
          <div>
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Order Items</h3>
            <table className="data-table" style={{ minWidth: 'auto' }}>
              <thead><tr><th>#</th><th>Description</th><th>Qty</th><th>Unit $</th><th>Total</th><th>Delivered</th></tr></thead>
              <tbody>{selected.items.map((item, i) => (
                <tr key={i}><td>{item.lineNumber}</td><td>{item.description}</td><td>{item.quantity}</td><td className="amount">${item.unitPrice?.toLocaleString()}</td><td className="amount">${item.lineTotal?.toLocaleString()}</td><td>{item.deliveredQty}</td></tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </DetailDrawer>
    </div>
  );
}
