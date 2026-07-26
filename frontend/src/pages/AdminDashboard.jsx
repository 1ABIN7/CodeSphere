import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { assessmentAPI, problemsAPI, submissionsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const EMPTY_STATE = {
  totalAssessments: 'Unavailable',
  pendingReviews: 'Unavailable',
  activeSessions: 'Unavailable',
  publishedQuestions: 'Unavailable',
};

export default function AdminDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(EMPTY_STATE);
  const [assessments, setAssessments] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [activity, setActivity] = useState([]);

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      setLoading(true);
      try {
        const [assessmentRes, problemsRes, submissionsRes] = await Promise.allSettled([
          assessmentAPI.list(),
          problemsAPI.list({ page: 0, size: 8 }),
          submissionsAPI.getMySubmissions({ page: 0, size: 8 }),
        ]);

        if (!isMounted) return;

        const assessmentItems = assessmentRes.status === 'fulfilled' ? (assessmentRes.value?.data?.content || assessmentRes.value?.data || []) : [];
        const problemItems = problemsRes.status === 'fulfilled' ? (problemsRes.value?.data?.content || problemsRes.value?.data || []) : [];
        const submissionItems = submissionsRes.status === 'fulfilled' ? (submissionsRes.value?.data?.content || submissionsRes.value?.data || []) : [];

        const nextStats = {
          totalAssessments: Array.isArray(assessmentItems) ? assessmentItems.length : 'Unavailable',
          pendingReviews: 'Pending review endpoint not exposed yet',
          activeSessions: 'Session metrics are not exposed yet',
          publishedQuestions: Array.isArray(problemItems) ? problemItems.length : 'Unavailable',
        };

        setStats(nextStats);
        setAssessments(Array.isArray(assessmentItems) ? assessmentItems.slice(0, 5) : []);
        setQuestions(Array.isArray(problemItems) ? problemItems.slice(0, 5) : []);
        setActivity(Array.isArray(submissionItems) ? submissionItems.slice(0, 6) : []);
      } catch (err) {
        if (!isMounted) return;
        toast.error('Unable to load admin overview right now.');
        setStats(EMPTY_STATE);
        setAssessments([]);
        setQuestions([]);
        setActivity([]);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, []);

  const roleLabel = useMemo(() => {
    const role = user?.role || '';
    if (role.includes('SUPER')) return 'Super Admin';
    if (role.includes('ORG')) return 'Org Admin';
    if (role.includes('EXAMINER')) return 'Examiner';
    return 'Admin';
  }, [user]);

  return (
    <div className="container fade-in">
      <div className="page-header">
        <h1 className="page-title">Admin dashboard</h1>
        <p className="page-subtitle">{roleLabel} overview for assessment and question management.</p>
      </div>

      <div className="dashboard-grid">
        {[
          { label: 'Total assessments', value: stats.totalAssessments, icon: '🧪' },
          { label: 'Pending review', value: stats.pendingReviews, icon: '⏳' },
          { label: 'Active sessions', value: stats.activeSessions, icon: '⚡' },
          { label: 'Published questions', value: stats.publishedQuestions, icon: '🗂️' },
        ].map((item) => (
          <div key={item.label} className="card stat-card">
            <div className="stat-card-icon">{item.icon}</div>
            <div className="stat-card-value" style={{ fontSize: 24 }}>{item.value}</div>
            <div className="stat-card-label">{item.label}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gap: 24, gridTemplateColumns: '1.1fr 0.9fr' }}>
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Recent assessments</div>
              <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>From the current assessment catalog.</div>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/assessments')}>Open catalog</button>
          </div>

          {loading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : assessments.length === 0 ? (
            <div className="empty-state" style={{ padding: 24 }}>
              <div className="empty-icon">🧪</div>
              <div className="empty-title">No assessments surfaced</div>
              <div className="empty-subtitle">The backend did not return any assessment items for this role yet.</div>
            </div>
          ) : (
            <div className="question-stack">
              {assessments.map((assessment) => (
                <div key={assessment.id || assessment.assessmentId} className="choice-option" style={{ justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ fontWeight: 700 }}>{assessment.title || assessment.name || `Assessment #${assessment.id || assessment.assessmentId}`}</div>
                    <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>
                      {assessment.description || 'Assessment entry available in the catalog.'}
                    </div>
                  </div>
                  <Link to={`/assessments/${assessment.id || assessment.assessmentId}`} className="btn btn-secondary btn-sm">Inspect</Link>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Recent activity</div>
              <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Latest submissions visible to the signed-in user.</div>
            </div>
          </div>

          {loading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : activity.length === 0 ? (
            <div className="empty-state" style={{ padding: 24 }}>
              <div className="empty-icon">📬</div>
              <div className="empty-title">No recent activity</div>
              <div className="empty-subtitle">Submissions will appear here once the backend exposes them.</div>
            </div>
          ) : (
            <div className="question-stack">
              {activity.map((entry) => (
                <div key={entry.id} className="choice-option" style={{ justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ fontWeight: 700 }}>{entry.problemTitle || `Submission #${entry.id}`}</div>
                    <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>{entry.status || 'Pending'}</div>
                  </div>
                  <span className="badge badge-default">{entry.language || 'n/a'}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-header">
          <div>
            <div className="card-title">Question inventory</div>
            <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Problem bank entries currently returned by the API.</div>
          </div>
          <Link to="/problems" className="btn btn-secondary btn-sm">Browse bank</Link>
        </div>

        {loading ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : questions.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-icon">🗂️</div>
            <div className="empty-title">No questions available</div>
            <div className="empty-subtitle">The problem bank is empty or inaccessible from the current role.</div>
          </div>
        ) : (
          <div className="question-stack">
            {questions.map((question) => (
              <div key={question.id} className="choice-option" style={{ justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontWeight: 700 }}>{question.title || `Problem #${question.id}`}</div>
                  <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>{question.description || 'Problem available in the bank.'}</div>
                </div>
                <span className="badge badge-tag">{question.difficulty || 'Unknown'}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
