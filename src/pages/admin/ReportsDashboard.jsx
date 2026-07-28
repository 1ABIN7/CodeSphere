import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  TrendingUp,
  Users,
  CheckCircle2,
  ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { adminAPI } from '../../api';

// TODO: backend endpoint pending — see ReportController
const EMPTY_REPORTS = {
  summary: {
    averageScore: 0,
    completionRate: 0,
    passRate: 0,
  },
  assessments: [],
};

export default function ReportsDashboard() {
  const [selectedId, setSelectedId] = useState(null);
  const [reports, setReports] = useState(EMPTY_REPORTS);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminAPI.getAssessmentReports()
      .then((response) => setReports(response.data))
      .catch(() => toast.error('Unable to load assessment reports.'))
      .finally(() => setLoading(false));
  }, []);

  const { summary, assessments } = reports;

  const selected = assessments.find((a) => a.id === selectedId);

  const summaryCards = [
    { label: 'Average Score', value: `${summary.averageScore}%`, icon: <TrendingUp size={24} />, color: 'var(--accent-light)' },
    { label: 'Completion Rate', value: `${summary.completionRate}%`, icon: <CheckCircle2 size={24} />, color: 'var(--green)' },
    { label: 'Pass Rate', value: `${summary.passRate}%`, icon: <Users size={24} />, color: 'var(--yellow)' },
  ];

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title" style={{ fontSize: 24 }}>Reports</h1>
        <p className="page-subtitle">Assessment performance analytics</p>
      </div>

      <div className="dashboard-grid" style={{ marginBottom: 32 }}>
        {summaryCards.map((c, i) => (
          <motion.div
            key={c.label}
            className="card stat-card"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
          >
            <div style={{ color: c.color, marginBottom: 8 }}>{c.icon}</div>
            <div className="stat-card-value">{c.value}</div>
            <div className="stat-card-label">{c.label}</div>
          </motion.div>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Assessment Results</span>
        </div>

        {loading ? <div style={{ padding: 24, color: 'var(--text-secondary)' }}>Loading reports…</div> : selected ? (
          <div>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => setSelectedId(null)}
              style={{ marginBottom: 16 }}
            >
              ← Back to list
            </button>
            <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16 }}>{selected.title}</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, marginBottom: 24 }}>
              <div
                style={{
                  padding: 16,
                  background: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-sm)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 28, fontWeight: 800, color: 'var(--accent-light)' }}>
                  {selected.averageScore}%
                </div>
                <div className="label" style={{ marginTop: 4 }}>Average Score</div>
              </div>
              <div
                style={{
                  padding: 16,
                  background: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-sm)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 28, fontWeight: 800, color: 'var(--green)' }}>
                  {selected.passRate}%
                </div>
                <div className="label" style={{ marginTop: 4 }}>Pass Rate</div>
              </div>
              <div
                style={{
                  padding: 16,
                  background: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-sm)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 28, fontWeight: 800, color: 'var(--yellow)' }}>
                  {selected.completionRate}%
                </div>
                <div className="label" style={{ marginTop: 4 }}>Completion Rate</div>
              </div>
            </div>

            <div>
              <div className="label" style={{ marginBottom: 8 }}>Score Distribution</div>
              <div style={{ display: 'flex', gap: 4, alignItems: 'flex-end', height: 120 }}>
                {[30, 50, 80, 120, 95, 60, 35].map((h, i) => (
                  <div
                    key={i}
                    style={{
                      flex: 1,
                      height: `${(h / 120) * 100}%`,
                      background: 'var(--accent)',
                      borderRadius: '4px 4px 0 0',
                      opacity: 0.7,
                    }}
                  />
                ))}
              </div>
              <div style={{ display: 'flex', gap: 4, marginTop: 4 }}>
                {['0-20', '20-40', '40-60', '60-80', '80-90', '90-95', '95-100'].map((l) => (
                  <div key={l} style={{ flex: 1, textAlign: 'center', fontSize: 10, color: 'var(--text-muted)' }}>
                    {l}
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Assessment</th>
                  <th>Candidates</th>
                  <th>Avg Score</th>
                  <th>Pass Rate</th>
                  <th>Completion</th>
                  <th style={{ textAlign: 'right' }}></th>
                </tr>
              </thead>
              <tbody>
                {assessments.map((a) => (
                  <tr key={a.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedId(a.id)}>
                    <td style={{ fontWeight: 600 }}>{a.title}</td>
                    <td>{a.candidates}</td>
                    <td>
                      <span style={{ color: 'var(--accent-light)', fontWeight: 600 }}>{a.averageScore}%</span>
                    </td>
                    <td>
                      <span style={{ color: a.passRate >= 60 ? 'var(--green)' : 'var(--red)', fontWeight: 600 }}>
                        {a.passRate}%
                      </span>
                    </td>
                    <td>{a.completionRate}%</td>
                    <td style={{ textAlign: 'right' }}>
                      <ChevronRight size={16} style={{ color: 'var(--text-muted)' }} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
