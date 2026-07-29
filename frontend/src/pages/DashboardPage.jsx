import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { submissionsAPI, problemsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const SKILL_COLORS = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

const STATUS_META = {
  ACCEPTED: { label: 'Accepted', cls: 'badge-accepted' },
  WRONG_ANSWER: { label: 'Wrong Answer', cls: 'badge-wrong' },
  TIME_LIMIT_EXCEEDED: { label: 'TLE', cls: 'badge-pending' },
  RUNTIME_ERROR: { label: 'Runtime Error', cls: 'badge-wrong' },
  COMPILATION_ERROR: { label: 'Compile Error', cls: 'badge-wrong' },
  PARTIALLY_ACCEPTED: { label: 'Partial', cls: 'badge-pending' },
};

export default function DashboardPage() {
  const { user, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, accepted: 0, problems: 0 });

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await submissionsAPI.getMySubmissions({ page: 0, size: 20 });
        const data = res.data.content || [];
        setSubmissions(data);
        const accepted = data.filter(s => s.status === 'ACCEPTED').length;
        const problems = new Set(data.filter(s => s.status === 'ACCEPTED').map(s => s.problemId)).size;
        setStats({ total: res.data.totalElements || data.length, accepted, problems });
      } catch {
        setSubmissions(MOCK_SUBMISSIONS);
        setStats({ total: 24, accepted: 9, problems: 7 });
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isLoggedIn]);

  const acceptRate = stats.total > 0 ? ((stats.accepted / stats.total) * 100).toFixed(1) : '0.0';

  return (
    <div className="container fade-in">
      <div className="page-header">
        <h1 className="page-title">Welcome back, {user?.username || 'Coder'} 👋</h1>
        <p className="page-subtitle">Track your progress and coding skills</p>
      </div>

      {/* Stat Cards */}
      <div className="dashboard-grid">
        {[
          { icon: '📬', value: stats.total, label: 'Total Submissions' },
          { icon: '✅', value: stats.accepted, label: 'Accepted' },
          { icon: '🧩', value: stats.problems, label: 'Problems Solved' },
          { icon: '📈', value: `${acceptRate}%`, label: 'Acceptance Rate' },
        ].map(s => (
          <div key={s.label} className="card stat-card">
            <div className="stat-card-icon">{s.icon}</div>
            <div className="stat-card-value">{s.value}</div>
            <div className="stat-card-label">{s.label}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 24 }}>
        {/* Recent Submissions */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <h2 style={{ fontSize: 18, fontWeight: 700 }}>Recent Submissions</h2>
            <Link to="/submissions" className="btn btn-ghost btn-sm">View All →</Link>
          </div>

          {loading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Problem</th>
                    <th>Status</th>
                    <th>Language</th>
                    <th>Time</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.slice(0, 10).map(s => (
                    <tr key={s.id} onClick={() => navigate(`/problems/${s.problemId}`)} style={{ cursor: 'pointer' }}>
                      <td style={{ fontWeight: 600, maxWidth: 200 }} className="truncate">
                        {s.problemTitle || `Problem #${s.problemId}`}
                      </td>
                      <td>
                        <span className={`badge ${STATUS_META[s.status]?.cls || 'badge-default'}`}>
                          {STATUS_META[s.status]?.label || s.status}
                        </span>
                      </td>
                      <td style={{ fontSize: 13, color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                        {s.language}
                      </td>
                      <td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                        {s.execTime ? `${s.execTime}ms` : '—'}
                      </td>
                      <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                        {s.createdAt ? new Date(s.createdAt).toLocaleDateString() : 'Today'}
                      </td>
                    </tr>
                  ))}
                  {submissions.length === 0 && (
                    <tr>
                      <td colSpan={5}>
                        <div className="empty-state" style={{ padding: '40px' }}>
                          <div className="empty-icon">📬</div>
                          <div className="empty-title">No submissions yet</div>
                          <div className="empty-subtitle">
                            <Link to="/problems" className="auth-link">Start solving problems →</Link>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Skill Breakdown */}
        <div>
          <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 16 }}>Skill Breakdown</h2>
          <div className="card">
            <SkillBreakdown submissions={submissions} />
          </div>

          {/* Quick Actions */}
          <div style={{ marginTop: 20 }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 16 }}>Quick Actions</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <Link to="/problems?difficulty=EASY" className="btn btn-secondary" style={{ justifyContent: 'flex-start' }}>
                🟢 Practice Easy Problems
              </Link>
              <Link to="/assessments" className="btn btn-primary" style={{ justifyContent: 'flex-start' }}>
                🧪 My Assessments
              </Link>
              <Link to="/problems?difficulty=MEDIUM" className="btn btn-secondary" style={{ justifyContent: 'flex-start' }}>
                🟡 Tackle Medium Problems
              </Link>
              <Link to="/problems?difficulty=HARD" className="btn btn-secondary" style={{ justifyContent: 'flex-start' }}>
                🔴 Challenge Hard Problems
              </Link>
              <Link to="/problems" className="btn btn-primary" style={{ justifyContent: 'flex-start' }}>
                🚀 Browse All Problems
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function SkillBreakdown({ submissions }) {
  const skills = {};
  (submissions || []).forEach(s => {
    if (s.status !== 'ACCEPTED') return;
    const lang = s.language || 'unknown';
    skills[lang] = (skills[lang] || 0) + 1;
  });

  const entries = Object.entries(skills).sort((a, b) => b[1] - a[1]);
  const max = entries[0]?.[1] || 1;

  // Difficulty breakdown from mock data
  const diffBreakdown = [
    { label: 'Easy', count: 4, color: '#10b981' },
    { label: 'Medium', count: 3, color: '#f59e0b' },
    { label: 'Hard', count: 0, color: '#ef4444' },
  ];

  return (
    <div>
      {entries.length > 0 && (
        <>
          <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 12 }}>
            By Language
          </div>
          <div className="skill-bar-container" style={{ marginBottom: 20 }}>
            {entries.map(([lang, count], i) => (
              <div key={lang} className="skill-bar-item">
                <div className="skill-bar-header">
                  <span style={{ fontWeight: 500, fontSize: 13 }}>{lang}</span>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{count} solved</span>
                </div>
                <div className="skill-bar-track">
                  <div
                    className="skill-bar-fill"
                    style={{
                      width: `${(count / max) * 100}%`,
                      background: `linear-gradient(90deg, ${SKILL_COLORS[i % SKILL_COLORS.length]}, ${SKILL_COLORS[(i + 1) % SKILL_COLORS.length]})`
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 12 }}>
        By Difficulty
      </div>
      <div style={{ display: 'flex', gap: 12 }}>
        {diffBreakdown.map(d => (
          <div key={d.label} style={{
            flex: 1, textAlign: 'center', padding: '12px 8px',
            background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)',
            border: `1px solid ${d.color}33`
          }}>
            <div style={{ fontSize: 22, fontWeight: 800, color: d.color }}>{d.count}</div>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>{d.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

const MOCK_SUBMISSIONS = [
  { id: 1, problemId: 1, problemTitle: 'Two Sum', status: 'ACCEPTED', language: 'java', execTime: 42, createdAt: new Date().toISOString() },
  { id: 2, problemId: 2, problemTitle: 'Reverse String', status: 'ACCEPTED', language: 'python', execTime: 28, createdAt: new Date().toISOString() },
  { id: 3, problemId: 5, problemTitle: 'Valid Parentheses', status: 'WRONG_ANSWER', language: 'java', execTime: 35, createdAt: new Date().toISOString() },
  { id: 4, problemId: 7, problemTitle: 'Longest Substring Without Repeating Characters', status: 'ACCEPTED', language: 'cpp', execTime: 18, createdAt: new Date().toISOString() },
  { id: 5, problemId: 9, problemTitle: 'Trapping Rain Water', status: 'TIME_LIMIT_EXCEEDED', language: 'python', execTime: 2000, createdAt: new Date().toISOString() },
  { id: 6, problemId: 3, problemTitle: 'FizzBuzz', status: 'ACCEPTED', language: 'javascript', execTime: 15, createdAt: new Date().toISOString() },
];
