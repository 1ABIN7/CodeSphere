import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { assessmentAPI, questionBankAPI, usersAPI } from '../../api';

const INITIAL = { title: '', description: '', assessmentType: 'MCQ', durationMinutes: 60, passingScore: 70, instructions: '', startTime: '', endTime: '', shuffleQuestions: false, shuffleOptions: false };
const newSection = (number) => ({ id: `${Date.now()}-${Math.random()}`, title: `Section ${number}`, questionIds: [] });

export default function AssessmentManagementPage() {
  const [assessments, setAssessments] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [form, setForm] = useState(INITIAL);
  const [sections, setSections] = useState([newSection(1)]);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [assignment, setAssignment] = useState(null);
  const [candidateId, setCandidateId] = useState('');
  const [deadline, setDeadline] = useState('');
  const [tracking, setTracking] = useState(null);
  const [assignmentStatus, setAssignmentStatus] = useState([]);

  const load = async () => {
    try {
      const [assessmentRes, questionRes, candidateRes] = await Promise.all([assessmentAPI.list(), questionBankAPI.list(), usersAPI.listCandidates()]);
      setAssessments(assessmentRes.data ?? []);
      setQuestions(questionRes.data?.content ?? questionRes.data ?? []);
      setCandidates(candidateRes.data ?? []);
    } catch { toast.error('Unable to load assessment administration.'); }
  };
  useEffect(() => { load(); }, []);

  const create = async (event) => {
    event.preventDefault();
    const populatedSections = sections.filter((section) => section.questionIds.length > 0);
    if (!populatedSections.length) return toast.error('Add at least one question to a section.');
    setSaving(true);
    try {
      const { data: assessment } = await assessmentAPI.create({ ...form, durationMinutes: Number(form.durationMinutes), passingScore: Number(form.passingScore), startTime: form.startTime || null, endTime: form.endTime || null, allowResume: true });
      await Promise.all(populatedSections.map(async (sectionDraft, sectionOrder) => {
        const { data: section } = await assessmentAPI.addSection(assessment.id, { title: sectionDraft.title.trim() || `Section ${sectionOrder + 1}`, sectionOrder, durationMinutes: Number(form.durationMinutes), sectionType: form.assessmentType, navigationMode: 'FREE' });
        await Promise.all(sectionDraft.questionIds.map((questionId, orderIndex) => {
          const question = questions.find((item) => item.id === questionId);
          return assessmentAPI.addQuestionToSection(assessment.id, section.id, { questionBankId: questionId, orderIndex, maxScore: question?.points ?? 1, negativeScore: question?.negativeScore ?? 0 });
        }));
      }));
      setAssessments((items) => [assessment, ...items]);
      setOpen(false); setForm(INITIAL); setSections([newSection(1)]);
      toast.success('Assessment created as a draft. Publish it when ready.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to create assessment.'); }
    finally { setSaving(false); }
  };

  const publish = async (assessment) => {
    try {
      const { data } = await assessmentAPI.publish(assessment.id);
      setAssessments((items) => items.map((item) => item.id === data.id ? data : item));
      toast.success('Assessment published.');
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to publish assessment.'); }
  };

  const assign = async (event) => {
    event.preventDefault();
    if (!candidateId || !assignment) return;
    try { await assessmentAPI.assign(assignment.id, Number(candidateId), deadline || undefined); toast.success('Candidate assigned.'); setAssignment(null); setCandidateId(''); setDeadline(''); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to assign candidate.'); }
  };

  const updateSection = (id, update) => setSections((items) => items.map((section) => section.id === id ? { ...section, ...update } : section));
  const toggleQuestion = (sectionId, questionId) => setSections((items) => items.map((section) => {
    if (section.id !== sectionId) return section;
    const questionIds = section.questionIds.includes(questionId) ? section.questionIds.filter((id) => id !== questionId) : [...section.questionIds, questionId];
    return { ...section, questionIds };
  }));
  const viewTracking = async (assessment) => { try { const { data } = await assessmentAPI.getAssignmentStatus(assessment.id); setAssignmentStatus(data ?? []); setTracking(assessment); } catch { toast.error('Unable to load assignment tracking.'); } };

  return <div className="container fade-in">
    <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}><div><h1 className="page-title">Assessments</h1><p className="page-subtitle">Build, publish, and assign assessments from your question bank.</p></div><button className="btn btn-primary" onClick={() => setOpen(true)}>+ Create assessment</button></div>
    <div className="card">{assessments.length === 0 ? <div className="empty-state"><div className="empty-title">No assessments yet</div><div className="empty-subtitle">Create one from your question bank.</div></div> : <div className="question-stack">{assessments.map((assessment) => <div className="choice-option" key={assessment.id} style={{ justifyContent: 'space-between' }}><div><strong>{assessment.title}</strong><div className="text-secondary" style={{ fontSize: 13, marginTop: 4 }}>{assessment.assessmentType} · {assessment.durationMinutes} minutes · {assessment.isPublished ? 'Published' : 'Draft'}</div></div><div style={{ display: 'flex', gap: 8 }}><button className="btn btn-ghost btn-sm" onClick={() => viewTracking(assessment)}>Tracking</button>{!assessment.isPublished && <button className="btn btn-primary btn-sm" onClick={() => publish(assessment)}>Publish</button>}<button className="btn btn-secondary btn-sm" onClick={() => setAssignment(assessment)}>Assign</button></div></div>)}</div>}</div>
    {tracking && <div className="modal-backdrop"><div className="card" style={{ width: 'min(680px, 94vw)', padding: 24 }}><div className="card-header"><div className="card-title">Assignment tracking · {tracking.title}</div><button className="btn btn-ghost btn-sm" onClick={() => setTracking(null)}>Close</button></div>{assignmentStatus.length === 0 ? <div className="empty-state"><div className="empty-subtitle">No candidates assigned.</div></div> : <div className="table-container"><table><thead><tr><th>Candidate</th><th>Deadline</th><th>Status</th></tr></thead><tbody>{assignmentStatus.map((item) => <tr key={item.userId}><td>{item.fullName || item.username}</td><td>{item.deadline ? new Date(item.deadline).toLocaleString() : 'No deadline'}</td><td><span className="badge badge-default">{item.sessionStatus}</span></td></tr>)}</tbody></table></div>}</div></div>}
    {assignment && <div className="modal-backdrop" role="dialog" aria-modal="true"><form className="card" onSubmit={assign} style={{ width: 'min(460px, 94vw)', padding: 24 }}><div className="card-header"><div className="card-title">Assign {assignment.title}</div><button className="btn btn-ghost btn-sm" type="button" onClick={() => setAssignment(null)}>Close</button></div><label className="label">Candidate<select required className="select" value={candidateId} onChange={(e) => setCandidateId(e.target.value)}><option value="">Select a candidate</option>{candidates.map((candidate) => <option value={candidate.id} key={candidate.id}>{candidate.fullName || candidate.username} ({candidate.email})</option>)}</select></label><label className="label" style={{ display: 'block', marginTop: 14 }}>Deadline (optional)<input className="input" type="datetime-local" value={deadline} onChange={(e) => setDeadline(e.target.value)} /></label><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button className="btn btn-secondary" type="button" onClick={() => setAssignment(null)}>Cancel</button><button className="btn btn-primary" disabled={!candidates.length}>Assign candidate</button></div>{!candidates.length && <p className="text-secondary" style={{ marginTop: 12 }}>No candidate accounts are available yet.</p>}</form></div>}
    {open && <div className="modal-backdrop" role="dialog" aria-modal="true"><form className="card" onSubmit={create} style={{ width: 'min(820px, 94vw)', maxHeight: '90vh', overflowY: 'auto', padding: 24 }}><div className="card-header"><div className="card-title">Create assessment</div><button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpen(false)}>Close</button></div><div style={{ display: 'grid', gridTemplateColumns: '1fr 180px 130px', gap: 12 }}><label className="label">Title<input className="input" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label className="label">Type<select className="select" value={form.assessmentType} onChange={(e) => setForm({ ...form, assessmentType: e.target.value })}><option value="MCQ">MCQ</option><option value="CODING">Coding</option><option value="WRITTEN">Written</option><option value="MIXED">Mixed</option></select></label><label className="label">Minutes<input className="input" required type="number" min="1" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} /></label></div><div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 14 }}><label className="label">Opens at<input className="input" type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></label><label className="label">Closes at<input className="input" type="datetime-local" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></label></div><label className="label" style={{ display: 'block', marginTop: 14 }}>Description<textarea className="textarea" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><label className="label" style={{ display: 'block', marginTop: 14 }}>Instructions<textarea className="textarea" value={form.instructions} onChange={(e) => setForm({ ...form, instructions: e.target.value })} /></label><label className="label" style={{ display: 'block', marginTop: 14, maxWidth: 160 }}>Passing score<input className="input" type="number" min="0" max="100" value={form.passingScore} onChange={(e) => setForm({ ...form, passingScore: e.target.value })} /></label><div style={{ display: 'flex', gap: 18, marginTop: 14 }}><label><input type="checkbox" checked={form.shuffleQuestions} onChange={(e) => setForm({ ...form, shuffleQuestions: e.target.checked })} /> Shuffle questions</label><label><input type="checkbox" checked={form.shuffleOptions} onChange={(e) => setForm({ ...form, shuffleOptions: e.target.checked })} /> Shuffle options</label></div><div style={{ marginTop: 18 }}><div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><strong>Sections</strong><button type="button" className="btn btn-secondary btn-sm" onClick={() => setSections((items) => [...items, newSection(items.length + 1)])}>+ Add section</button></div>{sections.map((section, index) => <div className="card" key={section.id} style={{ marginTop: 12, padding: 14 }}><div style={{ display: 'flex', gap: 10, alignItems: 'end' }}><label className="label" style={{ flex: 1 }}>Section {index + 1} name<input className="input" value={section.title} onChange={(e) => updateSection(section.id, { title: e.target.value })} /></label>{sections.length > 1 && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setSections((items) => items.filter((item) => item.id !== section.id))}>Remove</button>}</div><div className="text-secondary" style={{ fontSize: 13, marginTop: 8 }}>Select questions for this section ({section.questionIds.length})</div><div className="question-stack" style={{ marginTop: 10, maxHeight: 180, overflowY: 'auto' }}>{questions.map((question) => <label className="choice-option" key={question.id} style={{ display: 'flex', gap: 10, cursor: 'pointer' }}><input type="checkbox" checked={section.questionIds.includes(question.id)} onChange={() => toggleQuestion(section.id, question.id)} /><span><strong>{question.title}</strong><small className="text-secondary" style={{ display: 'block' }}>{question.difficulty} · {question.points ?? 1} points</small></span></label>)}</div></div>)}</div><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}><button type="button" className="btn btn-secondary" onClick={() => setOpen(false)}>Cancel</button><button className="btn btn-primary" disabled={saving}>{saving ? 'Creating...' : 'Create draft'}</button></div></form></div>}
  </div>;
}
