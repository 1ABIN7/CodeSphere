import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { evaluationAPI } from '../../api';
import { useAuth } from '../../context/AuthContext';

export default function EvaluatorDashboard() {
  const { user } = useAuth();
  const canManage = ['ROLE_SUPER_ADMIN', 'ROLE_ORG_ADMIN'].includes(user?.role);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [drafts, setDrafts] = useState({});
  const [management, setManagement] = useState([]);
  const [evaluators, setEvaluators] = useState([]);
  const [assignments, setAssignments] = useState({});
  const [resolutions, setResolutions] = useState({});
  const [preview, setPreview] = useState(null);

  useEffect(() => {
    evaluationAPI.getPending().then((res) => setItems(res.data ?? []))
      .catch(() => toast.error('Unable to load pending evaluations.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!canManage) return;
    Promise.all([evaluationAPI.getManagement(), evaluationAPI.getEvaluators()])
      .then(([queue, people]) => { setManagement(queue.data ?? []); setEvaluators(people.data ?? []); })
      .catch(() => toast.error('Unable to load evaluator assignments.'));
  }, [canManage]);

  const submit = async (item) => {
    const draft = drafts[item.answerId] ?? {};
    const rubricScores = Object.fromEntries((item.rubricCriteria ?? []).map((criterion, index) => [criterion.name, Number(draft.rubricScores?.[index] ?? 0)]));
    const score = item.rubricCriteria?.length ? Object.values(rubricScores).reduce((total, value) => total + value, 0) : draft.score;
    if (score === undefined || score === '') return toast.error('Enter a score first.');
    if (Number(score) > item.maxScore) return toast.error(`Score cannot exceed ${item.maxScore}.`);
    try {
      await evaluationAPI.submit(item.answerId, { score: Number(score), feedback: draft.feedback ?? '', rubricScores });
      setItems((all) => all.filter((entry) => entry.answerId !== item.answerId));
      toast.success(item.requiredReviewCount > 1 ? 'Review submitted for consensus.' : 'Evaluation submitted.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to submit evaluation.'); }
  };

  const reviewFile = async (item) => {
    try {
      const { data } = await evaluationAPI.downloadFile(item.answerId);
      const url = URL.createObjectURL(data);
      if (item.fileUrl?.toLowerCase().endsWith('.pdf')) setPreview({ url, name: item.questionTitle });
      else { const link = document.createElement('a'); link.href = url; link.download = item.fileUrl?.split('/').pop() || 'submission-file'; link.click(); URL.revokeObjectURL(url); }
    } catch { toast.error('Unable to open this submission file.'); }
  };

  const saveAssignment = async (item) => {
    const evaluatorIds = assignments[item.answerId] ?? item.assignedEvaluatorIds ?? [];
    if (!evaluatorIds.length) return toast.error('Choose at least one evaluator.');
    try { await evaluationAPI.assign(item.answerId, evaluatorIds); setManagement((all) => all.map((entry) => entry.answerId === item.answerId ? { ...entry, assignedEvaluatorIds: evaluatorIds } : entry)); toast.success('Evaluator assignment saved.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to assign evaluators.'); }
  };

  const resolve = async (item) => {
    const draft = resolutions[item.answerId] ?? {};
    if (draft.score === undefined || draft.score === '') return toast.error('Enter the final score.');
    try { await evaluationAPI.resolve(item.answerId, { score: Number(draft.score), feedback: draft.feedback ?? '' }); setManagement((all) => all.filter((entry) => entry.answerId !== item.answerId)); toast.success('Disagreement resolved and final score published.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to resolve this review.'); }
  };

  return (
    <div className="container fade-in evaluator-dashboard">
      <section className="evaluator-hero">
        <div><div className="evaluator-eyebrow">Evaluator workspace</div><h1>My review queue</h1><p>Review assigned candidate work, apply rubric scores, and leave useful feedback.</p></div>
        <div className="evaluator-queue-count"><strong>{loading ? '—' : items.length}</strong><span>waiting for your review</span></div>
      </section>

      {canManage && <ManagementPanel items={management} evaluators={evaluators} assignments={assignments} setAssignments={setAssignments} resolutions={resolutions} setResolutions={setResolutions} onSave={saveAssignment} onResolve={resolve} />}

      <section className="card evaluator-queue-card">
        <div className="card-header"><div><div className="card-title">Assigned reviews</div><div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>Score each response and add candidate-facing feedback.</div></div></div>
        {loading ? <div className="loading-center"><div className="spinner" /></div> : items.length === 0 ? <div className="empty-state"><div className="empty-icon">✓</div><div className="empty-title">All caught up</div><div className="empty-subtitle">There are no pending evaluations assigned to you.</div></div> : <div className="question-stack">{items.map((item) => <ReviewCard key={item.answerId} item={item} draft={drafts[item.answerId] ?? {}} setDraft={(draft) => setDrafts({ ...drafts, [item.answerId]: draft })} onFile={() => reviewFile(item)} onSubmit={() => submit(item)} />)}</div>}
      </section>

      {preview && <div className="modal-backdrop"><div className="card" style={{ width: 'min(960px, 96vw)', height: '85vh', padding: 16 }}><div className="card-header"><div className="card-title">PDF preview · {preview.name}</div><button className="btn btn-ghost btn-sm" onClick={() => { URL.revokeObjectURL(preview.url); setPreview(null); }}>Close</button></div><iframe title="Submission PDF preview" src={preview.url} style={{ width: '100%', height: 'calc(100% - 50px)', border: 0 }} /></div></div>}
    </div>
  );
}

function ReviewCard({ item, draft, setDraft, onFile, onSubmit }) {
  const criteria = item.rubricCriteria ?? [];
  const rubricScore = criteria.reduce((total, criterion, index) => total + Number(draft.rubricScores?.[index] ?? 0), 0);
  const plainText = (value) => String(value || '').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').trim();
  return <article className="choice-option evaluator-review-card"><div className="evaluator-review-meta"><strong>{item.candidateName}</strong><span>{item.questionTitle} · Max score: {item.maxScore} · Reviews: {item.reviewCount}/{item.requiredReviewCount}</span></div><div className="evaluator-workspace"><div className="evaluator-response-column"><section><div className="evaluator-panel-label">Question</div><h3>{item.questionTitle}</h3><div className="evaluator-question-content">{plainText(item.questionContent) || 'Question details are unavailable.'}</div></section><section><div className="evaluator-panel-label">Candidate response</div><div className="evaluator-answer-content">{plainText(item.answerText) || (item.fileUrl ? 'Candidate uploaded a file for this response.' : 'No answer content available.')}</div>{item.fileUrl && <button className="btn btn-secondary btn-sm" onClick={onFile}>{item.fileUrl.toLowerCase().endsWith('.pdf') ? 'Preview PDF' : 'Download submission file'}</button>}</section></div><aside className="evaluator-rubric-panel"><div className="evaluator-panel-label">Rubric & scoring</div>{criteria.length > 0 ? <div className="evaluator-rubric">{criteria.map((criterion, index) => <label key={criterion.name}><span>{criterion.name} <small>(max {criterion.maxPoints})</small></span><input className="input" type="number" min="0" max={criterion.maxPoints} value={draft.rubricScores?.[index] ?? ''} onChange={(event) => { const rubricScores = [...(draft.rubricScores ?? [])]; rubricScores[index] = event.target.value; setDraft({ ...draft, rubricScores }); }} /></label>)}</div> : <p className="text-secondary">No rubric is set for this question. Enter a total score below.</p>}<div className="evaluator-submit-row"><input className="input" type="number" min="0" max={item.maxScore} placeholder="Score" readOnly={criteria.length > 0} value={criteria.length ? rubricScore : (draft.score ?? '')} onChange={(event) => setDraft({ ...draft, score: event.target.value })} /><input className="input" placeholder="Feedback for candidate" value={draft.feedback ?? ''} onChange={(event) => setDraft({ ...draft, feedback: event.target.value })} /><button className="btn btn-primary btn-sm" onClick={onSubmit}>Submit review</button></div></aside></div></article>;
}

function ManagementPanel({ items, evaluators, assignments, setAssignments, resolutions, setResolutions, onSave, onResolve }) {
  const actionItems = items.filter((item) => {
    const selected = assignments[item.answerId] ?? item.assignedEvaluatorIds ?? [];
    return selected.length === 0 || item.reviewCount >= 2;
  });
  return <section className="card" style={{ marginBottom: 20 }}><div className="card-header"><div className="card-title">Review assignments & disagreements</div></div>{actionItems.length === 0 ? <div className="text-secondary">No reviews currently need assignment or resolution.</div> : <div className="question-stack">{actionItems.map((item) => { const selected = assignments[item.answerId] ?? item.assignedEvaluatorIds ?? []; const resolution = resolutions[item.answerId] ?? {}; return <div className="choice-option" key={item.answerId} style={{ display: 'block' }}><strong>{item.candidateName} · {item.questionTitle}</strong><div className="text-secondary" style={{ margin: '5px 0 10px' }}>Reviews: {item.reviewCount}/{item.requiredReviewCount}</div>{selected.length === 0 && <><div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>{evaluators.map((evaluator) => <label key={evaluator.id}><input type="checkbox" checked={selected.includes(evaluator.id)} onChange={() => setAssignments({ ...assignments, [item.answerId]: selected.includes(evaluator.id) ? selected.filter((id) => id !== evaluator.id) : [...selected, evaluator.id].slice(0, 2) })} /> {evaluator.name}</label>)}</div><button className="btn btn-secondary btn-sm" style={{ marginTop: 10 }} onClick={() => onSave(item)}>Save assignments</button></>}{item.reviewCount >= 2 && <div className="evaluator-resolution-row"><input className="input" type="number" min="0" max={item.maxScore} placeholder="Final score" value={resolution.score ?? ''} onChange={(event) => setResolutions({ ...resolutions, [item.answerId]: { ...resolution, score: event.target.value } })} /><input className="input" placeholder="Resolution note" value={resolution.feedback ?? ''} onChange={(event) => setResolutions({ ...resolutions, [item.answerId]: { ...resolution, feedback: event.target.value } })} /><button className="btn btn-primary btn-sm" onClick={() => onResolve(item)}>Resolve</button></div>}</div>; })}</div>}</section>;
}
