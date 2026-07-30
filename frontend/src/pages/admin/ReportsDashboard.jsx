import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminAPI } from '../../api';

const EMPTY = { summary: { averageScore: 0, passRate: 0 }, assessments: [] };
const percent = (value) => `${Number(value ?? 0).toFixed(1)}%`;
const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ec4899', '#06b6d4'];

export default function ReportsDashboard() {
  const [report, setReport] = useState(EMPTY);
  const [questionMetrics, setQuestionMetrics] = useState([]);
  const [candidateMetrics, setCandidateMetrics] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([adminAPI.getAssessmentReports(), adminAPI.getQuestionAnalytics(), adminAPI.getCandidateAnalytics()])
      .then(([assessmentRes, questionRes, candidateRes]) => {
        if (assessmentRes.status === 'fulfilled') setReport(assessmentRes.value.data ?? EMPTY); else toast.error('Unable to load assessment reports.');
        if (questionRes.status === 'fulfilled') setQuestionMetrics(questionRes.value.data?.questions ?? []);
        if (candidateRes.status === 'fulfilled') setCandidateMetrics(candidateRes.value.data?.candidates ?? []);
      }).finally(() => setLoading(false));
  }, []);

  const summary = report.summary ?? EMPTY.summary;
  const topPerformer = candidateMetrics[0];
  const difficultyData = useMemo(() => ['EASY', 'MEDIUM', 'HARD'].map((difficulty) => {
    const items = questionMetrics.filter((item) => item.difficulty === difficulty);
    return { difficulty: difficulty[0] + difficulty.slice(1).toLowerCase(), averageScore: items.length ? items.reduce((sum, item) => sum + item.averageScore, 0) / items.length : 0, questions: items.length };
  }), [questionMetrics]);

  return <div className="container fade-in">
    <div className="page-header"><h1 className="page-title">Assessment Reports</h1><p className="page-subtitle">Performance trends, top performers, and question effectiveness.</p></div>
    <div className="dashboard-grid">{[{ label: 'Average score', value: percent(summary.averageScore) }, { label: 'Pass rate', value: percent(summary.passRate) }, { label: 'Top performer', value: topPerformer ? topPerformer.name : '—', small: true }].map((metric) => <div className="card stat-card" key={metric.label}><div className="stat-card-value" style={metric.small ? { fontSize: 19 } : {}}>{metric.value}</div><div className="stat-card-label">{metric.label}</div></div>)}</div>
    {!loading && <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginTop: 24 }}>
      <ChartCard title="Assessment performance" subtitle="Average score by assessment"><ResponsiveContainer width="100%" height={260}><BarChart data={(report.assessments ?? []).slice(0, 8)}><CartesianGrid strokeDasharray="3 3" opacity={0.2} /><XAxis dataKey="title" tick={{ fontSize: 11 }} interval={0} angle={-18} textAnchor="end" height={60} /><YAxis domain={[0, 100]} /><Tooltip formatter={(value) => percent(value)} /><Bar dataKey="averageScore" radius={[5, 5, 0, 0]} fill="#6366f1" /></BarChart></ResponsiveContainer></ChartCard>
      <ChartCard title="Top candidates" subtitle="Ranked by average completed-assessment score"><ResponsiveContainer width="100%" height={260}><BarChart data={candidateMetrics.slice(0, 8)}><CartesianGrid strokeDasharray="3 3" opacity={0.2} /><XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-18} textAnchor="end" height={60} /><YAxis domain={[0, 100]} /><Tooltip formatter={(value) => percent(value)} /><Bar dataKey="averageScore" radius={[5, 5, 0, 0]}>{candidateMetrics.slice(0, 8).map((item, index) => <Cell key={item.userId} fill={COLORS[index % COLORS.length]} />)}</Bar></BarChart></ResponsiveContainer></ChartCard>
      <ChartCard title="Difficulty analysis" subtitle="Average score for attempted questions"><ResponsiveContainer width="100%" height={260}><BarChart data={difficultyData}><CartesianGrid strokeDasharray="3 3" opacity={0.2} /><XAxis dataKey="difficulty" /><YAxis domain={[0, 100]} /><Tooltip formatter={(value) => percent(value)} /><Bar dataKey="averageScore" radius={[5, 5, 0, 0]} fill="#22c55e" /></BarChart></ResponsiveContainer></ChartCard>
      <ChartCard title="Question effectiveness" subtitle="Most-attempted questions by average score"><ResponsiveContainer width="100%" height={260}><BarChart data={[...questionMetrics].sort((a, b) => b.attempts - a.attempts).slice(0, 8)}><CartesianGrid strokeDasharray="3 3" opacity={0.2} /><XAxis dataKey="title" tick={{ fontSize: 11 }} interval={0} angle={-18} textAnchor="end" height={60} /><YAxis domain={[0, 100]} /><Tooltip formatter={(value) => percent(value)} /><Bar dataKey="averageScore" radius={[5, 5, 0, 0]} fill="#f59e0b" /></BarChart></ResponsiveContainer></ChartCard>
    </div>}
    <ReportTable title="Assessment details" headers={['Assessment', 'Candidates', 'Average score', 'Pass rate']} empty="No assessment results are available yet.">{(report.assessments ?? []).map((item) => <tr key={item.id}><td>{item.title}</td><td>{item.candidates ?? 0}</td><td>{percent(item.averageScore)}</td><td>{percent(item.passRate)}</td></tr>)}</ReportTable>
    <ReportTable title="Candidate performance" headers={['Rank', 'Candidate', 'Completed', 'Average', 'Recent trend']} empty="Candidate performance appears after completed assessments.">{candidateMetrics.map((item) => <tr key={item.userId}><td>#{item.rank}</td><td><strong>{item.name}</strong></td><td>{item.completedAssessments}</td><td>{percent(item.averageScore)}</td><td>{item.trend.slice(-3).map((point) => `${point.assessmentTitle}: ${percent(point.score)}`).join(' · ')}</td></tr>)}</ReportTable>
    <ReportTable title="Question effectiveness" headers={['Question', 'Attempts', 'Avg. score', 'Avg. time']} empty="Question metrics appear after candidates submit answers.">{questionMetrics.map((item) => <tr key={item.id}><td><strong>{item.title}</strong><div className="text-secondary" style={{ fontSize: 12 }}>{item.questionType} · {item.difficulty || 'Unclassified'}</div></td><td>{item.attempts}</td><td>{percent(item.averageScore)}</td><td>{item.averageSecondsSpent}s</td></tr>)}</ReportTable>
  </div>;
}

function ChartCard({ title, subtitle, children }) { return <div className="card" style={{ padding: 20 }}><div className="card-title">{title}</div><div className="text-secondary" style={{ fontSize: 13, margin: '4px 0 12px' }}>{subtitle}</div>{children}</div>; }
function ReportTable({ title, headers, empty, children }) { const rows = Array.isArray(children) ? children : [children]; return <div className="card" style={{ marginTop: 24 }}><div className="card-title" style={{ marginBottom: 14 }}>{title}</div><div className="table-container"><table><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.length ? rows : <tr><td colSpan={headers.length} style={{ textAlign: 'center', padding: 28 }}>{empty}</td></tr>}</tbody></table></div></div>; }
