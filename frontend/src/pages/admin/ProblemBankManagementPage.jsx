import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { categoryAPI, problemsAPI } from '../../api';

const blankCase = (sample = false) => ({ inputData: '', expectedOutput: '', explanation: '', isSample: sample });
const INITIAL = { title: '', description: '', inputFormat: '', outputFormat: '', constraints: '', difficulty: 'MEDIUM', category: '', tags: '', points: '', negativeScore: 0, timeLimit: 1000, memoryLimit: 262144, debuggingLanguage: 'java', debuggingStarterCode: '', testCases: [blankCase(true), blankCase(false)] };

export default function ProblemBankManagementPage() {
  const [form, setForm] = useState(INITIAL);
  const [searchParams] = useSearchParams();
  const editingProblemId = searchParams.get('edit');
  const [taskMode, setTaskMode] = useState(searchParams.get('mode') === 'debugging' ? 'DEBUGGING' : 'CODING');
  const [categories, setCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const navigate = useNavigate();
  const update = (change) => setForm((current) => ({ ...current, ...change }));
  const flatCategories = (items, depth = 0) => items.flatMap((item) => [{ ...item, depth }, ...flatCategories(item.children ?? [], depth + 1)]);

  useEffect(() => { categoryAPI.list().then(({ data }) => setCategories(data ?? [])).catch(() => {}); }, []);
  useEffect(() => {
    if (!editingProblemId) return;
    problemsAPI.getById(editingProblemId).then(({ data: problem }) => {
      const tags = problem.tags ?? [];
      const debugging = tags.some((tag) => String(tag).toLowerCase() === 'debugging');
      const starterCode = problem.starterCode ?? {};
      const debuggingLanguage = starterCode.java ? 'java' : (Object.keys(starterCode)[0] ?? 'java');
      setTaskMode(debugging ? 'DEBUGGING' : 'CODING');
      setForm({ ...INITIAL, title: problem.title ?? '', description: problem.description ?? '', inputFormat: problem.inputFormat ?? '', outputFormat: problem.outputFormat ?? '', constraints: problem.constraints ?? '', difficulty: problem.difficulty ?? 'MEDIUM', tags: tags.filter((tag) => String(tag).toLowerCase() !== 'debugging').join(', '), timeLimit: problem.timeLimit ?? 1000, memoryLimit: problem.memoryLimit ?? 262144, debuggingLanguage, debuggingStarterCode: starterCode[debuggingLanguage] ?? '', testCases: (problem.sampleTestCases ?? []).map((testCase) => ({ inputData: testCase.inputData ?? '', expectedOutput: testCase.expectedOutput ?? '', explanation: testCase.explanation ?? '', isSample: Boolean(testCase.isSample) })) });
    }).catch(() => toast.error('Unable to load this task.'));
  }, [editingProblemId]);

  const updateCase = (index, change) => update({ testCases: form.testCases.map((testCase, testIndex) => testIndex === index ? { ...testCase, ...change } : testCase) });
  const exportTasks = async () => { try { const { data } = await problemsAPI.exportTaskCenterCsv(); const url = URL.createObjectURL(data); const link = document.createElement('a'); link.href = url; link.download = 'task-center.csv'; link.click(); URL.revokeObjectURL(url); } catch { toast.error('Unable to export Task Center tasks.'); } };
  const importTasks = async (event) => { const file = event.target.files?.[0]; if (!file) return; setImporting(true); try { const { data } = await problemsAPI.importTaskCenterCsv(file); toast.success(`${data.imported ?? 0} task(s) imported into Question Bank.`); } catch (error) { toast.error(error.response?.data?.message || 'Unable to import the Task Center CSV.'); } finally { setImporting(false); event.target.value = ''; } };

  const save = async (event) => {
    event.preventDefault();
    const cases = form.testCases.filter((testCase) => testCase.inputData.trim() && testCase.expectedOutput.trim());
    if (!cases.length) return toast.error('Add at least one complete test case.');
    if (taskMode === 'DEBUGGING' && !form.debuggingStarterCode.trim()) return toast.error('Add the broken starter code candidates need to repair.');
    setSaving(true);
    try {
      const tags = form.tags.split(',').map((tag) => tag.trim()).filter((tag) => tag && tag.toLowerCase() !== 'debugging');
      if (taskMode === 'DEBUGGING') tags.push('debugging');
      const { debuggingLanguage, debuggingStarterCode, ...task } = form;
      const payload = { ...task, points: form.points === '' ? null : Number(form.points), negativeScore: Number(form.negativeScore), timeLimit: Number(form.timeLimit), memoryLimit: Number(form.memoryLimit), tags, starterCode: taskMode === 'DEBUGGING' ? { [debuggingLanguage]: debuggingStarterCode } : undefined, testCases: cases.map((testCase, orderIndex) => ({ ...testCase, orderIndex })) };
      if (editingProblemId) await problemsAPI.update(editingProblemId, payload); else await problemsAPI.create(payload);
      toast.success(editingProblemId ? 'Task updated.' : `${taskMode === 'DEBUGGING' ? 'Debugging' : 'Coding'} task created.`);
      navigate('/admin/questions');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to save this task.'); } finally { setSaving(false); }
  };

  return <div className="container fade-in">
    <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}><div><h1 className="page-title">Task Center</h1><p className="page-subtitle">{editingProblemId ? 'Edit executable task details and judge tests.' : 'Create executable coding tasks with visible examples and hidden judge tests.'}</p></div><div style={{ display: 'flex', gap: 8 }}><button className="btn btn-secondary" type="button" onClick={exportTasks}>Export CSV</button><label className="btn btn-secondary" style={{ cursor: 'pointer' }}>{importing ? 'Importing...' : 'Import CSV'}<input type="file" accept=".csv" onChange={importTasks} disabled={importing} style={{ display: 'none' }} /></label><Link className="btn btn-secondary" to="/admin/questions">Back to Question Bank</Link></div></div>
    <form className="card problem-authoring-form" onSubmit={save}>
      <section className="problem-authoring-note"><strong>{editingProblemId ? 'Edit this executable task.' : 'Create the executable task first.'}</strong><p>{editingProblemId ? 'Changes here update the task candidates run and its judge tests.' : 'After saving, a linked question-bank entry is created.'}</p></section>
      <label className="label">Task type<select className="select" value={taskMode} onChange={(event) => setTaskMode(event.target.value)}><option value="CODING">Algorithmic coding</option><option value="DEBUGGING">Debugging task</option></select><small className="text-secondary">Debugging tasks appear in the Debugging Bank.</small></label>
      <div className="form-group" style={{ marginTop: 14 }}><label className="label">Problem title</label><input className="input" required value={form.title} onChange={(event) => update({ title: event.target.value })} placeholder={taskMode === 'DEBUGGING' ? 'e.g. Debug: Off-by-One Loop' : 'e.g. Pair Sum Indices'} /></div>
      <div className="form-group" style={{ marginTop: 14 }}><label className="label">Problem statement</label><textarea className="textarea" required value={form.description} onChange={(event) => update({ description: event.target.value })} placeholder={taskMode === 'DEBUGGING' ? 'Describe the faulty behavior and what the repaired program must do.' : 'Explain what the candidate must solve.'} /></div>
      {taskMode === 'DEBUGGING' && <section className="card" style={{ marginTop: 14, padding: 16, border: '1px solid var(--accent)' }}><div style={{ fontWeight: 700 }}>Broken starter code</div><p className="text-secondary" style={{ margin: '5px 0 12px' }}>Paste the program candidates must repair. Reset restores this exact code during the assessment.</p><label className="label">Language<select className="select" value={form.debuggingLanguage} onChange={(event) => update({ debuggingLanguage: event.target.value })}><option value="java">Java</option><option value="python">Python</option><option value="cpp">C++</option><option value="javascript">JavaScript</option></select></label><label className="label" style={{ display: 'block', marginTop: 10 }}>Code<textarea className="textarea" required value={form.debuggingStarterCode} onChange={(event) => update({ debuggingStarterCode: event.target.value })} style={{ minHeight: 260, fontFamily: 'monospace' }} placeholder="Paste the intentionally broken code here..." /></label></section>}
      <div className="problem-authoring-grid"><label className="label">Input format<textarea className="textarea" value={form.inputFormat} onChange={(event) => update({ inputFormat: event.target.value })} /></label><label className="label">Output format<textarea className="textarea" value={form.outputFormat} onChange={(event) => update({ outputFormat: event.target.value })} /></label></div>
      <label className="label" style={{ display: 'block', marginTop: 14 }}>Constraints<textarea className="textarea" value={form.constraints} onChange={(event) => update({ constraints: event.target.value })} placeholder="Input limits, valid ranges, and edge cases." /></label>
      <div className="problem-authoring-grid"><label className="label">Difficulty<select className="select" value={form.difficulty} onChange={(event) => update({ difficulty: event.target.value })}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label><label className="label">Category{categories.length ? <select className="select" value={form.category} onChange={(event) => update({ category: event.target.value })}><option value="">Select a category</option>{flatCategories(categories).map((category) => <option key={category.id} value={category.name}>{'— '.repeat(category.depth)}{category.name}</option>)}</select> : <input className="input" value={form.category} onChange={(event) => update({ category: event.target.value })} placeholder="Optional category" />}</label><label className="label">Tags<input className="input" value={form.tags} onChange={(event) => update({ tags: event.target.value })} placeholder="arrays, hash maps" /></label></div>
      <div className="problem-authoring-grid"><label className="label">Points<input className="input" type="number" min="0" value={form.points} onChange={(event) => update({ points: event.target.value })} placeholder="Auto by difficulty" /></label><label className="label">Negative<input className="input" type="number" min="0" value={form.negativeScore} onChange={(event) => update({ negativeScore: event.target.value })} /></label><label className="label">Time limit (ms)<input className="input" type="number" min="100" value={form.timeLimit} onChange={(event) => update({ timeLimit: event.target.value })} /></label></div>
      <div className="problem-test-header"><div><strong>Judge test cases</strong><p>Mark examples as visible; keep evaluation tests hidden.</p></div><button type="button" className="btn btn-secondary btn-sm" onClick={() => update({ testCases: [...form.testCases, blankCase(false)] })}>+ Add test</button></div>
      {form.testCases.map((testCase, index) => <div className="card problem-test-card" key={index}><div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}><strong>Test case {index + 1}</strong><div><label className="text-secondary" style={{ fontSize: 12 }}><input type="checkbox" checked={testCase.isSample} onChange={(event) => updateCase(index, { isSample: event.target.checked })} /> Visible example</label>{form.testCases.length > 1 && <button type="button" className="btn btn-ghost btn-sm" onClick={() => update({ testCases: form.testCases.filter((_, testIndex) => testIndex !== index) })}>Remove</button>}</div></div><div className="problem-authoring-grid"><label className="label">Input<textarea className="textarea" required value={testCase.inputData} onChange={(event) => updateCase(index, { inputData: event.target.value })} /></label><label className="label">Expected output<textarea className="textarea" required value={testCase.expectedOutput} onChange={(event) => updateCase(index, { expectedOutput: event.target.value })} /></label></div>{testCase.isSample && <label className="label" style={{ display: 'block', marginTop: 10 }}>Explanation (optional)<input className="input" value={testCase.explanation} onChange={(event) => updateCase(index, { explanation: event.target.value })} /></label>}</div>)}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 22 }}><Link className="btn btn-secondary" to="/admin/questions">Cancel</Link><button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : editingProblemId ? 'Save task changes' : `Create ${taskMode === 'DEBUGGING' ? 'debugging' : 'coding'} problem`}</button></div>
    </form>
  </div>;
}
