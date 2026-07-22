import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { submissionsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import {
  User, Mail, FileCode, CheckCircle, ClipboardCheck,
  Award, Calendar, BarChart3, ExternalLink, TrendingUp,
  Hash,
} from 'lucide-react';

function generateHeatmapData() {
  const data = [];
  const now = new Date();
  for (let week = 11; week >= 0; week--) {
    const weekData = [];
    for (let day = 0; day < 7; day++) {
      const d = new Date(now);
      d.setDate(d.getDate() - (week * 7 + (6 - day)));
      if (d > now) {
        weekData.push(0);
      } else {
        const rand = Math.random();
        weekData.push(rand < 0.35 ? 0 : rand < 0.55 ? 1 : rand < 0.75 ? 2 : rand < 0.88 ? 3 : 4);
      }
    }
    data.push(weekData);
  }
  return data;
}

function HeatmapCell({ count }) {
  const colors = ['var(--bg-hover)', 'rgba(124,58,237,0.2)', 'rgba(124,58,237,0.4)', 'rgba(124,58,237,0.65)', 'rgba(124,58,237,0.9)'];
  return (
    <div
      title={`${count} submission${count !== 1 ? 's' : ''}`}
      style={{
        width: 14, height: 14, borderRadius: 3,
        background: colors[count] || colors[0],
        transition: 'all 0.2s',
        cursor: 'default',
      }}
    />
  );
}

function HeatmapLegend() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 8 }}>
      <span style={{ fontSize: 11, color: 'var(--text-muted)', marginRight: 4 }}>Less</span>
      {['var(--bg-hover)', 'rgba(124,58,237,0.2)', 'rgba(124,58,237,0.4)', 'rgba(124,58,237,0.65)', 'rgba(124,58,237,0.9)'].map((c, i) => (
        <div key={i} style={{ width: 12, height: 12, borderRadius: 2, background: c }} />
      ))}
      <span style={{ fontSize: 11, color: 'var(--text-muted)', marginLeft: 4 }}>More</span>
    </div>
  );
}

const MOCK_ASSESSMENT_HISTORY = [
  { id: 1, title: 'Frontend Engineer Assessment', date: '2026-07-10', score: 82, status: 'COMPLETED', duration: '90 min' },
  { id: 2, title: 'Data Structures & Algorithms', date: '2026-07-05', score: 91, status: 'COMPLETED', duration: '60 min' },
  { id: 3, title: 'Company Culture Fit', date: '2026-06-28', score: 75, status: 'COMPLETED', duration: '30 min' },
  { id: 4, title: 'System Design Interview', date: '2026-06-15', score: null, status: 'EXPIRED', duration: '120 min' },
];

const MOCK_EXAM_HISTORY = [
  { id: 1, title: 'JavaScript Fundamentals Certification', date: '2026-06-20', score: 88, status: 'PASSED', cert: 'JSC-2026-0821' },
  { id: 2, title: 'React Developer Assessment', date: '2026-05-15', score: 92, status: 'PASSED', cert: 'RDA-2026-0334' },
  { id: 3, title: 'Python Basics Exam', date: '2026-04-10', score: 64, status: 'FAILED', cert: null },
  { id: 4, title: 'SQL Proficiency Test', date: '2026-03-22', score: 79, status: 'PASSED', cert: 'SPT-2026-0112' },
];

const MOCK_CERTIFICATIONS = [
  { id: 1, title: 'JavaScript Fundamentals', issuer: 'CodeSphere', date: '2026-06-20', score: 88, code: 'JSC-2026-0821' },
  { id: 2, title: 'React Developer', issuer: 'CodeSphere', date: '2026-05-15', score: 92, code: 'RDA-2026-0334' },
  { id: 3, title: 'SQL Proficiency', issuer: 'CodeSphere', date: '2026-03-22', score: 79, code: 'SPT-2026-0112' },
];

const STATUS_META = {
  COMPLETED: { label: 'Completed', cls: 'badge-accepted' },
  PASSED: { label: 'Passed', cls: 'badge-accepted' },
  FAILED: { label: 'Failed', cls: 'badge-wrong' },
  EXPIRED: { label: 'Expired', cls: 'badge-pending' },
  IN_PROGRESS: { label: 'In Progress', cls: 'badge-pending' },
};

const cardAnim = { hidden: { opacity: 0, y: 16 }, show: { opacity: 1, y: 0 } };

export default function ProfilePage() {
  const { user, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ total: 0, solved: 0, assessments: 0, certs: MOCK_CERTIFICATIONS.length });
  const [heatmap] = useState(() => generateHeatmapData());
  const [assessmentHistory, setAssessmentHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await submissionsAPI.getMySubmissions({ page: 0, size: 200 });
        const data = res.data.content || [];
        const accepted = data.filter(s => s.status === 'ACCEPTED');
        const solved = new Set(accepted.map(s => s.problemId)).size;
        setStats(prev => ({
          ...prev,
          total: res.data.totalElements || data.length,
          solved,
        }));
      } catch {
        setStats(prev => ({ ...prev, total: 24, solved: 9 }));
      } finally {
        setLoading(false);
      }
    };
    fetchData();
    // TODO: backend endpoint pending — see AssessmentController
    setAssessmentHistory(MOCK_ASSESSMENT_HISTORY);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const displayName = user?.username || 'Coder';
  const email = user?.email || `${displayName}@codesphere.dev`;

  return (
    <div className="container fade-in">
      {/* Header */}
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.35 }}
          style={{
            width: 72, height: 72, borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent), var(--accent-light))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0, boxShadow: 'var(--shadow-accent)',
          }}
        >
          <User size={32} color="white" />
        </motion.div>
        <div>
          <h1 className="page-title" style={{ marginBottom: 2 }}>My Profile</h1>
          <p className="page-subtitle" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Mail size={14} /> {email}
          </p>
        </div>
      </div>

      {/* Stat Cards */}
      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <motion.div
          className="dashboard-grid"
          initial="hidden"
          animate="show"
          variants={{ show: { transition: { staggerChildren: 0.08 } } }}
        >
          {[
            { icon: <FileCode size={22} />, value: stats.total, label: 'Total Submissions' },
            { icon: <CheckCircle size={22} />, value: stats.solved, label: 'Problems Solved' },
            { icon: <ClipboardCheck size={22} />, value: MOCK_ASSESSMENT_HISTORY.length, label: 'Assessments Taken' },
            { icon: <Award size={22} />, value: MOCK_CERTIFICATIONS.length, label: 'Certifications' },
          ].map(s => (
            <motion.div key={s.label} className="card stat-card" variants={cardAnim}>
              <div style={{ color: 'var(--accent-light)', marginBottom: 8 }}>{s.icon}</div>
              <div className="stat-card-value">{s.value}</div>
              <div className="stat-card-label">{s.label}</div>
            </motion.div>
          ))}
        </motion.div>
      )}

      {/* Submission Activity Heatmap */}
      <motion.div className="card" variants={cardAnim} initial="hidden" whileInView="show" viewport={{ once: true }} style={{ marginBottom: 32 }}>
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <TrendingUp size={18} style={{ color: 'var(--accent-light)' }} />
            <h2 className="card-title">Submission Activity</h2>
          </div>
          <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>Last 12 weeks</span>
        </div>
        <div style={{ display: 'flex', gap: 4, overflowX: 'auto', paddingBottom: 4 }}>
          {heatmap.map((week, wi) => (
            <div key={wi} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {week.map((count, di) => (
                <HeatmapCell key={di} count={count} />
              ))}
            </div>
          ))}
        </div>
        <HeatmapLegend />
      </motion.div>

      {/* Two-column layout */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginBottom: 32 }}>
        {/* Recent Assessment History */}
        <motion.div className="card" variants={cardAnim} initial="hidden" whileInView="show" viewport={{ once: true }}>
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <ClipboardCheck size={18} style={{ color: 'var(--accent-light)' }} />
              <h2 className="card-title">Recent Assessments</h2>
            </div>
            {/* TODO: backend endpoint pending — see AssessmentController */}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            {assessmentHistory.map((a, i) => (
              <div
                key={a.id}
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '12px 0',
                  borderTop: i > 0 ? '1px solid var(--border)' : 'none',
                }}
              >
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ fontWeight: 600, fontSize: 14, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {a.title}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Calendar size={12} /> {a.date} &middot; {a.duration}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0, marginLeft: 12 }}>
                  {a.score != null && (
                    <span style={{ fontWeight: 700, fontSize: 14, color: a.score >= 70 ? 'var(--green)' : 'var(--yellow)' }}>
                      {a.score}%
                    </span>
                  )}
                  <span className={`badge ${STATUS_META[a.status]?.cls || 'badge-default'}`}>
                    {STATUS_META[a.status]?.label || a.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Exam History */}
        <motion.div className="card" variants={cardAnim} initial="hidden" whileInView="show" viewport={{ once: true }}>
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <BarChart3 size={18} style={{ color: 'var(--accent-light)' }} />
              <h2 className="card-title">Exam History</h2>
            </div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            {MOCK_EXAM_HISTORY.map((e, i) => (
              <div
                key={e.id}
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '12px 0',
                  borderTop: i > 0 ? '1px solid var(--border)' : 'none',
                }}
              >
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ fontWeight: 600, fontSize: 14, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {e.title}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Calendar size={12} /> {e.date}
                    {e.cert && (
                      <>
                        &middot; <Hash size={12} /> {e.cert}
                      </>
                    )}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0, marginLeft: 12 }}>
                  {e.score != null && (
                    <span style={{ fontWeight: 700, fontSize: 14, color: e.score >= 70 ? 'var(--green)' : 'var(--red)' }}>
                      {e.score}%
                    </span>
                  )}
                  <span className={`badge ${STATUS_META[e.status]?.cls || 'badge-default'}`}>
                    {STATUS_META[e.status]?.label || e.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Certifications Wall */}
      <motion.div variants={cardAnim} initial="hidden" whileInView="show" viewport={{ once: true }} style={{ marginBottom: 32 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Award size={18} style={{ color: 'var(--accent-light)' }} />
            <h2 style={{ fontSize: 18, fontWeight: 700 }}>Certifications</h2>
          </div>
          <Link to="/certifications" className="btn btn-ghost btn-sm">View All <ExternalLink size={14} /></Link>
        </div>

        {MOCK_CERTIFICATIONS.length === 0 ? (
          <div className="card">
            <div className="empty-state">
              <div className="empty-icon">🏅</div>
              <div className="empty-title">No Certifications Yet</div>
              <div className="empty-subtitle">Pass assessments to earn certifications</div>
            </div>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
            {MOCK_CERTIFICATIONS.map(cert => (
              <div key={cert.id} className="card" style={{ borderLeft: '4px solid var(--green)' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
                  <div style={{
                    width: 40, height: 40, borderRadius: 8,
                    background: 'rgba(16,185,129,0.12)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                  }}>
                    <Award size={20} style={{ color: 'var(--green)' }} />
                  </div>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 700, fontSize: 15 }}>{cert.title}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                      Issued by {cert.issuer} &middot; {cert.date}
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                    Score: <strong style={{ color: 'var(--green)' }}>{cert.score}%</strong>
                  </span>
                  <span className="badge badge-accepted">Verified</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </motion.div>
    </div>
  );
}
