import { useState, useMemo, useCallback } from 'react';

export interface SortConfig {
  key: string;
  direction: 'asc' | 'desc';
}

export interface FilterConfig {
  key: string;
  value: string;
  type: 'text' | 'select' | 'date' | 'daterange' | 'number';
  options?: { label: string; value: string }[];
}

export function useTable<T extends Record<string, any>>(
  data: T[],
  options?: {
    defaultSort?: SortConfig;
    filters?: FilterConfig[];
    searchKeys?: string[];
    pageSize?: number;
  }
) {
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortConfig | null>(options?.defaultSort || null);
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(0);
  const [pageSize] = useState(options?.pageSize || 25);

  const toggleSort = useCallback((key: string) => {
    setSort(prev => {
      if (prev?.key === key) {
        return prev.direction === 'asc' ? { key, direction: 'desc' } : null;
      }
      return { key, direction: 'asc' };
    });
    setPage(0);
  }, []);

  const setFilter = useCallback((key: string, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(0);
  }, []);

  const clearFilters = useCallback(() => {
    setFilters({});
    setSearch('');
    setPage(0);
  }, []);

  const filtered = useMemo(() => {
    let result = [...data];

    // Search
    if (search && options?.searchKeys) {
      const lower = search.toLowerCase();
      result = result.filter(item =>
        options.searchKeys!.some(key => {
          const val = item[key];
          return val != null && String(val).toLowerCase().includes(lower);
        })
      );
    }

    // Filters
    for (const [key, value] of Object.entries(filters)) {
      if (!value) continue;
      result = result.filter(item => {
        const val = item[key];
        if (val == null) return false;
        return String(val).toLowerCase() === value.toLowerCase();
      });
    }

    return result;
  }, [data, search, filters, options?.searchKeys]);

  const sorted = useMemo(() => {
    if (!sort) return filtered;
    return [...filtered].sort((a, b) => {
      const aVal = a[sort.key];
      const bVal = b[sort.key];
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sort.direction === 'asc' ? aVal - bVal : bVal - aVal;
      }
      const cmp = String(aVal).localeCompare(String(bVal));
      return sort.direction === 'asc' ? cmp : -cmp;
    });
  }, [filtered, sort]);

  const paginated = useMemo(() => {
    const start = page * pageSize;
    return sorted.slice(start, start + pageSize);
  }, [sorted, page, pageSize]);

  const exportCSV = useCallback((filename: string, columns: { key: string; label: string }[]) => {
    const header = columns.map(c => c.label).join(',');
    const rows = sorted.map(item =>
      columns.map(c => {
        const val = item[c.key];
        if (val == null) return '';
        const str = String(val);
        return str.includes(',') || str.includes('"') || str.includes('\n')
          ? `"${str.replace(/"/g, '""')}"` : str;
      }).join(',')
    );
    const csv = [header, ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [sorted]);

  const activeFilterCount = Object.values(filters).filter(v => v).length + (search ? 1 : 0);

  return {
    data: paginated,
    allFiltered: sorted,
    total: sorted.length,
    totalPages: Math.ceil(sorted.length / pageSize),
    page, setPage,
    search, setSearch,
    sort, toggleSort,
    filters, setFilter, clearFilters,
    activeFilterCount,
    exportCSV,
  };
}
