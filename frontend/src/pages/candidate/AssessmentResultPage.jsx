import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { assessmentsAPI } from '../../api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import {
  BarChart3, CheckCircle, XCircle, Clock, ArrowLeft,
  ChevronDown, ChevronUp, FileText
} from 'lucide-react';

function ScoreRing({ score, maxScore, size = 120 }) {
  const pct = maxScore ? (score / maxScore) * 100 : 0;
  const r = (size / 2) - 8;
  const circ = 2 * Math.PI * r;
  const offset = circ - (pct / 100) * circ;
  const color = pct >= 70 ? '#10b981' : pct >= 40 ? '#f59e0b' : '#ef4444';

  return (
    <div style={{ width: size, height: size, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size/2} cy={size/2} r={r} stroke="var(--bg-hover)" strokeWidth="6" fill="none" />
        <circle cx={size/2} cy={size/2} r={r} stroke={color} strokeWidth="6" fill="none"
          strokeDasharray={circ} strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 1s ease', transform: 'rotate(-90deg)', transformOrigin: 'center' }}
          strokeLinecap="round" />
      </svg>
      <div style={{ position: 'absolute', textAlign: 'center' }}>
        <div style={{ fontSize: 28, fontWeight: 800, color }}>{Math.round(pct)}%</div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{score}/{maxScore}</div>
      </div>
    </div>
  );
}

function SectionResult({ section, index }) {
  const [expanded, setExpanded] = useState(false);
  const maxPoints = (section.questions || []).reduce((s, q) => s + (q.maxPoints || q.points || 0), 0);
  const earned = (section.questions || []).reduce((s, q) => s + (q.score || q.earnedPoints || 0), 0);
  const pct = maxPoints ? Math.round((earned / maxPoints) * 100) : 0;

  return (
    <div className="section-result-card">
      <div
        className="section-result-header"
        onClick={() => setExpanded(!expanded)}
        style={{ cursor: 'pointer' }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
          <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--accent-light)', minWidth: '24px' }}>
            {index + 1}
          </span>
          <div>
            <div style={{ fontWeight: 600, fontSize: '15px' }}>{section.title || `Section ${index + 1}`}</div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
              {section.questions?.length || 0} questions · {earned}/{maxPoints} points
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div className={`badge ${pct >= 70 ? 'badge-accepted' : pct >= 40 ? 'badge-medium' : 'badge-hard'}`}>
            {pct}%
          </div>
          {expanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
        </div>
      </div>

      {expanded && (
        <div className="section-result-body fade-in">
          {(section.questions || []).map((q, qi) => {
            const qMax = q.maxPoints || q.points || 0;
            const qEarned = q.score || q.earnedPoints || 0;
            const isCorrect = qEarned === qMax;
            return (
              <div key={q.id || qi} className="question-result-row">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1 }}>
                  {isCorrect ? (
                    <CheckCircle size={16} style={{ color: 'var(--green)', flexShrink: 0 }} />
                  ) : (
                    <XCircle size={16} style={{ color: 'var(--red)', flexShrink: 0 }} />
                  )}
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: '14px', fontWeight: 500 }}>
                      {q.title || `Question ${qi + 1}`}
                    </div>
                    {q.feedback && (
                      <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                        {q.feedback}
                      </div>
                    )}
                  </div>
                </div>
                <span style={{ fontSize: '13px', fontWeight: 600, color: isCorrect ? 'var(--green)' : 'var(--red)', flexShrink: 0 }}>
                  {qEarned}/{qMax}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function AssessmentResultPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    fetchResult();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchResult = async () => {
    setLoading(true);
    try {
      const res = await assessmentsAPI.getResult(id);
      setResult(res.data);
    } catch {
      toast.error('Could not load results. Showing demo data.');
      setResult(MOCK_RESULT);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: '60vh' }}>
        <div className="spinner" />
      </div>
    );
  }

  if (!result) return null;

  const totalMax = result.totalMaxScore || result.maxScore || 100;
  const totalEarned = result.totalScore || result.score || 0;

  return (
    <div className="fade-in" style={{ maxWidth: 900, margin: '0 auto' }}>
      <button
        className="btn btn-ghost btn-sm"
        onClick={() => navigate('/assessments')}
        style={{ marginBottom: '20px' }}
      >
        <ArrowLeft size={14} /> Back to Assessments
      </button>

      {/* Overall Result Card */}
      <div className="result-overview-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
          <ScoreRing score={totalEarned} maxScore={totalMax} />
          <div style={{ flex: 1, minWidth: 200 }}>
            <h1 style={{ fontSize: '24px', fontWeight: 800, marginBottom: '6px' }}>
              {result.assessmentTitle || `Assessment #${id}`}
            </h1>
            <div style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '12px' }}>
              {result.completedAt && (
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Clock size={14} /> Completed {new Date(result.completedAt).toLocaleString()}
                </span>
              )}
            </div>
            <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--green)' }}>
                  {result.correctCount || 0}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Correct</div>
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--red)' }}>
                  {result.incorrectCount || 0}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Incorrect</div>
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-secondary)' }}>
                  {result.unansweredCount || 0}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Unanswered</div>
              </div>
              {result.timeTakenMinutes && (
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--accent-light)' }}>
                    {result.timeTakenMinutes}m
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Time Taken</div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Section-wise Results */}
      <div style={{ marginTop: '32px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChart3 size={20} /> Section Breakdown
        </h2>
        {(result.sections || []).length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {result.sections.map((section, i) => (
              <SectionResult key={section.id || i} section={section} index={i} />
            ))}
          </div>
        ) : (
          <div className="card" style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            <FileText size={32} style={{ opacity: 0.4, marginBottom: '8px' }} />
            <div>No detailed section breakdown available.</div>
          </div>
        )}
      </div>
    </div>
  );
}

const MOCK_RESULT = {
  assessmentTitle: 'Frontend Engineer Assessment',
  totalScore: 72,
  totalMaxScore: 100,
  completedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  correctCount: 8,
  incorrectCount: 3,
  unansweredCount: 2,
  timeTakenMinutes: 95,
  sections: [
    {
      id: 1, title: 'Data Structures', questions: [
        { id: 1, title: 'Binary Tree Traversal', maxPoints: 10, score: 10, feedback: 'Correct implementation of in-order traversal.' },
        { id: 2, title: 'Hash Map Implementation', maxPoints: 10, score: 5, feedback: 'Partial credit: handled collisions but missed resize logic.' },
      ]
    },
    {
      id: 2, title: 'Algorithms', questions: [
        { id: 3, title: 'Dynamic Programming', maxPoints: 15, score: 12, feedback: 'Good approach, minor optimization missed.' },
        { id: 4, title: 'Graph Traversal', maxPoints: 15, score: 0, feedback: 'Incorrect handling of directed graphs.' },
      ]
    },
    {
      id: 3, title: 'System Design', questions: [
        { id: 5, title: 'URL Shortener', maxPoints: 20, score: 15, feedback: 'Solid design, could improve scalability discussion.' },
      ]
    }
  ],
};
