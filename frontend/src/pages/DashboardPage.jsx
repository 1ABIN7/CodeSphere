import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { submissionsAPI, assessmentAPI } from '../api';
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
  const [finalResults, setFinalResults] = useState([]);
  const [availableAssessments, setAvailableAssessments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, accepted: 0, problems: 0, assignments: 0 });

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    const fetchData = async () => {
      setLoading(true);
      try {
        const [submissionResult, assessmentResult, resultHistory] = await Promise.allSettled([
          submissionsAPI.getMySubmissions({ page: 0, size: 20 }),
          assessmentAPI.listAvailable(),
          assessmentAPI.getResultHistory(),
        ]);
        const data = submissionResult.status === 'fulfilled' ? (submissionResult.value.data.content || []) : [];
        const available = assessmentResult.status === 'fulfilled'
          ? (assessmentResult.value.data?.content || assessmentResult.value.data || []) : [];
        setSubmissions(data);
        setFinalResults(resultHistory.status === 'fulfilled' ? (resultHistory.value.data ?? []) : []);
        setAvailableAssessments(Array.isArray(available) ? available : []);
        const accepted = data.filter(s => s.status === 'ACCEPTED').length;
        const problems = new Set(data.filter(s => s.status === 'ACCEPTED').map(s => s.problemId)).size;
        setStats({ total: submissionResult.status === 'fulfilled' ? (submissionResult.value.data.totalElements || data.length) : 0, accepted, problems, assignments: Array.isArray(available) ? available.length : 0 });
      } catch {
        setSubmissions(MOCK_SUBMISSIONS);
        setStats({ total: 24, accepted: 9, problems: 7, assignments: 0 });
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isLoggedIn]);

  return (
    <div className="container fade-in candidate-dashboard">
      <section className="candidate-hero">
        <div>
          <div className="candidate-eyebrow">Your learning space</div>
          <h1>Welcome back{user?.fullName ? `, ${user.fullName.split(' ')[0]}` : user?.username ? `, ${user.username}` : ''}.</h1>
          <p>Continue an assessment or spend a few focused minutes practicing.</p>
        </div>
        <div className="candidate-hero-actions">
          <Link className="btn btn-secondary" to="/problems">Browse problems</Link>
          <Link className="btn btn-primary" to="/assessments">My assessments</Link>
        </div>
      </section>

      <section className="candidate-overview-section">
        <div className="candidate-section-heading">
          <div><h2>Your progress</h2><p>A snapshot of your coding activity.</p></div>
          {loading && <span className="candidate-loading-label"><span className="spinner" /> Updating</span>}
        </div>
        <div className="candidate-stat-grid">
        {[
          { icon: '🧪', value: stats.assignments, label: 'Available assessments', description: 'Ready when you are' },
          { icon: '🧩', value: stats.problems, label: 'Problems solved', description: 'Unique accepted problems' },
          { icon: '✅', value: stats.accepted, label: 'Accepted submissions', description: 'Solutions that passed' },
        ].map(s => (
          <div key={s.label} className="candidate-stat-card">
            <div className="candidate-stat-icon">{s.icon}</div>
            <div><div className="candidate-stat-value">{s.value}</div><div className="candidate-stat-label">{s.label}</div><div className="candidate-stat-description">{s.description}</div></div>
          </div>
        ))}
        </div>
      </section>

      {availableAssessments.length > 0 && <section className="candidate-overview-section"><div className="candidate-section-heading"><div><h2>Assigned assessments</h2><p>Tests that are ready for you to start or resume.</p></div><Link to="/assessments" className="btn btn-ghost btn-sm">View all</Link></div><div className="question-stack">{availableAssessments.slice(0, 3).map((assessment) => <div className="card" key={assessment.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 14, padding: 16 }}><div><strong>{assessment.title}</strong><div className="text-secondary" style={{ marginTop: 4 }}>{assessment.assessmentType || 'Assessment'} · {assessment.durationMinutes || 60} minutes</div></div><Link className="btn btn-primary btn-sm" to={`/assessments/${assessment.id}/session`}>Start assessment</Link></div>)}</div></section>}

      {finalResults.length > 0 && <section className="candidate-overview-section"><div className="candidate-section-heading"><div><h2>Final results</h2><p>Scores your administrator has released.</p></div><Link to="/assessments/history" className="btn btn-ghost btn-sm">View all</Link></div><div className="question-stack">{finalResults.slice(0, 3).map((result) => <div className="card" key={result.assessmentId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 14, padding: 16 }}><div><strong>{result.title}</strong><div className="text-secondary" style={{ marginTop: 4 }}>{result.status === 'PENDING_EVALUATION' ? 'Evaluation in progress' : `Final score: ${result.score} / ${result.totalScore}`}</div></div><Link className="btn btn-primary btn-sm" to={`/assessments/${result.assessmentId}/result`}>View result</Link></div>)}</div></section>}

      <section className="candidate-overview-section"><div className="candidate-section-heading"><div><h2>Recent activity</h2><p>Your latest coding submissions and progress.</p></div></div></section>
      <div className="candidate-workspace-grid">
        {/* Recent Submissions */}
        <div>
          <div className="candidate-panel-heading">
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
          <div className="candidate-panel-heading"><h2>Skills</h2><Link to="/assessments/history" className="btn btn-ghost btn-sm">Assessment history</Link></div>
          <div className="card candidate-skill-card">
            <SkillBreakdown submissions={submissions} />
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

      {entries.length === 0 && (
        <div className="text-secondary" style={{ fontSize: 13, lineHeight: 1.6 }}>
          Your accepted submissions will appear here by programming language.
        </div>
      )}
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
