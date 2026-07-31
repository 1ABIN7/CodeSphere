import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminAPI, usersAPI } from '../../api';

export default function AdminAiInsightsPage() {
  const [candidates, setCandidates] = useState([]);
  const [candidateId, setCandidateId] = useState('');
  const [insight, setInsight] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);

  useEffect(() => { usersAPI.listCandidates().then(({ data }) => setCandidates(data ?? [])).catch(() => toast.error('Unable to load candidates.')).finally(() => setLoading(false)); }, []);

  const selectCandidate = async (value) => {
    setCandidateId(value); setInsight(null); setMetrics(null);
    if (!value) return;
    try { const { data } = await adminAPI.getCandidateSkillMetrics(value); setMetrics(data); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to load candidate chart data.'); }
  };

  const analyze = async () => {
    if (!candidateId) return toast.error('Choose a candidate first.');
    setAnalyzing(true);
    try { const { data } = await adminAPI.getCandidateAiInsight(candidateId); setInsight(data); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to generate AI insight.'); }
    finally { setAnalyzing(false); }
  };

  return <div className="container fade-in admin-ai-insights">
    <section className="admin-ai-hero"><div className="admin-ai-icon">✦</div><div><div className="admin-ai-eyebrow">Gemini-powered review</div><h1>Candidate AI Insights</h1><p>Understand coding strengths, growth areas, and style patterns from completed submissions.</p></div></section>
    <section className="card admin-ai-controls"><div><label className="label">Candidate</label><select className="select" value={candidateId} onChange={(event) => selectCandidate(event.target.value)} disabled={loading}><option value="">Select a candidate…</option>{candidates.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.fullName || candidate.username} {candidate.email ? `· ${candidate.email}` : ''}</option>)}</select></div><button className="btn btn-primary" onClick={analyze} disabled={analyzing || loading || !candidateId}>{analyzing ? 'Analyzing…' : 'Generate insight'} <span>✦</span></button></section>
    <section className="admin-ai-disclaimer"><span>ⓘ</span><p>AI feedback is advisory only. It analyzes the candidate’s latest completed coding submissions and does not change assessment grades or make hiring decisions.</p></section>
    {analyzing && <section className="card admin-ai-loading"><div className="spinner" /><div><strong>Reviewing completed submissions</strong><p>Gemini is identifying patterns in the candidate’s code and judge results.</p></div></section>}
    {metrics && <SkillCharts metrics={metrics} />}
    {!analyzing && !insight && !metrics && <section className="card empty-state admin-ai-empty"><div className="empty-icon">✦</div><div className="empty-title">Select a candidate to begin</div><div className="empty-subtitle">Insights are based on up to the candidate’s 12 most recent coding submissions.</div></section>}
    {insight && <section className="card admin-ai-result"><header><div className="admin-ai-result-avatar">{insight.candidateName?.slice(0, 1).toUpperCase()}</div><div><h2>{insight.candidateName}</h2><p>Based on {insight.analyzedSubmissions} recent coding submission{insight.analyzedSubmissions === 1 ? '' : 's'}</p></div></header><div className="admin-ai-result-body">{insight.insight}</div></section>}
  </div>;
}

function SkillCharts({ metrics }) {
  const COLORS = ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ec4899'];
  const acceptanceRate = metrics.analyzedSubmissions ? Math.round(metrics.acceptedSubmissions / metrics.analyzedSubmissions * 100) : 0;
  return <section className="admin-ai-charts"><div className="admin-ai-metric-row"><Metric label="Recent submissions" value={metrics.analyzedSubmissions} /><Metric label="Accepted" value={metrics.acceptedSubmissions} /><Metric label="Acceptance rate" value={`${acceptanceRate}%`} /></div><div className="admin-ai-chart-grid"><ChartCard title="Submission performance" subtitle="Test-case score across recent submissions"><ResponsiveContainer width="100%" height={220}><LineChart data={metrics.scoreTrend}><CartesianGrid strokeDasharray="3 3" opacity={.15} /><XAxis dataKey="label" tick={{ fontSize: 11 }} /><YAxis domain={[0, 100]} tickFormatter={(value) => `${value}%`} /><Tooltip formatter={(value) => `${value}%`} /><Line type="monotone" dataKey="score" stroke="#8b5cf6" strokeWidth={3} dot={{ r: 3 }} /></LineChart></ResponsiveContainer></ChartCard><ChartCard title="Language activity" subtitle="Submissions and accepted solutions"><ResponsiveContainer width="100%" height={220}><BarChart data={metrics.languages}><CartesianGrid strokeDasharray="3 3" opacity={.15} /><XAxis dataKey="language" tick={{ fontSize: 11 }} /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="submitted" name="Submitted" fill="#3b82f6" radius={[4, 4, 0, 0]} /><Bar dataKey="accepted" name="Accepted" fill="#10b981" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></ChartCard><ChartCard title="Judge verdicts" subtitle="Outcome distribution"><ResponsiveContainer width="100%" height={220}><BarChart data={metrics.verdicts} layout="vertical"><CartesianGrid strokeDasharray="3 3" opacity={.15} /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="status" width={118} tick={{ fontSize: 10 }} /><Tooltip /><Bar dataKey="count" name="Submissions" radius={[0, 4, 4, 0]}>{metrics.verdicts.map((entry, index) => <Cell key={entry.status} fill={COLORS[index % COLORS.length]} />)}</Bar></BarChart></ResponsiveContainer></ChartCard></div></section>;
}

function Metric({ label, value }) { return <div className="admin-ai-metric"><strong>{value}</strong><span>{label}</span></div>; }
function ChartCard({ title, subtitle, children }) { return <article className="card admin-ai-chart"><header><strong>{title}</strong><small>{subtitle}</small></header>{children}</article>; }
