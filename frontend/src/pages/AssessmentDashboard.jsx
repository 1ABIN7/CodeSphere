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
        const res = await assessmentAPI.listAvailable();
        if (active) setAssessments(res.data || []);
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
    const status = saved?.completed ? 'Completed' : saved?.started ? 'Resume' : 'Not started';
    return { ...assessment, status };
  });

  return (
    <div className="container fade-in" style={{ paddingBottom: 60 }}>
      <div className="page-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><div><h1 className="page-title">Candidate assessments</h1>
        <p className="page-subtitle">Start or resume your live exam session from here.</p>
        </div><button className="btn btn-secondary btn-sm" onClick={() => navigate('/assessments/history')}>View history</button></div>
      </div>

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
                <button className="btn btn-primary" onClick={() => navigate(`/assessments/${assessment.id}/session`)}>
                  {assessment.status === 'Resume' ? 'Resume' : 'Start'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
