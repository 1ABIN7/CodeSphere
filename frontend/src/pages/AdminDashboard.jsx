import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { adminAPI, assessmentAPI, questionBankAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const EMPTY_STATE = {
  totalAssessments: 'Unavailable',
  pendingReviews: 'Unavailable',
  activeSessions: 'Unavailable',
  publishedQuestions: 'Unavailable',
};

const INITIAL_QUESTION = {
  title: '',
  content: '',
  category: '',
  questionType: 'MCQ_SINGLE',
  difficulty: 'MEDIUM',
  tags: '',
  correctAnswers: '',
  points: 1,
  negativeScore: 0,
};

export default function AdminDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(EMPTY_STATE);
  const [assessments, setAssessments] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [activity, setActivity] = useState([]);
  const [isQuestionModalOpen, setIsQuestionModalOpen] = useState(false);
  const [questionForm, setQuestionForm] = useState(INITIAL_QUESTION);
  const [isSavingQuestion, setIsSavingQuestion] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      setLoading(true);
      try {
        const [dashboardRes, assessmentRes, questionRes] = await Promise.allSettled([
          adminAPI.getDashboard({ activityLimit: 6 }),
          assessmentAPI.list(),
          questionBankAPI.list(),
        ]);

        if (!isMounted) return;

        const assessmentItems = assessmentRes.status === 'fulfilled' ? (assessmentRes.value?.data?.content || assessmentRes.value?.data || []) : [];
        const questionItems = questionRes.status === 'fulfilled' ? (questionRes.value?.data?.content || questionRes.value?.data || []) : [];
        const dashboard = dashboardRes.status === 'fulfilled' ? dashboardRes.value?.data : null;

        const nextStats = {
          totalAssessments: dashboard?.totalAssessments ?? 'Unavailable',
          pendingReviews: dashboard?.pendingReviews ?? 'Unavailable',
          activeSessions: dashboard?.activeSessions ?? 'Unavailable',
          publishedQuestions: dashboard?.publishedQuestions ?? 'Unavailable',
        };

        setStats(nextStats);
        setAssessments(Array.isArray(assessmentItems) ? assessmentItems.slice(0, 5) : []);
        setQuestions(Array.isArray(questionItems) ? questionItems.slice(0, 10) : []);
        setActivity(Array.isArray(dashboard?.recentActivity) ? dashboard.recentActivity : []);
      } catch {
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

  const handleQuestionChange = (event) => {
    const { name, value } = event.target;
    setQuestionForm((current) => ({ ...current, [name]: value }));
  };

  const handleCreateQuestion = async (event) => {
    event.preventDefault();
    setIsSavingQuestion(true);
    try {
      const payload = {
        ...questionForm,
        type: questionForm.questionType,
        tags: questionForm.tags.split(',').map((tag) => tag.trim()).filter(Boolean),
        points: Number(questionForm.points),
        negativeScore: Number(questionForm.negativeScore),
      };
      const { data: createdQuestion } = await questionBankAPI.create(payload);
      setQuestions((current) => [createdQuestion, ...current].slice(0, 5));
      setQuestionForm(INITIAL_QUESTION);
      setIsQuestionModalOpen(false);
      toast.success('Question added to the bank.');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Unable to add the question.');
    } finally {
      setIsSavingQuestion(false);
    }
  };

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

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 24 }}>
        <Link className="btn btn-secondary btn-sm" to="/problems">View problems</Link>
        <Link className="btn btn-primary btn-sm" to="/admin/assessments">Manage assessments</Link>
        <Link className="btn btn-primary btn-sm" to="/admin/questions">Manage question bank</Link>
        <Link className="btn btn-secondary btn-sm" to="/admin/categories">Manage categories</Link>
        <Link className="btn btn-secondary btn-sm" to="/admin/evaluations">Evaluate submissions</Link>
        <Link className="btn btn-secondary btn-sm" to="/admin/reports">View reports</Link>
        <Link className="btn btn-secondary btn-sm" to="/admin/users">Manage users</Link>
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
              <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Latest submissions across all candidates.</div>
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
                    <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>
                      {entry.candidateName || 'Unknown candidate'} · {entry.status || 'Pending'}
                    </div>
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
            <div className="card-title">Question bank</div>
            <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Your 10 most recently created questions.</div>
          </div>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setIsQuestionModalOpen(true)}>+ Add question</button>
        </div>

        {loading ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : questions.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <div className="empty-icon">🗂️</div>
            <div className="empty-title">No questions yet</div>
            <div className="empty-subtitle">Create the first question for your assessment bank.</div>
          </div>
        ) : (
          <div className="question-stack">
            {questions.map((question) => (
              <div key={question.id} className="choice-option" style={{ justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontWeight: 700 }}>{question.title || `Question #${question.id}`}</div>
                  <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>{question.content || 'Question available in the bank.'}</div>
                </div>
                <span className="badge badge-tag">{question.questionType || question.type || question.difficulty || 'Question'}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {isQuestionModalOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="create-question-title">
          <form className="card" onSubmit={handleCreateQuestion} style={{ width: 'min(720px, 100%)', maxHeight: '90vh', overflowY: 'auto', padding: 24 }}>
            <div className="card-header">
              <div>
                <div id="create-question-title" className="card-title">Add a question</div>
                <div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Add it to the bank for use in an assessment.</div>
              </div>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setIsQuestionModalOpen(false)}>Close</button>
            </div>

            <div className="form-group" style={{ marginTop: 16 }}>
              <label className="label" htmlFor="question-title">Title</label>
              <input id="question-title" className="input" name="title" value={questionForm.title} onChange={handleQuestionChange} required placeholder="e.g. Arrays: two sum" />
            </div>
            <div className="form-group" style={{ marginTop: 16 }}>
              <label className="label" htmlFor="question-content">Question content</label>
              <textarea id="question-content" className="textarea" name="content" value={questionForm.content} onChange={handleQuestionChange} required placeholder="Write the prompt, answer choices, or instructions." />
            </div>

            <div style={{ display: 'grid', gap: 12, gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', marginTop: 16 }}>
              <div className="form-group">
                <label className="label" htmlFor="question-type">Type</label>
                <select id="question-type" className="select" name="questionType" value={questionForm.questionType} onChange={handleQuestionChange}>
                  <option value="MCQ_SINGLE">Multiple choice</option>
                  <option value="MCQ_MULTI">Multiple select</option>
                  <option value="WRITTEN">Written response</option>
                  <option value="CODING">Coding</option>
                  <option value="READING_COMPREHENSION">Reading comprehension</option>
                </select>
              </div>
              <div className="form-group">
                <label className="label" htmlFor="question-difficulty">Difficulty</label>
                <select id="question-difficulty" className="select" name="difficulty" value={questionForm.difficulty} onChange={handleQuestionChange}>
                  <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
                </select>
              </div>
              <div className="form-group">
                <label className="label" htmlFor="question-category">Category</label>
                <input id="question-category" className="input" name="category" value={questionForm.category} onChange={handleQuestionChange} placeholder="Algorithms" />
              </div>
            </div>

            <div style={{ display: 'grid', gap: 12, gridTemplateColumns: '1fr 120px 120px', marginTop: 16 }}>
              <div className="form-group">
                <label className="label" htmlFor="question-answer">Correct answer(s)</label>
                <input id="question-answer" className="input" name="correctAnswers" value={questionForm.correctAnswers} onChange={handleQuestionChange} placeholder="e.g. B or A,C" />
              </div>
              <div className="form-group">
                <label className="label" htmlFor="question-points">Points</label>
                <input id="question-points" className="input" name="points" type="number" min="0" value={questionForm.points} onChange={handleQuestionChange} required />
              </div>
              <div className="form-group">
                <label className="label" htmlFor="question-negative-score">Negative</label>
                <input id="question-negative-score" className="input" name="negativeScore" type="number" min="0" value={questionForm.negativeScore} onChange={handleQuestionChange} required />
              </div>
            </div>
            <div className="form-group" style={{ marginTop: 16 }}>
              <label className="label" htmlFor="question-tags">Tags</label>
              <input id="question-tags" className="input" name="tags" value={questionForm.tags} onChange={handleQuestionChange} placeholder="arrays, hash maps, beginner" />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 24 }}>
              <button type="button" className="btn btn-ghost" onClick={() => setIsQuestionModalOpen(false)} disabled={isSavingQuestion}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={isSavingQuestion}>{isSavingQuestion ? 'Adding…' : 'Add question'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
