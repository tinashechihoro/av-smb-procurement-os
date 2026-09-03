interface Props {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, total, pageSize, onPageChange }: Props) {
  if (totalPages <= 1) return null;

  const start = page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, total);

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', borderTop: '1px solid #e3e8f0' }}>
      <span style={{ fontSize: 11, color: '#111' }}>
        Showing {start}–{end} of {total}
      </span>
      <div style={{ display: 'flex', gap: 4 }}>
        <button
          className="ghost-btn"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          style={{ padding: '4px 10px', fontSize: 11, opacity: page === 0 ? 0.4 : 1 }}
        >
          ← Prev
        </button>
        {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
          const pageNum = totalPages <= 5 ? i : Math.max(0, Math.min(page - 2, totalPages - 5)) + i;
          return (
            <button
              key={pageNum}
              className={pageNum === page ? 'primary-btn' : 'ghost-btn'}
              onClick={() => onPageChange(pageNum)}
              style={{ padding: '4px 8px', fontSize: 11, minWidth: 30 }}
            >
              {pageNum + 1}
            </button>
          );
        })}
        <button
          className="ghost-btn"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          style={{ padding: '4px 10px', fontSize: 11, opacity: page >= totalPages - 1 ? 0.4 : 1 }}
        >
          Next →
        </button>
      </div>
    </div>
  );
}
