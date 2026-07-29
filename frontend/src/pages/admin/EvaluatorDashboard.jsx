import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { evaluationAPI } from '../../api';

export default function EvaluatorDashboard() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [drafts, setDrafts] = useState({});
  useEffect(() => { evaluationAPI.getPending().then((res) => setItems(res.data ?? [])).catch(() => toast.error('Unable to load pending evaluations.')).finally(() => setLoading(false)); }, []);
  const submit = async (item) => {
    const draft = drafts[item.answerId] ?? {};
    const rubricScore = (item.rubricCriteria ?? []).reduce((total, criterion, index) => total + Number(draft.rubricScores?.[index] ?? 0), 0);
    const score = item.rubricCriteria?.length ? rubricScore : draft.score;
    if (score === undefined || score === '') return toast.error('Enter a score first.');
    if (Number(score) > item.maxScore) return toast.error(`Score cannot exceed ${item.maxScore}.`);
    const rubricScores = Object.fromEntries((item.rubricCriteria ?? []).map((criterion, index) => [criterion.name, Number(draft.rubricScores?.[index] ?? 0)]));
    try { await evaluationAPI.submit(item.answerId, { score: Number(score), feedback: draft.feedback ?? '', rubricScores }); setItems((all) => all.filter((entry) => entry.answerId !== item.answerId)); toast.success(item.requiredReviewCount > 1 ? 'Review submitted for consensus.' : 'Evaluation submitted.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to submit evaluation.'); }
  };
  return <div className="container fade-in"><div className="page-header"><h1 className="page-title">Evaluator Queue</h1><p className="page-subtitle">Two independent reviews are averaged into the candidate’s consensus score.</p></div><div className="card">{loading ? <div className="loading-center"><div className="spinner" /></div> : items.length === 0 ? <div className="empty-state"><div className="empty-title">All caught up</div><div className="empty-subtitle">There are no pending evaluations.</div></div> : <div className="question-stack">{items.map((item) => { const draft = drafts[item.answerId] ?? {}; const rubricScore = (item.rubricCriteria ?? []).reduce((total, criterion, index) => total + Number(draft.rubricScores?.[index] ?? 0), 0); return <div key={item.answerId} className="choice-option" style={{ display: 'block' }}><strong>{item.candidateName}</strong><div className="text-secondary" style={{ marginTop: 4 }}>{item.questionTitle} · Max score: {item.maxScore} · Reviews: {item.reviewCount}/{item.requiredReviewCount}</div><pre style={{ whiteSpace: 'pre-wrap', margin: '14px 0', font: 'inherit', color: 'var(--text-primary)' }}>{item.answerText || item.fileUrl || 'No answer content available.'}</pre>{item.rubricCriteria?.length > 0 && <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: 8, marginBottom: 12 }}>{item.rubricCriteria.map((criterion, index) => <><div key={`${criterion.name}-label`} className="text-secondary">{criterion.name} (max {criterion.maxPoints})</div><input key={`${criterion.name}-score`} className="input" type="number" min="0" max={criterion.maxPoints} value={draft.rubricScores?.[index] ?? ''} onChange={(event) => { const rubricScores = [...(draft.rubricScores ?? [])]; rubricScores[index] = event.target.value; setDrafts({ ...drafts, [item.answerId]: { ...draft, rubricScores } }); }} /></>)}</div>}<div style={{ display: 'grid', gridTemplateColumns: '120px 1fr auto', gap: 10 }}><input className="input" type="number" min="0" max={item.maxScore} placeholder="Score" readOnly={item.rubricCriteria?.length > 0} value={item.rubricCriteria?.length ? rubricScore : (draft.score ?? '')} onChange={(e) => setDrafts({ ...drafts, [item.answerId]: { ...draft, score: e.target.value } })} /><input className="input" placeholder="Feedback for candidate" value={draft.feedback ?? ''} onChange={(e) => setDrafts({ ...drafts, [item.answerId]: { ...draft, feedback: e.target.value } })} /><button className="btn btn-primary btn-sm" onClick={() => submit(item)}>Submit review</button></div></div>; })}</div>}</div></div>;
}
