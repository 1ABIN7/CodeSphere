import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { assessmentAPI } from '../api';

export default function AssessmentHistoryPage() {
  const [items, setItems] = useState([]); const [loading, setLoading] = useState(true);
  useEffect(() => { assessmentAPI.getResultHistory().then((res) => setItems(res.data ?? [])).finally(() => setLoading(false)); }, []);
  return <div className="container fade-in"><div className="page-header"><h1 className="page-title">Assessment History</h1><p className="page-subtitle">Review completed assessments and evaluation status.</p></div><div className="card">{loading ? <div className="loading-center"><div className="spinner" /></div> : items.length === 0 ? <div className="empty-state"><div className="empty-title">No completed assessments yet</div><div className="empty-subtitle">Completed results will appear here.</div></div> : <div className="table-container"><table><thead><tr><th>Assessment</th><th>Score</th><th>Status</th><th>Submitted</th><th /></tr></thead><tbody>{items.map((item) => <tr key={item.assessmentId}><td>{item.title}</td><td>{item.score} / {item.totalScore}</td><td><span className="badge badge-default">{item.status === 'PENDING_EVALUATION' ? 'Pending evaluation' : 'Completed'}</span></td><td>{item.submittedAt ? new Date(item.submittedAt).toLocaleString() : '—'}</td><td><Link className="btn btn-secondary btn-sm" to={`/assessments/${item.assessmentId}/result`}>View result</Link></td></tr>)}</tbody></table></div>}</div></div>;
}
