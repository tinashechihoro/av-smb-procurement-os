import { useEffect, useState } from 'react';
import { auditApi } from '../../api/services';
import { useTable } from '../../hooks/useTable';
import DataTable from '../../components/common/DataTable';
import TableToolbar from '../../components/common/TableToolbar';
import Pagination from '../../components/common/Pagination';
import type { AuditEntry } from '../../types';

export default function AuditTrail() {
  const [entries, setEntries] = useState<AuditEntry[]>([]);

  useEffect(() => { auditApi.list({ limit: '500' }).then(r => setEntries(r.data)).catch(() => {}); }, []);

  const table = useTable(entries, {
    searchKeys: ['action', 'entityType', 'entityId', 'userId'],
    defaultSort: { key: 'createdAt', direction: 'desc' },
    pageSize: 50,
  });

  const columns = [
    { key: 'createdAt', label: 'Timestamp', render: (e: AuditEntry) => new Date(e.createdAt).toLocaleString() },
    { key: 'action', label: 'Action', render: (e: AuditEntry) => <span className="chip" style={{ fontFamily: 'monospace' }}>{e.action}</span> },
    { key: 'entityType', label: 'Entity' },
    { key: 'entityId', label: 'Entity ID', render: (e: AuditEntry) => <span style={{ fontFamily: 'monospace', fontSize: 10 }}>{e.entityId?.substring(0, 8)}…</span> },
    { key: 'userId', label: 'User', render: (e: AuditEntry) => <span className="muted">{e.userId?.substring(0, 8)}…</span> },
  ];

  const csvColumns = [
    { key: 'createdAt', label: 'Timestamp' }, { key: 'action', label: 'Action' },
    { key: 'entityType', label: 'Entity' }, { key: 'entityId', label: 'Entity ID' }, { key: 'userId', label: 'User' },
  ];

  return (
    <div>
      <div className="page-head"><div><h2>Audit Trail</h2><p>Immutable system event log. Every material change recorded with user, timestamp, and before/after state.</p></div></div>

      <TableToolbar
        search={table.search} onSearchChange={table.setSearch} searchPlaceholder="Search action, entity, user…"
        total={table.total} entityName="events"
        activeFilterCount={table.activeFilterCount} onClearFilters={table.clearFilters}
        onExportCSV={() => table.exportCSV('audit-trail', csvColumns)}
      />

      <DataTable columns={columns} data={table.data} sort={table.sort} onSort={table.toggleSort} keyExtractor={(e) => String(e.id)} />
      <Pagination page={table.page} totalPages={table.totalPages} total={table.total} pageSize={50} onPageChange={table.setPage} />
    </div>
  );
}
