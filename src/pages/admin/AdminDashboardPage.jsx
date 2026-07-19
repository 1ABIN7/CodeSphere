import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ClipboardList,
  FileQuestion,
  Clock,
  Plus,
  BookOpen,
  BarChart3,
} from 'lucide-react';
import { assessmentsAPI } from '../../api';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState({
    totalAssessments: 0,
    questionsInBank: 0,
    pendingEvaluations: 0,
  });
  const [recentAssessments, setRecentAssessments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  async function loadDashboard() {
    setLoading(true);
    try {
      const res = await assessmentsAPI.getAllAssessments();
      const list = res.data?.content ?? res.data ?? [];
      setRecentAssessments(list.slice(0, 5));
      setStats((s) => ({ ...s, totalAssessments: list.length }));
    } catch {
      // endpoints may not exist yet
    } finally {
      // TODO: backend endpoint pending — fetch questions count and pending evaluations
      setLoading(false);
    }
  }

  const statCards = [
    {
      label: 'Total Assessments',
      value: stats.totalAssessments,
      icon: <ClipboardList size={24} />,
      color: 'var(--accent-light)',
    },
    {
      label: 'Questions in Bank',
      value: stats.questionsInBank,
      icon: <FileQuestion size={24} />,
      color: 'var(--green)',
    },
    {
      label: 'Pending Evaluations',
      value: stats.pendingEvaluations,
      icon: <Clock size={24} />,
      color: 'var(--yellow)',
    },
  ];

  const quickActions = [
    { label: 'Create Assessment', to: '/admin/assessments/new', icon: <Plus size={16} /> },
    { label: 'Browse Questions', to: '/admin/questions', icon: <BookOpen size={16} /> },
    { label: 'View Reports', to: '/admin/reports', icon: <BarChart3 size={16} /> },
  ];

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Admin Dashboard</h1>
        <p className="page-subtitle">Overview of your assessment platform</p>
      </div>

      <div className="dashboard-grid" style={{ marginBottom: 32 }}>
        {statCards.map((s, i) => (
          <motion.div
            key={s.label}
            className="card stat-card"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
          >
            <div style={{ color: s.color, marginBottom: 8 }}>{s.icon}</div>
            <div className="stat-card-value">{loading ? '—' : s.value}</div>
            <div className="stat-card-label">{s.label}</div>
          </motion.div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 32 }}>
        {quickActions.map((a) => (
          <Link key={a.label} to={a.to} className="btn btn-secondary btn-sm">
            {a.icon} {a.label}
          </Link>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Recent Assessments</span>
        </div>
        {recentAssessments.length === 0 ? (
          <div className="empty-state" style={{ padding: 32 }}>
            <div className="empty-title">No assessments yet</div>
            <div className="empty-subtitle">Create your first assessment to get started.</div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {recentAssessments.map((a) => (
              <div
                key={a.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-sm)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{a.title}</div>
                  <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                    {a.type ?? 'Assessment'}
                  </div>
                </div>
                <span
                  className={`badge ${
                    a.published ? 'badge-easy' : 'badge-default'
                  }`}
                >
                  {a.published ? 'Published' : 'Draft'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
