import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { submissionsAPI } from '../api';
import { useAuth } from '../context/AuthContext';

const STATUS_META = {
  ACCEPTED: { label: 'Accepted', cls: 'badge-accepted', emoji: '✅' },
  WRONG_ANSWER: { label: 'Wrong Answer', cls: 'badge-wrong', emoji: '❌' },
  TIME_LIMIT_EXCEEDED: { label: 'TLE', cls: 'badge-pending', emoji: '⏱' },
  RUNTIME_ERROR: { label: 'Runtime Error', cls: 'badge-wrong', emoji: '💥' },
  COMPILATION_ERROR: { label: 'Compile Error', cls: 'badge-wrong', emoji: '⚙' },
  PARTIALLY_ACCEPTED: { label: 'Partial', cls: 'badge-pending', emoji: '🟡' },
};

export default function SubmissionsPage() {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    const fetch = async () => {
      setLoading(true);
      try {
        const res = await submissionsAPI.getMySubmissions({ page, size: 20 });
        setSubmissions(res.data.content || []);
        setTotalPages(res.data.totalPages || 1);
      } catch {
        setSubmissions(MOCK_SUBS);
        setTotalPages(1);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [page, isLoggedIn]);

  return (
    <div className="container fade-in">
      <div className="page-header">
        <h1 className="page-title">My Submissions</h1>
        <p className="page-subtitle">Your complete submission history</p>
      </div>

      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Problem</th>
                  <th>Status</th>
                  <th>Language</th>
                  <th>Score</th>
                  <th>Time</th>
                  <th>Tests Passed</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((s, i) => (
                  <tr key={s.id} onClick={() => navigate(`/problems/${s.problemId}`)} style={{ cursor: 'pointer' }}>
                    <td style={{ color: 'var(--text-muted)', fontWeight: 500 }}>{page * 20 + i + 1}</td>
                    <td style={{ fontWeight: 600, maxWidth: 240 }} className="truncate">
                      {s.problemTitle || `Problem #${s.problemId}`}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span>{STATUS_META[s.status]?.emoji}</span>
                        <span className={`badge ${STATUS_META[s.status]?.cls || 'badge-default'}`}>
                          {STATUS_META[s.status]?.label || s.status}
                        </span>
                      </div>
                    </td>
                    <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{s.language}</td>
                    <td>
                      {s.score != null
                        ? <span style={{ color: s.score === 100 ? 'var(--green)' : 'var(--yellow)', fontWeight: 700 }}>{s.score}/100</span>
                        : '—'}
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                      {s.execTime ? `${s.execTime}ms` : '—'}
                    </td>
                    <td style={{ fontSize: 13 }}>
                      {s.testCasesPassed != null
                        ? <span style={{ color: s.testCasesPassed === s.totalTestCases ? 'var(--green)' : 'var(--text-secondary)' }}>
                            {s.testCasesPassed}/{s.totalTestCases}
                          </span>
                        : '—'}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {s.createdAt ? new Date(s.createdAt).toLocaleDateString() : 'Today'}
                    </td>
                  </tr>
                ))}
                {submissions.length === 0 && (
                  <tr>
                    <td colSpan={8}>
                      <div className="empty-state">
                        <div className="empty-icon">📬</div>
                        <div className="empty-title">No submissions yet</div>
                        <div className="empty-subtitle">
                          <Link to="/problems" className="auth-link">Browse problems and start coding →</Link>
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button className="page-btn" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>‹</button>
              {Array.from({ length: totalPages }, (_, i) => (
                <button key={i} className={`page-btn ${page === i ? 'active' : ''}`} onClick={() => setPage(i)}>{i + 1}</button>
              ))}
              <button className="page-btn" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>›</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

const MOCK_SUBS = [
  { id: 1, problemId: 1, problemTitle: 'Two Sum', status: 'ACCEPTED', language: 'java', score: 100, execTime: 42, testCasesPassed: 6, totalTestCases: 6, createdAt: new Date().toISOString() },
  { id: 2, problemId: 2, problemTitle: 'Reverse String', status: 'ACCEPTED', language: 'python', score: 100, execTime: 28, testCasesPassed: 5, totalTestCases: 5, createdAt: new Date().toISOString() },
  { id: 3, problemId: 5, problemTitle: 'Valid Parentheses', status: 'WRONG_ANSWER', language: 'java', score: 33, execTime: 35, testCasesPassed: 2, totalTestCases: 6, createdAt: new Date().toISOString() },
  { id: 4, problemId: 7, problemTitle: 'Longest Substring Without Repeating Characters', status: 'ACCEPTED', language: 'cpp', score: 100, execTime: 18, testCasesPassed: 6, totalTestCases: 6, createdAt: new Date().toISOString() },
  { id: 5, problemId: 9, problemTitle: 'Trapping Rain Water', status: 'TIME_LIMIT_EXCEEDED', language: 'python', score: 0, execTime: 2001, testCasesPassed: 0, totalTestCases: 5, createdAt: new Date().toISOString() },
  { id: 6, problemId: 3, problemTitle: 'FizzBuzz', status: 'ACCEPTED', language: 'javascript', score: 100, execTime: 15, testCasesPassed: 4, totalTestCases: 4, createdAt: new Date().toISOString() },
  { id: 7, problemId: 4, problemTitle: 'Palindrome Check', status: 'ACCEPTED', language: 'python', score: 100, execTime: 22, testCasesPassed: 5, totalTestCases: 5, createdAt: new Date().toISOString() },
  { id: 8, problemId: 10, problemTitle: 'N-Queens', status: 'COMPILATION_ERROR', language: 'cpp', score: 0, execTime: 0, testCasesPassed: 0, totalTestCases: 5, createdAt: new Date().toISOString() },
];
