import { SortConfig } from '../../hooks/useTable';

interface Column<T> {
  key: string;
  label: string;
  sortable?: boolean;
  render?: (item: T) => React.ReactNode;
  width?: string;
  align?: 'left' | 'right' | 'center';
}

interface Props<T> {
  columns: Column<T>[];
  data: T[];
  sort: SortConfig | null;
  onSort: (key: string) => void;
  onRowClick?: (item: T) => void;
  emptyMessage?: string;
  keyExtractor: (item: T) => string;
}

export default function DataTable<T extends Record<string, any>>({
  columns, data, sort, onSort, onRowClick, emptyMessage = 'No data found', keyExtractor
}: Props<T>) {
  return (
    <div className="panel">
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              {columns.map(col => (
                <th
                  key={col.key}
                  style={{ cursor: col.sortable !== false ? 'pointer' : 'default', textAlign: col.align || 'left', width: col.width }}
                  onClick={() => col.sortable !== false && onSort(col.key)}
                >
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    {col.label}
                    {col.sortable !== false && sort?.key === col.key && (
                      <span style={{ fontSize: 10 }}>{sort.direction === 'asc' ? '▲' : '▼'}</span>
                    )}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.length > 0 ? data.map(item => (
              <tr
                key={keyExtractor(item)}
                onClick={() => onRowClick?.(item)}
                style={onRowClick ? { cursor: 'pointer' } : undefined}
              >
                {columns.map(col => (
                  <td key={col.key} style={{ textAlign: col.align || 'left' }}>
                    {col.render ? col.render(item) : item[col.key]}
                  </td>
                ))}
              </tr>
            )) : (
              <tr><td colSpan={columns.length} style={{ textAlign: 'center', padding: 30, color: '#111' }}>{emptyMessage}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
