import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { assessmentAPI } from '../api';
import { useAuth } from '../context/AuthContext';

const STORAGE_KEY = 'codesphere-assessment-progress';

function readProgress() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

function formatDate(value) {
  if (!value) return 'No deadline';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'No deadline';
  return date.toLocaleString([], { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

export default function AssessmentDashboard() {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [assessments, setAssessments] = useState([]);
  const [availableIds, setAvailableIds] = useState(new Set());
  const [releasedResults, setReleasedResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }

    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [assessmentRes, assignedRes, resultRes] = await Promise.allSettled([assessmentAPI.listAvailable(), assessmentAPI.listAssigned(), assessmentAPI.getResultHistory()]);
        if (assignedRes.status !== 'fulfilled') throw assignedRes.reason;
        if (active) {
          setAssessments(assignedRes.value.data || []);
          setAvailableIds(new Set(assessmentRes.status === 'fulfilled' ? (assessmentRes.value.data || []).map((assessment) => assessment.id) : []));
          setReleasedResults(resultRes.status === 'fulfilled' ? (resultRes.value.data ?? []) : []);
        }
      } catch (err) {
        if (active) {
          setError('The assessment catalog could not be loaded right now.');
          toast.error('Unable to load assessments.');
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    load();
    return () => { active = false; };
  }, [isLoggedIn, navigate]);

  const progress = useMemo(() => readProgress(), []);

  const cards = assessments.map((assessment) => {
    const key = String(assessment.id);
    const saved = progress[key];
    const now = new Date();
    const start = assessment.startTime ? new Date(assessment.startTime) : null;
    const end = assessment.endTime ? new Date(assessment.endTime) : null;
    const unavailableReason = start && start > now ? `Opens ${formatDate(assessment.startTime)}` : end && end < now ? 'The assessment window has closed' : 'This assessment is being prepared';
    const status = saved?.completed ? 'Completed' : !availableIds.has(assessment.id) ? unavailableReason : saved?.started ? 'Resume' : 'Not started';
    return { ...assessment, status, unavailable: !availableIds.has(assessment.id) };
  });

  return (
    <div className="container fade-in" style={{ paddingBottom: 60 }}>
      <div className="page-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><div><h1 className="page-title">Candidate assessments</h1>
        <p className="page-subtitle">Start or resume your live exam session from here.</p>
        </div><div style={{display:'flex',gap:8}}><button className="btn btn-secondary btn-sm" onClick={() => navigate('/notifications')}>Notifications</button><button className="btn btn-secondary btn-sm" onClick={() => navigate('/certifications')}>Certifications</button><button className="btn btn-secondary btn-sm" onClick={() => navigate('/assessments/history')}>View history</button></div></div>
      </div>

      {releasedResults.length > 0 && <section className="card" style={{ padding: 20, marginBottom: 24 }}><div className="card-header"><div><div className="card-title">Final results</div><div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Scores released by your administrator.</div></div><button className="btn btn-ghost btn-sm" onClick={() => navigate('/assessments/history')}>View all</button></div><div className="question-stack" style={{ marginTop: 12 }}>{releasedResults.slice(0, 3).map((result) => <div className="choice-option" key={result.assessmentId} style={{ justifyContent: 'space-between', gap: 14 }}><div><strong>{result.title}</strong><div className="text-secondary" style={{ marginTop: 4 }}>{result.status === 'PENDING_EVALUATION' ? 'Evaluation in progress' : `Final score: ${result.score} / ${result.totalScore}`}</div></div><button className="btn btn-primary btn-sm" onClick={() => navigate(`/assessments/${result.assessmentId}/result`)}>View result</button></div>)}</div></section>}

      {loading ? (
        <div className="card" style={{ padding: 24 }}>
          <div className="assessment-skeleton" />
          <div className="assessment-skeleton" />
          <div className="assessment-skeleton" />
        </div>
      ) : error ? (
        <div className="card empty-state">
          <div className="empty-icon">⚠️</div>
          <div className="empty-title">Assessments unavailable</div>
          <div className="empty-subtitle">{error}</div>
          <button className="btn btn-primary mt-4" onClick={() => window.location.reload()}>Retry</button>
        </div>
      ) : cards.length === 0 ? (
        <div className="card empty-state">
          <div className="empty-icon">🧪</div>
          <div className="empty-title">No assessments assigned yet</div>
          <div className="empty-subtitle">Once your exam is published and assigned, it will appear here.</div>
        </div>
      ) : (
        <div className="assessment-grid">
          {cards.map((assessment) => (
            <div key={assessment.id} className="card assessment-card">
              <div className="card-header" style={{ marginBottom: 12 }}>
                <div>
                  <div className="badge badge-tag" style={{ marginBottom: 8 }}>{assessment.assessmentType || 'MIXED'}</div>
                  <h2 style={{ fontSize: 20, fontWeight: 700 }}>{assessment.title}</h2>
                </div>
                <span className={`badge ${assessment.status === 'Resume' ? 'badge-pending' : assessment.status === 'Completed' ? 'badge-accepted' : 'badge-default'}`}>
                  {assessment.status}
                </span>
              </div>

              <p style={{ color: 'var(--text-secondary)', marginBottom: 16 }}>{assessment.description || 'Live assessment session'}</p>

              <div className="assessment-meta-grid">
                <div>
                  <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Duration</div>
                  <div style={{ fontWeight: 700 }}>{assessment.durationMinutes || 60} min</div>
                </div>
                <div>
                  <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Deadline</div>
                  <div style={{ fontWeight: 700 }}>{formatDate(assessment.endTime || assessment.deadline)}</div>
                </div>
              </div>

              <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end' }}>
                <button className="btn btn-primary" disabled={assessment.unavailable} onClick={() => navigate(`/assessments/${assessment.id}/session`)}>
                  {assessment.unavailable ? 'Not available' : assessment.status === 'Resume' ? 'Resume' : 'Start'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
