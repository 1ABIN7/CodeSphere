import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { categoryAPI, questionBankAPI } from '../../api';

const EMPTY_FORM = {
  title: '', content: '', category: '', questionType: 'MCQ_SINGLE', difficulty: 'MEDIUM',
  tags: '', options: '', correctAnswers: '', codingProblemId: '', passageText: '', readingDurationSeconds: 0, minWordCount: 0, maxWordCount: 2000, points: 1, negativeScore: 0, subQuestions: [],
};

export default function QuestionBankPage() {
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const [versionQuestion, setVersionQuestion] = useState(null);
  const [versions, setVersions] = useState([]);
  const [rubricQuestion, setRubricQuestion] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [categories, setCategories] = useState([]);

  const loadQuestions = async () => {
    setLoading(true);
    try {
      const { data } = await questionBankAPI.list();
      setQuestions(Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []);
    } catch { toast.error('Unable to load the question bank.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadQuestions(); categoryAPI.list().then(({ data }) => setCategories(data ?? [])).catch(() => {}); }, []);
  const flatCategories = (items, depth = 0) => items.flatMap((item) => [{ ...item, depth }, ...flatCategories(item.children ?? [], depth + 1)]);

  const visibleQuestions = useMemo(() => questions.filter((question) => {
    const text = `${question.title ?? ''} ${question.content ?? ''} ${question.category ?? ''}`.toLowerCase();
    return text.includes(query.trim().toLowerCase());
  }), [questions, query]);

  const openCreate = () => { setEditing(null); setForm(EMPTY_FORM); setEditorOpen(true); };
  const openEdit = (question) => {
    setEditing(question);
    setForm({
      title: question.title ?? '', content: question.content ?? '', category: question.category ?? '',
      questionType: question.questionType ?? question.type ?? 'MCQ_SINGLE', difficulty: question.difficulty ?? 'MEDIUM',
      tags: Array.isArray(question.tags) ? question.tags.join(', ') : question.tags ?? '',
      options: Array.isArray(question.options) ? question.options.join('\n') : '',
      codingProblemId: question.codingProblemId ?? '',
      passageText: question.passageText ?? '', readingDurationSeconds: question.readingDurationSeconds ?? 0, minWordCount: question.minWordCount ?? 0, maxWordCount: question.maxWordCount ?? 2000,
      correctAnswers: question.correctAnswers ?? '', points: question.points ?? 1, negativeScore: question.negativeScore ?? 0,
      subQuestions: (question.subQuestions ?? []).map((subQuestion) => ({ title: subQuestion.title ?? '', questionType: subQuestion.questionType ?? 'MCQ_SINGLE', content: subQuestion.content ?? '', options: Array.isArray(subQuestion.options) ? subQuestion.options.join('\n') : '', correctAnswers: subQuestion.correctAnswers ?? '', points: subQuestion.points ?? 1 })),
    });
    setEditorOpen(true);
  };

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = { ...form, type: form.questionType, codingProblemId: form.codingProblemId ? Number(form.codingProblemId) : null, readingDurationSeconds: Number(form.readingDurationSeconds) || 0, points: Number(form.points), negativeScore: Number(form.negativeScore), tags: form.tags.split(',').map((tag) => tag.trim()).filter(Boolean), options: form.options.split('\n').map((option) => option.trim()).filter(Boolean), subQuestions: form.questionType === 'READING_COMPREHENSION' ? form.subQuestions.filter((subQuestion) => subQuestion.title.trim() && subQuestion.content.trim()).map((subQuestion) => ({ title: subQuestion.title.trim(), content: subQuestion.content.trim(), questionType: subQuestion.questionType || 'MCQ_SINGLE', type: subQuestion.questionType || 'MCQ_SINGLE', difficulty: form.difficulty, options: (subQuestion.questionType || 'MCQ_SINGLE').startsWith('MCQ') ? subQuestion.options.split('\n').map((option) => option.trim()).filter(Boolean) : [], correctAnswers: subQuestion.correctAnswers.trim(), points: Number(subQuestion.points) || 1, negativeScore: 0, category: form.category, tags: [] })) : [] };
    try {
      const { data } = editing ? await questionBankAPI.update(editing.id, payload) : await questionBankAPI.create(payload);
      setQuestions((items) => editing ? items.map((item) => item.id === data.id ? data : item) : [data, ...items]);
      setEditorOpen(false);
      toast.success(editing ? 'Question updated.' : 'Question added to the bank.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to save the question.'); }
    finally { setSaving(false); }
  };

  const remove = async (question) => {
    if (!window.confirm(`Delete “${question.title}”?`)) return;
    try { await questionBankAPI.delete(question.id); setQuestions((items) => items.filter((item) => item.id !== question.id)); toast.success('Question deleted.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to delete the question.'); }
  };

  const updateApproval = async (question, action) => {
    try {
      const feedback = action === 'reject' ? window.prompt('Rejection feedback:') : null;
      if (action === 'reject' && feedback === null) return;
      const request = action === 'approve' ? questionBankAPI.approve(question.id) : action === 'reject' ? questionBankAPI.reject(question.id, feedback) : questionBankAPI.submitForApproval(question.id);
      const { data } = await request;
      setQuestions((items) => items.map((item) => item.id === data.id ? data : item));
      toast.success(action === 'approve' ? 'Question approved.' : action === 'reject' ? 'Question rejected.' : 'Question submitted for approval.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to update approval status.'); }
  };

  const openVersions = async (question) => {
    try {
      const { data } = await questionBankAPI.versions(question.id);
      setVersions(data ?? []);
      setVersionQuestion(question);
    } catch { toast.error('Unable to load question history.'); }
  };

  const restoreVersion = async (version) => {
    if (!versionQuestion || !window.confirm(`Restore version ${version.versionNumber}? Your current version is kept in the history.`)) return;
    try {
      const { data } = await questionBankAPI.restoreVersion(versionQuestion.id, version.versionNumber);
      setQuestions((items) => items.map((item) => item.id === data.id ? data : item));
      setVersionQuestion(null);
      toast.success(`Restored version ${version.versionNumber}.`);
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to restore this version.'); }
  };

  const openRubric = async (question) => {
    try {
      const { data } = await questionBankAPI.getRubric(question.id);
      setCriteria((data.criteria ?? []).map((criterion) => ({ criterionName: criterion.criterionName, maxPoints: criterion.maxPoints })));
    } catch { setCriteria([]); }
    setRubricQuestion(question);
  };

  const saveRubric = async () => {
    if (!rubricQuestion) return;
    const valid = criteria.filter((criterion) => criterion.criterionName.trim() && Number(criterion.maxPoints) > 0)
      .map((criterion) => ({ criterionName: criterion.criterionName.trim(), maxPoints: Number(criterion.maxPoints) }));
    if (!valid.length) return toast.error('Add at least one rubric criterion.');
    try { await questionBankAPI.saveRubric(rubricQuestion.id, valid); setRubricQuestion(null); toast.success('Rubric saved.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to save rubric.'); }
  };

  const importQuestions = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setImporting(true);
    try { const formData = new FormData(); formData.append('file', file); await questionBankAPI.import(formData); await loadQuestions(); toast.success('Questions imported.'); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to import questions.'); }
    finally { setImporting(false); event.target.value = ''; }
  };

  const exportQuestions = async (format) => {
    try { const { data } = await questionBankAPI.export(format); const url = URL.createObjectURL(data); const link = document.createElement('a'); link.href = url; link.download = `questions.${format}`; link.click(); URL.revokeObjectURL(url); }
    catch { toast.error('Unable to export questions.'); }
  };

  return <div className="container fade-in">
    <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
      <div><h1 className="page-title">Question Bank</h1><p className="page-subtitle">Create, edit, import, and organize assessment questions.</p></div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><button className="btn btn-secondary btn-sm" onClick={() => exportQuestions('csv')}>Export CSV</button><label className="btn btn-secondary btn-sm" style={{ cursor: 'pointer' }}>{importing ? 'Importing...' : 'Import'}<input type="file" accept=".csv,.json" onChange={importQuestions} style={{ display: 'none' }} disabled={importing} /></label><button className="btn btn-primary" onClick={openCreate}>+ Create question</button></div>
    </div>
    <div className="card">
      <input className="input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search questions..." style={{ marginBottom: 16 }} />
      {loading ? <div className="loading-center"><div className="spinner" /></div> : visibleQuestions.length === 0 ? <div className="empty-state"><div className="empty-title">No matching questions</div><div className="empty-subtitle">Create the first question for this bank.</div></div> :
        <div className="question-stack">{visibleQuestions.map((question) => <div key={question.id} className="choice-option" style={{ justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
          <div><strong>{question.title}</strong><div className="text-secondary" style={{ marginTop: 5, fontSize: 13 }}>{question.content}</div><div style={{ marginTop: 8, display: 'flex', gap: 8 }}><span className="badge badge-default">{question.questionType ?? question.type}</span><span className="badge badge-tag">{question.difficulty}</span><span className="badge badge-default">{question.status || 'DRAFT'}</span></div></div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}><button className="btn btn-secondary btn-sm" onClick={() => openEdit(question)}>Edit</button><button className="btn btn-secondary btn-sm" onClick={() => openVersions(question)}>History</button>{['WRITTEN', 'SUBJECTIVE', 'FILE_UPLOAD'].includes(question.questionType) && <button className="btn btn-secondary btn-sm" onClick={() => openRubric(question)}>Rubric</button>}{question.status === 'DRAFT' && <button className="btn btn-secondary btn-sm" onClick={() => updateApproval(question, 'submit')}>Submit</button>}{question.status === 'PENDING_APPROVAL' && <><button className="btn btn-primary btn-sm" onClick={() => updateApproval(question, 'approve')}>Approve</button><button className="btn btn-ghost btn-sm" onClick={() => updateApproval(question, 'reject')}>Reject</button></>}<button className="btn btn-ghost btn-sm" onClick={() => remove(question)}>Delete</button></div>
        </div>)}</div>}
    </div>
    {editorOpen && <div className="modal-backdrop" role="dialog" aria-modal="true"><form className="card" onSubmit={save} style={{ width: 'min(720px, 94vw)', maxHeight: '90vh', overflowY: 'auto', padding: 24 }}>
      <div className="card-header"><div className="card-title">{editing ? 'Edit question' : 'Create question'}</div><button className="btn btn-ghost btn-sm" type="button" onClick={() => setEditorOpen(false)}>Close</button></div>
      <div className="form-group"><label className="label">Title</label><input className="input" required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></div>
      <div className="form-group" style={{ marginTop: 14 }}><label className="label">Question content</label><textarea className="textarea" required value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} /></div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 14 }}><label className="label">Type<select className="select" value={form.questionType} onChange={(event) => setForm({ ...form, questionType: event.target.value })}><option value="MCQ_SINGLE">Multiple choice</option><option value="MCQ_MULTI">Multiple select</option><option value="WRITTEN">Written</option><option value="CODING">Algorithmic coding</option><option value="SQL">SQL query</option><option value="API_IMPLEMENTATION">API implementation</option><option value="DEBUGGING">Debugging task</option><option value="READING_COMPREHENSION">Reading comprehension</option><option value="FILE_UPLOAD">File upload</option></select></label><label className="label">Difficulty<select className="select" value={form.difficulty} onChange={(event) => setForm({ ...form, difficulty: event.target.value })}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label><label className="label">Category{categories.length ? <select className="select" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}><option value="">Select a category</option>{flatCategories(categories).map((category) => <option key={category.id} value={category.name}>{'— '.repeat(category.depth)}{category.name}</option>)}</select> : <input className="input" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} placeholder="Create categories first" />}</label></div>
      {(form.questionType === 'MCQ_SINGLE' || form.questionType === 'MCQ_MULTI') && <label className="label" style={{ display: 'block', marginTop: 14 }}>Answer choices (one per line)<textarea className="textarea" required value={form.options} onChange={(event) => setForm({ ...form, options: event.target.value })} placeholder={'First choice\nSecond choice\nThird choice'} /></label>}
      {['CODING', 'API_IMPLEMENTATION', 'DEBUGGING'].includes(form.questionType) && <label className="label" style={{ display: 'block', marginTop: 14 }}>Linked judge problem ID<input className="input" required type="number" min="1" value={form.codingProblemId} onChange={(event) => setForm({ ...form, codingProblemId: event.target.value })} placeholder="Existing problem ID" /><small className="text-secondary">Use a coding problem with test cases. API and debugging tasks use the same secure judge.</small></label>}
      {form.questionType === 'SQL' && <div className="text-secondary" style={{ marginTop: 14 }}>SQL answers are submitted in a query editor and sent to the evaluator queue for review.</div>}
      {form.questionType === 'READING_COMPREHENSION' && <div style={{ display: 'grid', gridTemplateColumns: '1fr 180px', gap: 12, marginTop: 14 }}><label className="label">Reading passage<textarea className="textarea" required value={form.passageText} onChange={(event) => setForm({ ...form, passageText: event.target.value })} /></label><label className="label">Reading time (seconds)<input className="input" type="number" min="0" value={form.readingDurationSeconds} onChange={(event) => setForm({ ...form, readingDurationSeconds: event.target.value })} /><small className="text-secondary">Questions unlock when this ends.</small></label></div>}
      {form.questionType === 'READING_COMPREHENSION' && <div style={{ marginTop: 14 }}><div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><label className="label">Passage questions</label><button className="btn btn-secondary btn-sm" type="button" onClick={() => setForm({ ...form, subQuestions: [...form.subQuestions, { title: `Question ${form.subQuestions.length + 1}`, questionType: 'MCQ_SINGLE', content: '', options: '', correctAnswers: '', points: 1 }] })}>+ Add question</button></div>{form.subQuestions.map((subQuestion, index) => <div className="card" key={index} style={{ padding: 14, marginTop: 10 }}><div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}><strong>Passage question {index + 1}</strong><button className="btn btn-ghost btn-sm" type="button" onClick={() => setForm({ ...form, subQuestions: form.subQuestions.filter((_, itemIndex) => itemIndex !== index) })}>Remove</button></div><div style={{ display: 'grid', gridTemplateColumns: '1fr 180px', gap: 12, marginTop: 8 }}><label className="label">Title<input className="input" value={subQuestion.title} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, title: event.target.value } : item) })} /></label><label className="label">Answer type<select className="select" value={subQuestion.questionType || 'MCQ_SINGLE'} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, questionType: event.target.value } : item) })}><option value="MCQ_SINGLE">Multiple choice</option><option value="SHORT_ANSWER">Short answer</option></select></label></div><label className="label" style={{ display: 'block', marginTop: 8 }}>Question<textarea className="textarea" value={subQuestion.content} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, content: event.target.value } : item) })} /></label>{(subQuestion.questionType || 'MCQ_SINGLE') === 'MCQ_SINGLE' && <><label className="label" style={{ display: 'block', marginTop: 8 }}>Choices (one per line)<textarea className="textarea" value={subQuestion.options} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, options: event.target.value } : item) })} /></label><label className="label" style={{ display: 'block', marginTop: 8 }}>Correct label<input className="input" placeholder="A" value={subQuestion.correctAnswers} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, correctAnswers: event.target.value } : item) })} /></label></>}<label className="label" style={{ display: 'block', marginTop: 8, maxWidth: 110 }}>Points<input className="input" type="number" min="1" value={subQuestion.points} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, points: event.target.value } : item) })} /></label></div>)}</div>}
      {(form.questionType === 'WRITTEN' || form.questionType === 'READING_COMPREHENSION') && <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 14 }}><label className="label">Minimum words<input className="input" type="number" min="0" value={form.minWordCount} onChange={(event) => setForm({ ...form, minWordCount: event.target.value })} /></label><label className="label">Maximum words<input className="input" type="number" min="1" value={form.maxWordCount} onChange={(event) => setForm({ ...form, maxWordCount: event.target.value })} /></label></div>}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px 110px', gap: 12, marginTop: 14 }}><label className="label">Correct label(s)<input className="input" value={form.correctAnswers} onChange={(event) => setForm({ ...form, correctAnswers: event.target.value })} placeholder="A or A,C" /></label><label className="label">Points<input className="input" type="number" min="0" value={form.points} onChange={(event) => setForm({ ...form, points: event.target.value })} /></label><label className="label">Negative<input className="input" type="number" min="0" value={form.negativeScore} onChange={(event) => setForm({ ...form, negativeScore: event.target.value })} /></label></div>
      <label className="label" style={{ display: 'block', marginTop: 14 }}>Tags (comma separated)<input className="input" value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} /></label>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button className="btn btn-secondary" type="button" onClick={() => setEditorOpen(false)}>Cancel</button><button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save question'}</button></div>
    </form></div>}
    {versionQuestion && <div className="modal-backdrop" role="dialog" aria-modal="true"><div className="card" style={{ width: 'min(620px, 94vw)', maxHeight: '80vh', overflowY: 'auto', padding: 24 }}><div className="card-header"><div className="card-title">Version history · {versionQuestion.title}</div><button className="btn btn-ghost btn-sm" onClick={() => setVersionQuestion(null)}>Close</button></div>{versions.length === 0 ? <div className="empty-state"><div className="empty-subtitle">No earlier versions yet. Editing this question creates one.</div></div> : <div className="question-stack">{versions.map((version) => <div className="choice-option" key={version.id} style={{ justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}><div><strong>Version {version.versionNumber}</strong><div className="text-secondary" style={{ fontSize: 13, marginTop: 5 }}>{new Date(version.createdAt).toLocaleString()}</div><div style={{ marginTop: 8 }}>{version.content}</div></div><button className="btn btn-secondary btn-sm" onClick={() => restoreVersion(version)}>Restore</button></div>)}</div>}</div></div>}
    {rubricQuestion && <div className="modal-backdrop" role="dialog" aria-modal="true"><div className="card" style={{ width: 'min(620px, 94vw)', padding: 24 }}><div className="card-header"><div className="card-title">Rubric · {rubricQuestion.title}</div><button className="btn btn-ghost btn-sm" onClick={() => setRubricQuestion(null)}>Close</button></div><div className="text-secondary" style={{ fontSize: 13, marginBottom: 12 }}>Set the scoring criteria reviewers use for this question.</div>{criteria.map((criterion, index) => <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px auto', gap: 10, marginTop: 8 }} key={index}><input className="input" placeholder="Criterion, e.g. Evidence" value={criterion.criterionName} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, criterionName: event.target.value } : item))} /><input className="input" type="number" min="1" placeholder="Points" value={criterion.maxPoints} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, maxPoints: event.target.value } : item))} /><button className="btn btn-ghost btn-sm" onClick={() => setCriteria(criteria.filter((_, itemIndex) => itemIndex !== index))}>Remove</button></div>)}<button className="btn btn-secondary btn-sm" style={{ marginTop: 12 }} onClick={() => setCriteria([...criteria, { criterionName: '', maxPoints: 1 }])}>+ Add criterion</button><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button className="btn btn-secondary" onClick={() => setRubricQuestion(null)}>Cancel</button><button className="btn btn-primary" onClick={saveRubric}>Save rubric</button></div></div></div>}
  </div>;
}
