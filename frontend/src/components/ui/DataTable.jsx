import { useState, useMemo } from 'react';
import { ChevronUp, ChevronDown, AlertCircle, RefreshCw } from 'lucide-react';

function SkeletonRows({ columns, pageSize }) {
  return (
    <tbody>
      {Array.from({ length: pageSize }).map((_, i) => (
        <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
          {columns.map((col) => (
            <td key={col.key} style={{ padding: '14px 16px' }}>
              <div
                style={{
                  height: 14,
                  borderRadius: 4,
                  background: 'var(--bg-hover)',
                  animation: 'pulse 1.5s ease-in-out infinite',
                  width: `${50 + Math.random() * 40}%`,
                }}
              />
            </td>
          ))}
        </tr>
      ))}
      <style>{`@keyframes pulse { 0%,100%{opacity:.4} 50%{opacity:.8} }`}</style>
    </tbody>
  );
}

export default function DataTable({
  columns = [],
  data = [],
  loading = false,
  error = null,
  onRetry,
  emptyMessage = 'No data available',
  emptyIcon,
  onRowClick,
  pageSize = 10,
}) {
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');
  const [currentPage, setCurrentPage] = useState(1);

  const handleSort = (key) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
    setCurrentPage(1);
  };

  const sortedData = useMemo(() => {
    if (!sortKey) return data;
    return [...data].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
      }
      const cmp = String(aVal).localeCompare(String(bVal));
      return sortDir === 'asc' ? cmp : -cmp;
    });
  }, [data, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sortedData.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const paginatedData = sortedData.slice((safePage - 1) * pageSize, safePage * pageSize);

  if (error) {
    return (
      <div className="empty-state">
        <div style={{ color: 'var(--red)', marginBottom: 12 }}>
          <AlertCircle size={40} />
        </div>
        <div className="empty-title">Something went wrong</div>
        <div className="empty-subtitle" style={{ marginBottom: 16 }}>{error}</div>
        {onRetry && (
          <button className="btn btn-secondary btn-sm" onClick={onRetry}>
            <RefreshCw size={14} /> Retry
          </button>
        )}
      </div>
    );
  }

  if (!loading && paginatedData.length === 0) {
    return (
      <div className="empty-state">
        {emptyIcon && <div className="empty-icon">{emptyIcon}</div>}
        <div className="empty-title">{emptyMessage}</div>
      </div>
    );
  }

  return (
    <div>
      <div className="table-container">
        <table>
          <thead>
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  onClick={col.sortable !== false ? () => handleSort(col.key) : undefined}
                  style={{
                    cursor: col.sortable !== false ? 'pointer' : 'default',
                    userSelect: 'none',
                    whiteSpace: 'nowrap',
                  }}
                >
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    {col.label}
                    {col.sortable !== false && sortKey === col.key && (
                      sortDir === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                    )}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          {loading ? (
            <SkeletonRows columns={columns} pageSize={pageSize} />
          ) : (
            <tbody>
              {paginatedData.map((row, idx) => (
                <tr
                  key={row.id ?? idx}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  style={{ cursor: onRowClick ? 'pointer' : 'default' }}
                >
                  {columns.map((col) => (
                    <td key={col.key}>
                      {col.render ? col.render(row[col.key], row) : (row[col.key] ?? '—')}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          )}
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="page-btn"
            disabled={safePage <= 1}
            onClick={() => setCurrentPage((p) => p - 1)}
          >
            ‹
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1)
            .filter((p) => p === 1 || p === totalPages || Math.abs(p - safePage) <= 2)
            .reduce((acc, p, i, arr) => {
              if (i > 0 && p - arr[i - 1] > 1) acc.push('...');
              acc.push(p);
              return acc;
            }, [])
            .map((p, i) =>
              p === '...' ? (
                <span key={`dots-${i}`} style={{ color: 'var(--text-muted)', padding: '0 4px' }}>…</span>
              ) : (
                <button
                  key={p}
                  className={`page-btn ${p === safePage ? 'active' : ''}`}
                  onClick={() => setCurrentPage(p)}
                >
                  {p}
                </button>
              )
            )}
          <button
            className="page-btn"
            disabled={safePage >= totalPages}
            onClick={() => setCurrentPage((p) => p + 1)}
          >
            ›
          </button>
        </div>
      )}
    </div>
  );
}
