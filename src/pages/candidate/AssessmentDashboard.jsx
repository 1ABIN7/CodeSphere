import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { assessmentsAPI } from '../../api';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import {
  Clock, PlayCircle, BarChart3, Calendar, AlertTriangle,
  CheckCircle, Timer, FileText
} from 'lucide-react';

const STATUS_CONFIG = {
  UPCOMING: { label: 'Upcoming', color: 'var(--blue)', bg: 'rgba(59,130,246,0.12)', icon: <Calendar size={14} /> },
  IN_PROGRESS: { label: 'In Progress', color: 'var(--accent-light)', bg: 'rgba(124,58,237,0.12)', icon: <Timer size={14} /> },
  COMPLETED: { label: 'Completed', color: 'var(--green)', bg: 'rgba(16,185,129,0.12)', icon: <CheckCircle size={14} /> },
  EXPIRED: { label: 'Expired', color: 'var(--red)', bg: 'rgba(239,68,68,0.12)', icon: <AlertTriangle size={14} /> },
};

const TYPE_LABELS = {
  CODING: 'Coding',
  MCQ_SINGLE: 'MCQ (Single)',
  MCQ_MULTI: 'MCQ (Multi)',
  SUBJECTIVE: 'Written',
  READING_COMPREHENSION: 'Reading Comp.',
  FILE_UPLOAD: 'File Upload',
  MIXED: 'Mixed',
};

function formatDeadline(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  const now = new Date();
  const diff = d - now;
  if (diff <= 0) return 'Expired';
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  if (days > 0) return `${days}d ${hours}h remaining`;
  const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  if (hours > 0) return `${hours}h ${mins}m remaining`;
  return `${mins}m remaining`;
}

function AssessmentCard({ assessment, onStart }) {
  const status = STATUS_CONFIG[assessment.status] || STATUS_CONFIG.UPCOMING;
  const deadline = formatDeadline(assessment.deadline);
  const isCompleted = assessment.status === 'COMPLETED' || assessment.status === 'EXPIRED';

  return (
    <div className="assessment-card fade-in">
      <div className="assessment-card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1, minWidth: 0 }}>
          <FileText size={20} style={{ color: 'var(--accent-light)', flexShrink: 0 }} />
          <div style={{ minWidth: 0 }}>
            <h3 style={{ fontSize: '16px', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {assessment.title}
            </h3>
            {assessment.description && (
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '4px 0 0', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {assessment.description}
              </p>
            )}
          </div>
        </div>
        <span className="assessment-status-badge" style={{ background: status.bg, color: status.color, flexShrink: 0 }}>
          {status.icon} {status.label}
        </span>
      </div>

      <div className="assessment-card-meta">
        <div className="assessment-meta-item">
          <Clock size={14} />
          <span>{assessment.durationMinutes || 60} min</span>
        </div>
        <div className="assessment-meta-item">
          <BarChart3 size={14} />
          <span>{TYPE_LABELS[assessment.type] || assessment.type || 'Mixed'}</span>
        </div>
        {assessment.totalQuestions && (
          <div className="assessment-meta-item">
            <FileText size={14} />
            <span>{assessment.totalQuestions} questions</span>
          </div>
        )}
        {deadline && (
          <div className="assessment-meta-item" style={{ color: deadline.includes('Expired') ? 'var(--red)' : 'var(--text-secondary)' }}>
            <Calendar size={14} />
            <span>{deadline}</span>
          </div>
        )}
        {assessment.score != null && (
          <div className="assessment-meta-item" style={{ color: 'var(--green)' }}>
            <BarChart3 size={14} />
            <span>Score: {assessment.score}%</span>
          </div>
        )}
      </div>

      <div className="assessment-card-footer">
        {isCompleted ? (
          <button className="btn btn-secondary btn-sm" onClick={() => onStart(assessment, 'result')}>
            <BarChart3 size={14} /> View Results
          </button>
        ) : (
          <button className="btn btn-primary btn-sm" onClick={() => onStart(assessment, 'session')}>
            <PlayCircle size={14} /> {assessment.status === 'IN_PROGRESS' ? 'Resume' : 'Start'}
          </button>
        )}
      </div>
    </div>
  );
}

export default function AssessmentDashboard() {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [assessments, setAssessments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    fetchAssessments();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchAssessments = async () => {
    setLoading(true);
    try {
      const res = await assessmentsAPI.getMyAssessments();
      setAssessments(res.data?.content || res.data || []);
    } catch {
      toast.error('Could not load assessments. Showing demo data.');
      setAssessments(MOCK_ASSESSMENTS);
    } finally {
      setLoading(false);
    }
  };

  const handleNavigate = (assessment, mode) => {
    if (mode === 'result') {
      navigate(`/assessments/${assessment.id}/result`);
    } else {
      navigate(`/assessments/${assessment.id}/session`);
    }
  };

  const filtered = filter === 'ALL'
    ? assessments
    : assessments.filter(a => a.status === filter);

  const counts = {
    ALL: assessments.length,
    UPCOMING: assessments.filter(a => a.status === 'UPCOMING').length,
    IN_PROGRESS: assessments.filter(a => a.status === 'IN_PROGRESS').length,
    COMPLETED: assessments.filter(a => a.status === 'COMPLETED').length,
  };

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: '60vh' }}>
        <div className="spinner" />
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">My Assessments</h1>
        <p className="page-subtitle">View and take assessments assigned to you</p>
      </div>

      <div className="assessment-filters">
        {['ALL', 'IN_PROGRESS', 'UPCOMING', 'COMPLETED'].map(f => (
          <button
            key={f}
            className={`filter-tab ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f === 'ALL' ? 'All' : STATUS_CONFIG[f]?.label || f}
            <span className="filter-count">{counts[f]}</span>
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📋</div>
          <div className="empty-title">No Assessments Found</div>
          <div className="empty-subtitle">
            {filter === 'ALL' ? 'You have no assessments assigned yet.' : `No ${filter.toLowerCase()} assessments.`}
          </div>
        </div>
      ) : (
        <div className="assessment-list">
          {filtered.map(a => (
            <AssessmentCard key={a.id} assessment={a} onStart={handleNavigate} />
          ))}
        </div>
      )}
    </div>
  );
}

const MOCK_ASSESSMENTS = [
  {
    id: 1, title: 'Frontend Engineer Assessment', type: 'MIXED',
    description: 'Full-stack assessment covering coding, MCQ, and system design.',
    durationMinutes: 120, status: 'IN_PROGRESS',
    deadline: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    totalQuestions: 15,
  },
  {
    id: 2, title: 'Data Structures & Algorithms', type: 'CODING',
    description: 'Timed coding assessment with 5 algorithmic problems.',
    durationMinutes: 90, status: 'UPCOMING',
    deadline: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
    totalQuestions: 5,
  },
  {
    id: 3, title: 'Company Culture Fit', type: 'MCQ_SINGLE',
    description: 'Multiple choice questions about company values and work culture.',
    durationMinutes: 30, status: 'COMPLETED',
    deadline: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
    totalQuestions: 20, score: 85,
  },
];
