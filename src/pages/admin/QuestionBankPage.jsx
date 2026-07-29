import { useEffect, useMemo, useState } from 'react';
import { Edit, Filter, Plus, Search, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import Modal from '../../components/ui/Modal';
import { questionBankAPI } from '../../api';

const EMPTY_FORM = {
  title: '', content: '', category: '', questionType: 'MCQ_SINGLE',
  difficulty: 'MEDIUM', tags: '', correctAnswers: '', points: 1, negativeScore: 0,
};

export default function QuestionBankPage() {
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState('ALL');
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadQuestions = async () => {
    setLoading(true);
    try {
      const { data } = await questionBankAPI.list();
      setQuestions(Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []);
    } catch {
      toast.error('Unable to load the question bank.');
      setQuestions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadQuestions(); }, []);

  const visibleQuestions = useMemo(() => questions.filter((question) => {
    const query = search.trim().toLowerCase();
    const matchesSearch = !query || question.title?.toLowerCase().includes(query) || question.content?.toLowerCase().includes(query);
    return matchesSearch && (difficulty === 'ALL' || question.difficulty === difficulty);
  }), [questions, search, difficulty]);

  const openCreate = () => { setEditing(null); setForm(EMPTY_FORM); setIsEditorOpen(true); };
  const openEdit = (question) => {
    setEditing(question);
    setIsEditorOpen(true);
    setForm({
      title: question.title ?? '', content: question.content ?? '', category: question.category ?? '',
      questionType: question.questionType ?? question.type ?? 'MCQ_SINGLE', difficulty: question.difficulty ?? 'MEDIUM',
      tags: (question.tags ?? []).join(', '), correctAnswers: question.correctAnswers ?? '',
      points: question.points ?? 1, negativeScore: question.negativeScore ?? 0,
    });
  };
  const closeEditor = () => { setEditing(null); setForm(EMPTY_FORM); setIsEditorOpen(false); };

  const saveQuestion = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = {
      ...form, type: form.questionType,
      tags: form.tags.split(',').map((tag) => tag.trim()).filter(Boolean),
      points: Number(form.points), negativeScore: Number(form.negativeScore),
    };
    try {
      const { data } = editing
        ? await questionBankAPI.update(editing.id, payload)
        : await questionBankAPI.create(payload);
      setQuestions((current) => editing
        ? current.map((question) => question.id === data.id ? data : question)
        : [data, ...current]);
      toast.success(editing ? 'Question updated.' : 'Question added to the bank.');
      closeEditor();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Unable to save the question.');
    } finally { setSaving(false); }
  };

  const deleteQuestion = async (question) => {
    if (!window.confirm(`Delete “${question.title}”? This cannot be undone.`)) return;
    try {
      await questionBankAPI.delete(question.id);
      setQuestions((current) => current.filter((item) => item.id !== question.id));
      toast.success('Question deleted.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to delete the question.'); }
  };

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div><h1 style={{ fontSize: 24, fontWeight: 800 }}>Question Bank</h1><p className="text-secondary">Create, edit, and organize assessment questions.</p></div>
        <button className="btn btn-primary" onClick={openCreate}><Plus size={16} /> Create Question</button>
      </div>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', display: 'flex', gap: 12 }}>
          <div className="input-group" style={{ maxWidth: 340 }}><Search size={16} /><input className="input" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search questions..." /></div>
          <select className="select" value={difficulty} onChange={(event) => setDifficulty(event.target.value)}><option value="ALL">All difficulties</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select>
          <Filter size={18} style={{ alignSelf: 'center', color: 'var(--text-muted)' }} />
        </div>
        <div className="table-container"><table><thead><tr><th>Title</th><th>Type</th><th>Difficulty</th><th>Category</th><th style={{ textAlign: 'right' }}>Actions</th></tr></thead><tbody>
          {loading ? <tr><td colSpan="5" style={{ textAlign: 'center', padding: 32 }}>Loading questions…</td></tr>
            : visibleQuestions.length === 0 ? <tr><td colSpan="5" style={{ textAlign: 'center', padding: 32 }}>No questions found.</td></tr>
            : visibleQuestions.map((question) => <tr key={question.id}><td style={{ fontWeight: 600 }}>{question.title}</td><td><span className="badge badge-default">{question.questionType || question.type || 'Question'}</span></td><td><span className="badge badge-tag">{question.difficulty || 'Unspecified'}</span></td><td>{question.category || '—'}</td><td style={{ textAlign: 'right' }}><button className="btn btn-ghost btn-sm" onClick={() => openEdit(question)} aria-label={`Edit ${question.title}`}><Edit size={16} /></button><button className="btn btn-ghost btn-sm" style={{ color: 'var(--red)' }} onClick={() => deleteQuestion(question)} aria-label={`Delete ${question.title}`}><Trash2 size={16} /></button></td></tr>)}
        </tbody></table></div>
      </div>
      <Modal isOpen={isEditorOpen} onClose={closeEditor} title={editing ? 'Edit Question' : 'Create Question'} footer={<><button className="btn btn-secondary" onClick={closeEditor} disabled={saving}>Cancel</button><button className="btn btn-primary" form="question-editor" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Question'}</button></>}>
        <form id="question-editor" onSubmit={saveQuestion} style={{ display: 'grid', gap: 14 }}>
          <input className="input" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Question title" required />
          <textarea className="textarea" value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="Question prompt, options, or instructions" required />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}><select className="select" value={form.questionType} onChange={(event) => setForm({ ...form, questionType: event.target.value })}><option value="MCQ_SINGLE">MCQ (single)</option><option value="MCQ_MULTI">MCQ (multi)</option><option value="WRITTEN">Written</option><option value="CODING">Coding</option><option value="READING_COMPREHENSION">Reading comprehension</option><option value="FILE_UPLOAD">File upload</option></select><select className="select" value={form.difficulty} onChange={(event) => setForm({ ...form, difficulty: event.target.value })}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></div>
          <input className="input" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} placeholder="Category" /><input className="input" value={form.correctAnswers} onChange={(event) => setForm({ ...form, correctAnswers: event.target.value })} placeholder="Correct answer(s), if applicable" /><input className="input" value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} placeholder="Tags, comma separated" />
        </form>
      </Modal>
    </div>
  );
}
