import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { categoryAPI, questionBankAPI } from '../../api';

const EMPTY_FORM = {
  title: '', content: '', category: '', questionType: 'MCQ_SINGLE', difficulty: 'MEDIUM',
  tags: '', options: '', correctAnswers: '', codingProblemId: '', sqlSetup: '', sqlTestCases: [{ name: 'Sample dataset', setupSql: '', expectedRows: '[]', hidden: false }], apiTestCases: [{ name: 'Health check', method: 'GET', path: '/health', body: '', expectedStatus: 200, expectedBody: '{"ok":true}', hidden: false }], passageText: '', readingDurationSeconds: 0, minWordCount: 0, maxWordCount: 2000, points: 1, negativeScore: 0, subQuestions: [],
};

const TYPE_GUIDANCE = {
  MCQ_SINGLE: { icon: '◉', title: 'Multiple choice', text: 'Add choices and mark one correct answer.' },
  MCQ_MULTI: { icon: '☑', title: 'Multiple select', text: 'Add choices and mark every correct answer.' },
  WRITTEN: { icon: '✎', title: 'Written response', text: 'Set word limits and add a rubric after saving.' },
  CODING: { icon: '</>', title: 'Algorithmic coding / debugging', text: 'Use Task Center when you need executable test cases and automatic judging.' },
  SQL: { icon: '▤', title: 'SQL query', text: 'Provide a safe dataset and expected query results.' },
  API_IMPLEMENTATION: { icon: '↔', title: 'API implementation', text: 'Define HTTP checks for a candidate-built service.' },
  DEBUGGING: { icon: '⚙', title: 'Debugging task', text: 'Link a coding task that candidates must repair.' },
  READING_COMPREHENSION: { icon: '▱', title: 'Reading comprehension', text: 'Add a passage, timed reading, and nested questions.' },
  FILE_UPLOAD: { icon: '⇧', title: 'File upload', text: 'Ask candidates to submit a document or work sample.' },
};

export default function QuestionBankPage() {
  const navigate = useNavigate();
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [questionTypeFilter, setQuestionTypeFilter] = useState('ALL');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importMode, setImportMode] = useState('UPSERT');
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
    const type = question.questionType ?? question.type ?? '';
    return text.includes(query.trim().toLowerCase()) && (questionTypeFilter === 'ALL' || type === questionTypeFilter);
  }), [questions, query, questionTypeFilter]);
  const chooseQuestionType = (questionType) => setForm({ ...form, questionType, readingDurationSeconds: questionType === 'READING_COMPREHENSION' && Number(form.readingDurationSeconds) <= 0 ? 60 : form.readingDurationSeconds });

  const openCreate = () => { setEditing(null); setCriteria([]); setForm(EMPTY_FORM); setEditorOpen(true); };
  const openEdit = async (question) => {
    if (question.codingProblemId) {
      navigate(`/admin/problems?edit=${question.codingProblemId}`);
      return;
    }
    setEditing(question);
    setForm({
      title: question.title ?? '', content: question.content ?? '', category: question.category ?? '',
      questionType: question.questionType ?? question.type ?? 'MCQ_SINGLE', difficulty: question.difficulty ?? 'MEDIUM',
      tags: Array.isArray(question.tags) ? question.tags.join(', ') : question.tags ?? '',
      options: Array.isArray(question.options) ? question.options.join('\n') : '',
      codingProblemId: question.codingProblemId ?? '',
      sqlSetup: question.sqlSetup ?? '',
      sqlTestCases: (() => { try { return JSON.parse(question.sqlTestCases || '[]').map((testCase) => ({ ...testCase, expectedRows: JSON.stringify(testCase.expectedRows ?? [], null, 2) })); } catch { return []; } })(),
      apiTestCases: (() => { try { return JSON.parse(question.apiTestCases || '[]').map((testCase) => ({ name: testCase.name ?? '', method: testCase.method ?? 'GET', path: testCase.path ?? '/', body: typeof testCase.body === 'string' ? testCase.body : JSON.stringify(testCase.body ?? ''), expectedStatus: testCase.expectedStatus ?? 200, expectedBody: typeof testCase.expectedBody === 'string' ? testCase.expectedBody : JSON.stringify(testCase.expectedBody ?? ''), hidden: Boolean(testCase.hidden) })); } catch { return []; } })(),
      passageText: question.passageText ?? '', readingDurationSeconds: question.readingDurationSeconds ?? 0, minWordCount: question.minWordCount ?? 0, maxWordCount: question.maxWordCount ?? 2000,
      correctAnswers: question.correctAnswers ?? '', points: question.points ?? 1, negativeScore: question.negativeScore ?? 0,
      subQuestions: (question.subQuestions ?? []).map((subQuestion) => ({ title: subQuestion.title ?? '', questionType: subQuestion.questionType ?? 'MCQ_SINGLE', content: subQuestion.content ?? '', options: Array.isArray(subQuestion.options) ? subQuestion.options.join('\n') : '', correctAnswers: subQuestion.correctAnswers ?? '', points: subQuestion.points ?? 1 })),
    });
    try {
      const { data } = await questionBankAPI.getRubric(question.id);
      setCriteria((data.criteria ?? []).map((criterion) => ({ criterionName: criterion.criterionName, maxPoints: criterion.maxPoints })));
    } catch { setCriteria([]); }
    setEditorOpen(true);
  };

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    let sqlTestCases = [];
    try { sqlTestCases = form.questionType === 'SQL' ? form.sqlTestCases.filter((testCase) => testCase.name.trim()).map((testCase) => ({ ...testCase, name: testCase.name.trim(), expectedRows: JSON.parse(testCase.expectedRows || '[]') })) : []; }
    catch { setSaving(false); toast.error('SQL expected rows must be valid JSON, for example [{"id": 1}].'); return; }
    const apiTestCases = form.questionType === 'API_IMPLEMENTATION' ? JSON.stringify(form.apiTestCases.filter((testCase) => testCase.name.trim() && testCase.path.trim()).map((testCase) => ({ ...testCase, name: testCase.name.trim(), path: testCase.path.trim(), expectedStatus: Number(testCase.expectedStatus) || 200, body: testCase.body || null, expectedBody: testCase.expectedBody || null }))) : null;
    if (form.questionType === 'API_IMPLEMENTATION' && JSON.parse(apiTestCases).length === 0) { setSaving(false); toast.error('Add at least one HTTP test case.'); return; }
    const payload = { ...form, type: form.questionType, codingProblemId: form.codingProblemId ? Number(form.codingProblemId) : null, sqlSetup: form.questionType === 'SQL' ? form.sqlSetup : null, sqlTestCases: form.questionType === 'SQL' ? JSON.stringify(sqlTestCases) : null, apiTestCases, readingDurationSeconds: Number(form.readingDurationSeconds) || 0, points: Number(form.points), negativeScore: Number(form.negativeScore), tags: form.tags.split(',').map((tag) => tag.trim()).filter(Boolean), options: form.options.split('\n').map((option) => option.trim()).filter(Boolean), subQuestions: form.questionType === 'READING_COMPREHENSION' ? form.subQuestions.filter((subQuestion) => subQuestion.title.trim() && subQuestion.content.trim()).map((subQuestion) => ({ title: subQuestion.title.trim(), content: subQuestion.content.trim(), questionType: subQuestion.questionType || 'MCQ_SINGLE', type: subQuestion.questionType || 'MCQ_SINGLE', difficulty: form.difficulty, options: (subQuestion.questionType || 'MCQ_SINGLE').startsWith('MCQ') ? subQuestion.options.split('\n').map((option) => option.trim()).filter(Boolean) : [], correctAnswers: subQuestion.correctAnswers.trim(), points: Number(subQuestion.points) || 1, negativeScore: 0, category: form.category, tags: [] })) : [] };
    try {
      const { data } = editing ? await questionBankAPI.update(editing.id, payload) : await questionBankAPI.create(payload);
      const rubricCriteria = criteria.filter((criterion) => criterion.criterionName.trim() && Number(criterion.maxPoints) > 0)
        .map((criterion) => ({ criterionName: criterion.criterionName.trim(), maxPoints: Number(criterion.maxPoints) }));
      if (form.questionType === 'WRITTEN' && rubricCriteria.length) await questionBankAPI.saveRubric(data.id, rubricCriteria);
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
    try { const formData = new FormData(); formData.append('file', file); const { data } = await questionBankAPI.import(formData, importMode); await loadQuestions(); toast.success(`${data?.length ?? 0} question(s) ${importMode === 'CREATE' ? 'created' : 'created or updated'}.`); }
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
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}><Link className="btn btn-secondary btn-sm" to="/admin/problems">Task Center</Link><button className="btn btn-secondary btn-sm" onClick={() => exportQuestions('csv')}>Export CSV</button><select className="select" aria-label="Question import mode" value={importMode} onChange={(event) => setImportMode(event.target.value)} style={{ width: 185 }}><option value="UPSERT">Update matching IDs</option><option value="CREATE">Create new copies</option></select><label className="btn btn-secondary btn-sm" style={{ cursor: 'pointer' }}>{importing ? 'Importing...' : 'Import'}<input type="file" accept=".csv,.json" onChange={importQuestions} style={{ display: 'none' }} disabled={importing} /></label><button className="btn btn-primary" onClick={openCreate}>+ Create question</button></div>
    </div>
    <div className="card">
      <div className="question-bank-filters"><input className="input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search questions..." /><select className="select" value={questionTypeFilter} onChange={(event) => setQuestionTypeFilter(event.target.value)}><option value="ALL">All question types</option><option value="MCQ_SINGLE">Multiple choice</option><option value="MCQ_MULTI">Multiple select</option><option value="WRITTEN">Written</option><option value="CODING">Algorithmic coding</option><option value="DEBUGGING">Debugging</option><option value="SQL">SQL query</option><option value="API_IMPLEMENTATION">API implementation</option><option value="READING_COMPREHENSION">Reading comprehension</option><option value="FILE_UPLOAD">File upload</option></select></div>
      {loading ? <div className="loading-center"><div className="spinner" /></div> : visibleQuestions.length === 0 ? <div className="empty-state"><div className="empty-title">No matching questions</div><div className="empty-subtitle">Create the first question for this bank.</div></div> :
        <div className="question-stack">{visibleQuestions.map((question) => <div key={question.id} className="choice-option" style={{ justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
          <div><strong>{question.title}</strong><div className="text-secondary" style={{ marginTop: 5, fontSize: 13 }}>{question.content}</div><div style={{ marginTop: 8, display: 'flex', gap: 8 }}><span className="badge badge-default">{question.questionType ?? question.type}</span><span className="badge badge-tag">{question.difficulty}</span><span className="badge badge-default">{question.status || 'DRAFT'}</span></div></div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}><button className="btn btn-secondary btn-sm" onClick={() => openEdit(question)}>Edit</button><button className="btn btn-secondary btn-sm" onClick={() => openVersions(question)}>History</button>{!question.systemGenerated && <>{['WRITTEN', 'SUBJECTIVE', 'FILE_UPLOAD'].includes(question.questionType) && <button className="btn btn-secondary btn-sm" onClick={() => openRubric(question)}>Rubric</button>}{question.status === 'DRAFT' && <button className="btn btn-secondary btn-sm" onClick={() => updateApproval(question, 'submit')}>Submit</button>}{question.status === 'PENDING_APPROVAL' && <><button className="btn btn-primary btn-sm" onClick={() => updateApproval(question, 'approve')}>Approve</button><button className="btn btn-ghost btn-sm" onClick={() => updateApproval(question, 'reject')}>Reject</button></>}<button className="btn btn-ghost btn-sm" onClick={() => remove(question)}>Delete</button></>}</div>
        </div>)}</div>}
    </div>
    {editorOpen && <div className="modal-backdrop" role="dialog" aria-modal="true"><form className="card question-editor" onSubmit={save} style={{ width: 'min(800px, 94vw)', maxHeight: '90vh', overflowY: 'auto', padding: 28 }}>
      <div className="card-header"><div className="card-title">{editing ? 'Edit question' : 'Create question'}</div><button className="btn btn-ghost btn-sm" type="button" onClick={() => setEditorOpen(false)}>Close</button></div>
      <label className="label" style={{ display: 'block', marginBottom: 12 }}>Question type<select className="select" value={form.questionType} onChange={(event) => chooseQuestionType(event.target.value)}><option value="MCQ_SINGLE">Multiple choice</option><option value="MCQ_MULTI">Multiple select</option><option value="WRITTEN">Written</option><option value="CODING">Algorithmic coding / debugging</option><option value="SQL">SQL query</option><option value="API_IMPLEMENTATION">API implementation</option><option value="READING_COMPREHENSION">Reading comprehension</option><option value="FILE_UPLOAD">File upload</option></select></label>
      <QuestionTypeGuide type={form.questionType} />
      {form.questionType === 'CODING' && !editing && <div className="coding-task-center-link"><div><strong>Need an executable coding or debugging task?</strong><p>Use Task Center to add test cases and automatic judging. It will create a Question Bank entry for you.</p></div><Link className="btn btn-secondary btn-sm" to="/admin/problems">Open Task Center</Link></div>}
      {(form.questionType !== 'CODING' || editing) && <><div className="form-group"><label className="label">Title</label><input className="input" required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></div>
      <div className="form-group" style={{ marginTop: 14 }}><label className="label">Question content</label><textarea className="textarea" required value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} /></div></>}
      {form.questionType === 'WRITTEN' && <RubricEditor criteria={criteria} setCriteria={setCriteria} />}
      {(form.questionType !== 'CODING' || editing) && <div style={{ display: 'grid', gridTemplateColumns: form.questionType === 'FILE_UPLOAD' ? '1fr 1fr' : '1fr 1fr 1fr', gap: 12, marginTop: 14 }}>{form.questionType !== 'FILE_UPLOAD' && <label className="label">Difficulty<select className="select" value={form.difficulty} onChange={(event) => setForm({ ...form, difficulty: event.target.value })}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label>}<label className="label">Category{categories.length ? <select className="select" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}><option value="">Select a category</option>{flatCategories(categories).map((category) => <option key={category.id} value={category.name}>{'— '.repeat(category.depth)}{category.name}</option>)}</select> : <input className="input" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} placeholder="Create categories first" />}</label><label className="label">Tags<input className="input" value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} placeholder="arrays, beginner" /></label></div>}
      {(form.questionType === 'MCQ_SINGLE' || form.questionType === 'MCQ_MULTI') && <label className="label" style={{ display: 'block', marginTop: 14 }}>Answer choices (one per line)<textarea className="textarea" required value={form.options} onChange={(event) => setForm({ ...form, options: event.target.value })} placeholder={'First choice\nSecond choice\nThird choice'} /></label>}
      {form.questionType === 'API_IMPLEMENTATION' && <div style={{ marginTop: 14 }}><div className="text-secondary">Candidate code must be a JavaScript Node HTTP server listening on port 3000. Every request runs inside the same network-disabled container as that server.</div><div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}><strong>HTTP test cases</strong><button type="button" className="btn btn-secondary btn-sm" onClick={() => setForm({ ...form, apiTestCases: [...form.apiTestCases, { name: `HTTP check ${form.apiTestCases.length + 1}`, method: 'GET', path: '/', body: '', expectedStatus: 200, expectedBody: '', hidden: true }] })}>+ Add test</button></div>{form.apiTestCases.map((testCase, index) => <div className="card" key={index} style={{ padding: 12, marginTop: 10 }}><div style={{ display: 'grid', gridTemplateColumns: '1fr 120px 1fr auto', gap: 10, alignItems: 'end' }}><label className="label">Name<input className="input" value={testCase.name} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item) })} /></label><label className="label">Method<select className="select" value={testCase.method} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, method: event.target.value } : item) })}><option>GET</option><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option></select></label><label className="label">Path<input className="input" value={testCase.path} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, path: event.target.value } : item) })} placeholder="/health" /></label><div><label className="text-secondary" style={{ fontSize: 12 }}><input type="checkbox" checked={Boolean(testCase.hidden)} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, hidden: event.target.checked } : item) })} /> Hidden</label><button type="button" className="btn btn-ghost btn-sm" onClick={() => setForm({ ...form, apiTestCases: form.apiTestCases.filter((_, itemIndex) => itemIndex !== index) })}>Remove</button></div></div><div className="problem-authoring-grid" style={{ marginTop: 10 }}><label className="label">Request body (optional)<textarea className="textarea" value={testCase.body} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, body: event.target.value } : item) })} placeholder='{"key":"value"}' /></label><label className="label">Expected body (optional)<textarea className="textarea" value={testCase.expectedBody} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, expectedBody: event.target.value } : item) })} placeholder='{"ok":true}' /></label></div><label className="label" style={{ display: 'block', marginTop: 8, maxWidth: 180 }}>Expected status<input className="input" type="number" min="100" max="599" value={testCase.expectedStatus} onChange={(event) => setForm({ ...form, apiTestCases: form.apiTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, expectedStatus: event.target.value } : item) })} /></label></div>)}</div>}
      {form.questionType === 'SQL' && <div style={{ marginTop: 14 }}><div className="text-secondary">Each candidate query runs only as a read-only SELECT in a fresh, isolated database for every test case.</div><label className="label" style={{ display: 'block', marginTop: 10 }}>Shared schema and seed SQL<textarea className="textarea" required value={form.sqlSetup} onChange={(event) => setForm({ ...form, sqlSetup: event.target.value })} placeholder={'CREATE TABLE employees (id INT, name VARCHAR(80));\nINSERT INTO employees VALUES (1, \'Ada\');'} /></label><div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}><strong>Automated SQL test cases</strong><button type="button" className="btn btn-secondary btn-sm" onClick={() => setForm({ ...form, sqlTestCases: [...form.sqlTestCases, { name: `Case ${form.sqlTestCases.length + 1}`, setupSql: '', expectedRows: '[]', hidden: true }] })}>+ Add test</button></div>{form.sqlTestCases.map((testCase, index) => <div className="card" key={index} style={{ padding: 12, marginTop: 10 }}><div style={{ display: 'flex', gap: 10, alignItems: 'end' }}><label className="label" style={{ flex: 1 }}>Name<input className="input" value={testCase.name} onChange={(event) => setForm({ ...form, sqlTestCases: form.sqlTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item) })} /></label><label><input type="checkbox" checked={Boolean(testCase.hidden)} onChange={(event) => setForm({ ...form, sqlTestCases: form.sqlTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, hidden: event.target.checked } : item) })} /> Hidden</label><button type="button" className="btn btn-ghost btn-sm" onClick={() => setForm({ ...form, sqlTestCases: form.sqlTestCases.filter((_, itemIndex) => itemIndex !== index) })}>Remove</button></div><label className="label" style={{ display: 'block', marginTop: 8 }}>Additional setup for this case<textarea className="textarea" value={testCase.setupSql} onChange={(event) => setForm({ ...form, sqlTestCases: form.sqlTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, setupSql: event.target.value } : item) })} placeholder="Optional INSERT statements" /></label><label className="label" style={{ display: 'block', marginTop: 8 }}>Expected rows (JSON)<textarea className="textarea" required value={testCase.expectedRows} onChange={(event) => setForm({ ...form, sqlTestCases: form.sqlTestCases.map((item, itemIndex) => itemIndex === index ? { ...item, expectedRows: event.target.value } : item) })} placeholder={'[{"id": 1, "name": "Ada"}]'} /></label></div>)}</div>}
      {form.questionType === 'READING_COMPREHENSION' && <div style={{ display: 'grid', gridTemplateColumns: '1fr 180px', gap: 12, marginTop: 14 }}><label className="label">Reading passage<textarea className="textarea" required value={form.passageText} onChange={(event) => setForm({ ...form, passageText: event.target.value })} /></label><label className="label">Reading time (seconds)<input className="input" type="number" min="1" required value={form.readingDurationSeconds} onChange={(event) => setForm({ ...form, readingDurationSeconds: event.target.value })} /><small className="text-secondary">Defaults to 60 seconds. Questions unlock when this ends.</small></label></div>}
      {form.questionType === 'READING_COMPREHENSION' && <div style={{ marginTop: 14 }}><div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><label className="label">Passage questions</label><button className="btn btn-secondary btn-sm" type="button" onClick={() => setForm({ ...form, subQuestions: [...form.subQuestions, { title: `Question ${form.subQuestions.length + 1}`, questionType: 'MCQ_SINGLE', content: '', options: '', correctAnswers: '', points: 1 }] })}>+ Add question</button></div>{form.subQuestions.map((subQuestion, index) => <div className="card" key={index} style={{ padding: 14, marginTop: 10 }}><div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}><strong>Passage question {index + 1}</strong><button className="btn btn-ghost btn-sm" type="button" onClick={() => setForm({ ...form, subQuestions: form.subQuestions.filter((_, itemIndex) => itemIndex !== index) })}>Remove</button></div><div style={{ display: 'grid', gridTemplateColumns: '1fr 180px', gap: 12, marginTop: 8 }}><label className="label">Title<input className="input" value={subQuestion.title} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, title: event.target.value } : item) })} /></label><label className="label">Answer type<select className="select" value={subQuestion.questionType || 'MCQ_SINGLE'} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, questionType: event.target.value } : item) })}><option value="MCQ_SINGLE">Multiple choice</option><option value="SHORT_ANSWER">Short answer</option></select></label></div><label className="label" style={{ display: 'block', marginTop: 8 }}>Question<textarea className="textarea" value={subQuestion.content} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, content: event.target.value } : item) })} /></label>{(subQuestion.questionType || 'MCQ_SINGLE') === 'MCQ_SINGLE' && <><label className="label" style={{ display: 'block', marginTop: 8 }}>Choices (one per line)<textarea className="textarea" value={subQuestion.options} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, options: event.target.value } : item) })} /></label><label className="label" style={{ display: 'block', marginTop: 8 }}>Correct label<input className="input" placeholder="A" value={subQuestion.correctAnswers} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, correctAnswers: event.target.value } : item) })} /></label></>}<label className="label" style={{ display: 'block', marginTop: 8, maxWidth: 110 }}>Points<input className="input" type="number" min="1" value={subQuestion.points} onChange={(event) => setForm({ ...form, subQuestions: form.subQuestions.map((item, itemIndex) => itemIndex === index ? { ...item, points: event.target.value } : item) })} /></label></div>)}</div>}
      {(form.questionType === 'WRITTEN' || form.questionType === 'READING_COMPREHENSION') && <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 14 }}><label className="label">Minimum words<input className="input" type="number" min="0" value={form.minWordCount} onChange={(event) => setForm({ ...form, minWordCount: event.target.value })} /></label><label className="label">Maximum words<input className="input" type="number" min="1" value={form.maxWordCount} onChange={(event) => setForm({ ...form, maxWordCount: event.target.value })} /></label></div>}
      {(form.questionType !== 'CODING' || editing) && <div style={{ display: 'grid', gridTemplateColumns: ['WRITTEN', 'FILE_UPLOAD', 'SQL', 'API_IMPLEMENTATION', 'READING_COMPREHENSION', 'CODING'].includes(form.questionType) ? '1fr 1fr' : '1fr 110px 110px', gap: 12, marginTop: 14 }}>{!['WRITTEN', 'FILE_UPLOAD', 'SQL', 'API_IMPLEMENTATION', 'READING_COMPREHENSION', 'CODING'].includes(form.questionType) && <label className="label">{form.questionType === 'MCQ_SINGLE' ? 'Correct label' : form.questionType === 'MCQ_MULTI' ? 'Correct labels' : 'Correct answer'}<input className="input" value={form.correctAnswers} onChange={(event) => setForm({ ...form, correctAnswers: event.target.value })} placeholder={form.questionType === 'MCQ_MULTI' ? 'Ex: A, C' : 'Ex: A'} /></label>}<label className="label">Points<input className="input" type="number" min="0" value={form.points} onChange={(event) => setForm({ ...form, points: event.target.value })} /></label><label className="label">Negative<input className="input" type="number" min="0" value={form.negativeScore} onChange={(event) => setForm({ ...form, negativeScore: event.target.value })} /></label></div>}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button className="btn btn-secondary" type="button" onClick={() => setEditorOpen(false)}>{form.questionType === 'CODING' && !editing ? 'Close' : 'Cancel'}</button>{(form.questionType !== 'CODING' || editing) && <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save question'}</button>}</div>
    </form></div>}
    {versionQuestion && <div className="modal-backdrop" role="dialog" aria-modal="true"><div className="card" style={{ width: 'min(620px, 94vw)', maxHeight: '80vh', overflowY: 'auto', padding: 24 }}><div className="card-header"><div className="card-title">Version history · {versionQuestion.title}</div><button className="btn btn-ghost btn-sm" onClick={() => setVersionQuestion(null)}>Close</button></div>{versions.length === 0 ? <div className="empty-state"><div className="empty-subtitle">No earlier versions yet. Editing this question creates one.</div></div> : <div className="question-stack">{versions.map((version) => <div className="choice-option" key={version.id} style={{ justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}><div><strong>Version {version.versionNumber}</strong><div className="text-secondary" style={{ fontSize: 13, marginTop: 5 }}>{new Date(version.createdAt).toLocaleString()}</div><div style={{ marginTop: 8 }}>{version.content}</div></div><button className="btn btn-secondary btn-sm" onClick={() => restoreVersion(version)}>Restore</button></div>)}</div>}</div></div>}
    {rubricQuestion && <div className="modal-backdrop" role="dialog" aria-modal="true"><div className="card" style={{ width: 'min(620px, 94vw)', padding: 24 }}><div className="card-header"><div className="card-title">Rubric · {rubricQuestion.title}</div><button className="btn btn-ghost btn-sm" onClick={() => setRubricQuestion(null)}>Close</button></div><div className="text-secondary" style={{ fontSize: 13, marginBottom: 12 }}>Set the scoring criteria reviewers use for this question.</div>{criteria.map((criterion, index) => <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px auto', gap: 10, marginTop: 8 }} key={index}><input className="input" placeholder="Criterion, e.g. Evidence" value={criterion.criterionName} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, criterionName: event.target.value } : item))} /><input className="input" type="number" min="1" placeholder="Points" value={criterion.maxPoints} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, maxPoints: event.target.value } : item))} /><button className="btn btn-ghost btn-sm" onClick={() => setCriteria(criteria.filter((_, itemIndex) => itemIndex !== index))}>Remove</button></div>)}<button className="btn btn-secondary btn-sm" style={{ marginTop: 12 }} onClick={() => setCriteria([...criteria, { criterionName: '', maxPoints: 1 }])}>+ Add criterion</button><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button className="btn btn-secondary" onClick={() => setRubricQuestion(null)}>Cancel</button><button className="btn btn-primary" onClick={saveRubric}>Save rubric</button></div></div></div>}
  </div>;
}

function QuestionTypeGuide({ type }) {
  const guide = TYPE_GUIDANCE[type] ?? TYPE_GUIDANCE.MCQ_SINGLE;
  return <div className="question-type-guide"><span className="question-type-icon">{guide.icon}</span><div><strong>{guide.title}</strong><p>{guide.text}</p></div></div>;
}

function RubricEditor({ criteria, setCriteria }) {
  return <section className="question-rubric-editor"><div><strong>Evaluator rubric</strong><p>Only admins and evaluators see this. Candidates do not.</p></div>{criteria.map((criterion, index) => <div className="question-rubric-row" key={index}><input className="input" placeholder="Criterion, e.g. Clarity of explanation" value={criterion.criterionName} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, criterionName: event.target.value } : item))} /><input className="input" type="number" min="1" placeholder="Points" value={criterion.maxPoints} onChange={(event) => setCriteria(criteria.map((item, itemIndex) => itemIndex === index ? { ...item, maxPoints: event.target.value } : item))} /><button className="btn btn-ghost btn-sm" type="button" onClick={() => setCriteria(criteria.filter((_, itemIndex) => itemIndex !== index))}>Remove</button></div>)}<button className="btn btn-secondary btn-sm" type="button" onClick={() => setCriteria([...criteria, { criterionName: '', maxPoints: 1 }])}>+ Add criterion</button></section>;
}
