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

  const statusClass = (s: string) => ({ DRAFT:'neutral', SUBMITTED:'info', CONFIRMED:'info', PARTIAL_DELIVERY:'warning', DELIVERED:'success', INVOICED:'accent', CLOSED:'success', CANCELLED:'danger' }[s]||'neutral');
  const statusOptions = ['DRAFT','SUBMITTED','CONFIRMED','PARTIAL_DELIVERY','DELIVERED','INVOICED','CLOSED','CANCELLED'].map(s => ({ label: s, value: s }));

  const totalValue = orders.reduce((s,o) => s + o.totalAmount, 0);
  const activeValue = orders.filter(o => ['CONFIRMED','PARTIAL_DELIVERY'].includes(o.status)).reduce((s,o) => s + o.totalAmount, 0);
  const deliveredCount = orders.filter(o => ['DELIVERED','INVOICED','CLOSED'].includes(o.status)).length;

  const columns = [
    { key: 'orderNumber', label: 'Order', render: (o: Order) => <div className="row-main"><div className="record-icon">▣</div><div className="row-title">{o.orderNumber}</div></div> },
    { key: 'status', label: 'Status', render: (o: Order) => <span className={`status ${statusClass(o.status)}`}>{o.status}</span> },
    { key: 'items', label: 'Items', align: 'right' as const, render: (o: Order) => o.items?.length || 0 },
    { key: 'fulfilment', label: 'Fulfilment', render: (o: Order) => {
      const totalQty = o.items?.reduce((s,i) => s + i.quantity, 0) || 0;
      const deliveredQty = o.items?.reduce((s,i) => s + i.deliveredQty, 0) || 0;
      const pct = totalQty > 0 ? Math.round((deliveredQty / totalQty) * 100) : 0;
      return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{ width: 60, height: 6, background: '#e3e8f0', borderRadius: 3, overflow: 'hidden' }}>
            <div style={{ width: `${pct}%`, height: '100%', background: pct === 100 ? '#0b6e52' : '#0a4fc5', borderRadius: 3 }} />
          </div>
          <span style={{ fontSize: 10, color: '#111' }}>{pct}%</span>
        </div>
      );
    }},
    { key: 'totalAmount', label: 'Total', align: 'right' as const, render: (o: Order) => <span className="amount">${o.totalAmount?.toLocaleString()}</span> },
  ];

  const csvColumns = [
    { key: 'orderNumber', label: 'Order' }, { key: 'status', label: 'Status' }, { key: 'totalAmount', label: 'Total' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Orders</h2><p>Approved purchase orders with fulfilment tracking and delivery progress.</p></div></div>

      <div className="kpi-grid" style={{ marginBottom: 14 }}>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Total Value</span><span className="kpi-icon">▣</span></div><div className="kpi-value">${totalValue.toLocaleString()}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">In Fulfilment</span><span className="kpi-icon">⇢</span></div><div className="kpi-value" style={{ color: '#0a4fc5' }}>${activeValue.toLocaleString()}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Delivered</span><span className="kpi-icon">✓</span></div><div className="kpi-value" style={{ color: '#0b6e52' }}>{deliveredCount}</div></div>
        <div className="kpi-card"><div className="kpi-top"><span className="kpi-label">Total Orders</span><span className="kpi-icon">▣</span></div><div className="kpi-value">{orders.length}</div></div>
      </div>

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
            <h3 style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Order Items — Fulfilment</h3>
            <table className="data-table" style={{ minWidth: 'auto' }}>
              <thead><tr><th>#</th><th>Description</th><th>Ordered</th><th>Delivered</th><th>Progress</th><th>Unit $</th><th>Total</th></tr></thead>
              <tbody>{selected.items.map((item, i) => {
                const pct = item.quantity > 0 ? Math.round((item.deliveredQty / item.quantity) * 100) : 0;
                return (
                  <tr key={i}>
                    <td>{item.lineNumber}</td><td>{item.description}</td>
                    <td>{item.quantity}</td><td>{item.deliveredQty}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <div style={{ width: 50, height: 5, background: '#e3e8f0', borderRadius: 3, overflow: 'hidden' }}>
                          <div style={{ width: `${pct}%`, height: '100%', background: pct === 100 ? '#0b6e52' : '#0a4fc5', borderRadius: 3 }} />
                        </div>
                        <span style={{ fontSize: 9, color: '#111' }}>{pct}%</span>
                      </div>
                    </td>
                    <td className="amount">${item.unitPrice?.toLocaleString()}</td>
                    <td className="amount">${item.lineTotal?.toLocaleString()}</td>
                  </tr>
                );
              })}</tbody>
            </table>
          </div>
        )}
      </DetailDrawer>
    </div>
  );
}
