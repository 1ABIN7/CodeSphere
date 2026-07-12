import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { problemsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const DIFFICULTY_FILTERS = ['ALL', 'EASY', 'MEDIUM', 'HARD'];
const SORT_OPTIONS = [
  { value: 'id', label: 'Default' },
  { value: 'title', label: 'Title' },
  { value: 'difficulty', label: 'Difficulty' },
  { value: 'acceptance', label: 'Acceptance Rate' },
  { value: 'popular', label: 'Most Popular' },
];

function DifficultyBadge({ difficulty }) {
  const cls = difficulty === 'EASY' ? 'badge-easy' : difficulty === 'MEDIUM' ? 'badge-medium' : 'badge-hard';
  return <span className={`badge ${cls}`}>{difficulty}</span>;
}

function AcceptanceBar({ rate }) {
  if (rate == null) return <span className="text-muted">—</span>;
  const pct = parseFloat(rate).toFixed(1);
  const color = pct >= 60 ? 'var(--green)' : pct >= 40 ? 'var(--yellow)' : 'var(--red)';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <div style={{ width: 50, height: 4, background: 'var(--bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 2 }} />
      </div>
      <span style={{ fontSize: 13, color: 'var(--text-secondary)', minWidth: 40 }}>{pct}%</span>
    </div>
  );
}

export default function ProblemsPage() {
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState('ALL');
  const [sortBy, setSortBy] = useState('id');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const fetchProblems = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 20, sortBy };
      if (search.trim()) params.search = search.trim();
      if (difficulty !== 'ALL') params.difficulty = difficulty;
      const res = await problemsAPI.list(params);
      setProblems(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setTotalElements(res.data.totalElements || 0);
    } catch {
      toast.error('Failed to load problems. Make sure the backend is running.');
      // Show mock data for demo
      setProblems(MOCK_PROBLEMS);
      setTotalPages(2);
      setTotalElements(25);
    } finally {
      setLoading(false);
    }
  }, [page, search, difficulty, sortBy]);

  useEffect(() => {
    const timer = setTimeout(fetchProblems, 300);
    return () => clearTimeout(timer);
  }, [fetchProblems]);

  const handleSearchChange = (e) => { setSearch(e.target.value); setPage(0); };
  const handleDifficultyChange = (d) => { setDifficulty(d); setPage(0); };

  return (
    <div className="container fade-in">
      <div className="page-header">
        <h1 className="page-title">Problem Bank</h1>
        <p className="page-subtitle">{totalElements} problems to master your coding skills</p>
      </div>

      {/* Filters */}
      <div className="filters">
        <div className="filter-search">
          <input
            className="input w-full"
            placeholder="🔍 Search problems..."
            value={search}
            onChange={handleSearchChange}
          />
        </div>

        <div className="filter-tabs">
          {DIFFICULTY_FILTERS.map(d => (
            <button
              key={d}
              className={`filter-tab ${difficulty === d ? 'active' : ''}`}
              onClick={() => handleDifficultyChange(d)}
            >
              {d}
            </button>
          ))}
        </div>

        <select className="select" value={sortBy} onChange={(e) => setSortBy(e.target.value)} style={{ minWidth: 160 }}>
          {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      {/* Stats Bar */}
      <div style={{ display: 'flex', gap: 24, marginBottom: 20 }}>
        {['EASY', 'MEDIUM', 'HARD'].map(d => {
          const count = problems.filter(p => p.difficulty === d).length;
          const cls = d === 'EASY' ? 'badge-easy' : d === 'MEDIUM' ? 'badge-medium' : 'badge-hard';
          return (
            <div key={d} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span className={`badge ${cls}`}>{d}</span>
              <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{count}</span>
            </div>
          );
        })}
      </div>

      {/* Table */}
      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 60 }}>#</th>
                  <th>Title</th>
                  <th style={{ width: 100 }}>Difficulty</th>
                  <th style={{ width: 140 }}>Acceptance</th>
                  <th style={{ width: 80 }}>Submissions</th>
                  <th style={{ width: 80 }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {problems.map((p, i) => (
                  <tr
                    key={p.id}
                    onClick={() => navigate(`/problems/${p.id}`)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td style={{ color: 'var(--text-muted)', fontWeight: 500 }}>
                      {page * 20 + i + 1}
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <span style={{ fontWeight: 600, fontSize: 14 }}>{p.title}</span>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          {(p.tags || []).slice(0, 3).map(t => (
                            <span key={t} className="badge badge-tag">{t}</span>
                          ))}
                        </div>
                      </div>
                    </td>
                    <td><DifficultyBadge difficulty={p.difficulty} /></td>
                    <td><AcceptanceBar rate={p.acceptanceRate} /></td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                      {p.totalSubmissions ?? 0}
                    </td>
                    <td>
                      {p.solvedByCurrentUser
                        ? <span style={{ color: 'var(--green)', fontSize: 18 }}>✓</span>
                        : <span style={{ color: 'var(--text-muted)', fontSize: 16 }}>○</span>}
                    </td>
                  </tr>
                ))}
                {problems.length === 0 && (
                  <tr>
                    <td colSpan={6}>
                      <div className="empty-state">
                        <div className="empty-icon">🔍</div>
                        <div className="empty-title">No problems found</div>
                        <div className="empty-subtitle">Try adjusting your search or filters</div>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pagination">
              <button className="page-btn" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>‹</button>
              {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => (
                <button
                  key={i}
                  className={`page-btn ${page === i ? 'active' : ''}`}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))}
              <button className="page-btn" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>›</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

// Mock data shown when backend is offline
const MOCK_PROBLEMS = [
  { id: 1, title: 'Two Sum', difficulty: 'EASY', acceptanceRate: '65.2', totalSubmissions: 1240, tags: ['array', 'hash-map'], solvedByCurrentUser: false },
  { id: 2, title: 'Reverse String', difficulty: 'EASY', acceptanceRate: '78.4', totalSubmissions: 890, tags: ['string', 'two-pointers'], solvedByCurrentUser: false },
  { id: 3, title: 'FizzBuzz', difficulty: 'EASY', acceptanceRate: '85.1', totalSubmissions: 743, tags: ['math', 'string'], solvedByCurrentUser: false },
  { id: 4, title: 'Palindrome Check', difficulty: 'EASY', acceptanceRate: '72.3', totalSubmissions: 621, tags: ['string', 'two-pointers'], solvedByCurrentUser: false },
  { id: 5, title: 'Valid Parentheses', difficulty: 'MEDIUM', acceptanceRate: '58.7', totalSubmissions: 987, tags: ['stack', 'string'], solvedByCurrentUser: false },
  { id: 6, title: 'Binary Search', difficulty: 'MEDIUM', acceptanceRate: '64.9', totalSubmissions: 754, tags: ['binary-search', 'array'], solvedByCurrentUser: false },
  { id: 7, title: 'Longest Substring Without Repeating Characters', difficulty: 'MEDIUM', acceptanceRate: '49.2', totalSubmissions: 1103, tags: ['sliding-window', 'hash-map'], solvedByCurrentUser: false },
  { id: 8, title: 'Container With Most Water', difficulty: 'MEDIUM', acceptanceRate: '55.1', totalSubmissions: 832, tags: ['two-pointers', 'greedy'], solvedByCurrentUser: false },
  { id: 9, title: 'Trapping Rain Water', difficulty: 'HARD', acceptanceRate: '41.3', totalSubmissions: 671, tags: ['two-pointers', 'dynamic-programming'], solvedByCurrentUser: false },
  { id: 10, title: 'N-Queens', difficulty: 'HARD', acceptanceRate: '32.8', totalSubmissions: 412, tags: ['backtracking', 'recursion'], solvedByCurrentUser: false },
  { id: 11, title: 'Sliding Window Maximum', difficulty: 'HARD', acceptanceRate: '38.5', totalSubmissions: 523, tags: ['deque', 'sliding-window'], solvedByCurrentUser: false },
  { id: 12, title: 'Minimum Window Substring', difficulty: 'HARD', acceptanceRate: '29.7', totalSubmissions: 389, tags: ['sliding-window', 'hash-map'], solvedByCurrentUser: false },
];
