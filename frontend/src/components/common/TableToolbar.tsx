interface FilterOption {
  key: string;
  label: string;
  type: 'select' | 'date' | 'text';
  options?: { label: string; value: string }[];
  value: string;
  onChange: (value: string) => void;
}

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder?: string;
  filters?: FilterOption[];
  activeFilterCount?: number;
  onClearFilters?: () => void;
  total: number;
  entityName: string;
  onExportCSV?: () => void;
  onCreateClick?: () => void;
  createLabel?: string;
}

export default function TableToolbar({
  search, onSearchChange, searchPlaceholder = 'Search…',
  filters, activeFilterCount, onClearFilters,
  total, entityName, onExportCSV, onCreateClick, createLabel = '+ New'
}: Props) {
  return (
    <>
      <div className="filter-bar context-filter-bar">
        <label className="global-search" style={{ minWidth: 220 }}>
          <span className="search-icon">⌕</span>
          <input
            placeholder={searchPlaceholder}
            value={search}
            onChange={e => onSearchChange(e.target.value)}
          />
        </label>

        {filters?.map(f => (
          <select
            key={f.key}
            className="filter-select"
            value={f.value}
            onChange={e => f.onChange(e.target.value)}
          >
            <option value="">{f.label}: All</option>
            {f.type === 'select' && f.options?.map(o => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        ))}

        {activeFilterCount ? (
          <button className="ghost-btn filter-clear" onClick={onClearFilters}>
            Reset ({activeFilterCount})
          </button>
        ) : null}

        <span className="filter-count muted">{total} {entityName}</span>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 6 }}>
          {onExportCSV && (
            <button className="ghost-btn" onClick={onExportCSV} style={{ fontSize: 10 }}>
              ⬇ CSV Export
            </button>
          )}
          {onCreateClick && (
            <button className="primary-btn" onClick={onCreateClick}>
              {createLabel}
            </button>
          )}
        </div>
      </div>
    </>
  );
}
