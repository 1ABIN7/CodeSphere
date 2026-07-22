import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Save,
  Eye,
  Plus,
  Trash2,
  ChevronRight,
  ChevronLeft,
  GripVertical,
} from 'lucide-react';
import Modal from '../../components/ui/Modal';
import { assessmentsAPI } from '../../api';

const ASSESSMENT_TYPES = ['CODING', 'MCQ', 'WRITTEN', 'READING', 'FILE_UPLOAD', 'MASTERY'];
const NAV_MODES = ['SEQUENTIAL', 'FREE'];

function emptySection() {
  return {
    _key: Date.now() + Math.random(),
    title: '',
    timeLimitMinutes: '',
    navigationMode: 'FREE',
    questions: [],
  };
}

const INITIAL = {
  title: '',
  description: '',
  type: 'CODING',
  durationMinutes: 60,
  sections: [emptySection()],
  settings: {
    shuffle_questions: false,
    shuffle_options: false,
    allow_resume: true,
    is_certifying: false,
    is_published: false,
    access_code: '',
    passing_score: '',
  },
};

export default function AssessmentWizardPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(id);
  const [step, setStep] = useState(0);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(INITIAL);
  const [bankQuestions, setBankQuestions] = useState([]);
  const [questionPickerOpen, setQuestionPickerOpen] = useState(null);

  useEffect(() => {
    if (isEditing) {
      assessmentsAPI.getById(id).then((res) => {
        const d = res.data;
        setForm({
          title: d.title ?? '',
          description: d.description ?? '',
          type: d.type ?? 'CODING',
          durationMinutes: d.durationMinutes ?? 60,
          sections: (d.sections ?? []).map((s) => ({ ...s, _key: s.id ?? Math.random() })),
          settings: {
            shuffle_questions: d.shuffleQuestions ?? false,
            shuffle_options: d.shuffleOptions ?? false,
            allow_resume: d.allowResume ?? true,
            is_certifying: d.isCertifying ?? false,
            is_published: d.published ?? false,
            access_code: d.accessCode ?? '',
            passing_score: d.passingScore ?? '',
          },
        });
      }).catch(() => toast.error('Failed to load assessment'));
    }
  }, [id, isEditing]);

  useEffect(() => {
    assessmentsAPI.getAllAssessments?.()
      .then(() => {})
      .catch(() => {});
    // Attempt to load question bank
    import('../../api').then(({ problemsAPI }) => {
      problemsAPI.list({ page: 0, size: 200 }).then((res) => {
        setBankQuestions(res.data?.content ?? res.data ?? []);
      }).catch(() => {});
    });
  }, []);

  const setField = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  const setSetting = (key, value) =>
    setForm((f) => ({ ...f, settings: { ...f.settings, [key]: value } }));

  const updateSection = (idx, key, value) => {
    setForm((f) => {
      const sections = [...f.sections];
      sections[idx] = { ...sections[idx], [key]: value };
      return { ...f, sections };
    });
  };

  const addSection = () =>
    setForm((f) => ({ ...f, sections: [...f.sections, emptySection()] }));

  const removeSection = (idx) =>
    setForm((f) => ({ ...f, sections: f.sections.filter((_, i) => i !== idx) }));

  const addQuestionToSection = (sectionIdx, question) => {
    setForm((f) => {
      const sections = [...f.sections];
      const section = { ...sections[sectionIdx] };
      section.questions = [...(section.questions ?? []), question];
      sections[sectionIdx] = section;
      return { ...f, sections };
    });
  };

  const removeQuestionFromSection = (sectionIdx, qIdx) => {
    setForm((f) => {
      const sections = [...f.sections];
      const section = { ...sections[sectionIdx] };
      section.questions = section.questions.filter((_, i) => i !== qIdx);
      sections[sectionIdx] = section;
      return { ...f, sections };
    });
  };

  const moveSection = (idx, dir) => {
    setForm((f) => {
      const sections = [...f.sections];
      const target = idx + dir;
      if (target < 0 || target >= sections.length) return f;
      [sections[idx], sections[target]] = [sections[target], sections[idx]];
      return { ...f, sections };
    });
  };

  async function handleSave(publish) {
    setSaving(true);
    const payload = {
      title: form.title,
      description: form.description,
      type: form.type,
      durationMinutes: Number(form.durationMinutes) || 60,
      published: publish,
      sections: form.sections.map((s, i) => ({
        title: s.title,
        sortOrder: i,
        timeLimitMinutes: s.timeLimitMinutes ? Number(s.timeLimitMinutes) : null,
        navigationMode: s.navigationMode,
        questionIds: (s.questions ?? []).map((q) => q.id),
      })),
      shuffleQuestions: form.settings.shuffle_questions,
      shuffleOptions: form.settings.shuffle_options,
      allowResume: form.settings.allow_resume,
      isCertifying: form.settings.is_certifying,
      accessCode: form.settings.access_code || null,
      passingScore: form.settings.passing_score ? Number(form.settings.passing_score) : null,
    };
    try {
      if (isEditing) {
        await assessmentsAPI.update(id, payload);
        // Note: may need to add update to api.js
      } else {
        await assessmentsAPI.create(payload);
        // Note: may need to add create to api.js
      }
      toast.success(publish ? 'Assessment published' : 'Assessment saved as draft');
      navigate('/admin/assessments');
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  const STEPS = ['Basic Info', 'Sections', 'Questions', 'Settings', 'Review'];

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: 24 }}>
            {isEditing ? 'Edit Assessment' : 'Create Assessment'}
          </h1>
        </div>
      </div>

      {/* Step indicator */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 32 }}>
        {STEPS.map((label, i) => (
          <div
            key={label}
            style={{
              flex: 1,
              textAlign: 'center',
              padding: '10px 0',
              fontSize: 13,
              fontWeight: 600,
              borderRadius: 'var(--radius-sm)',
              background: i === step ? 'var(--accent)' : i < step ? 'rgba(124,58,237,0.15)' : 'var(--bg-card)',
              color: i === step ? '#fff' : i < step ? 'var(--accent-light)' : 'var(--text-secondary)',
              border: `1px solid ${i === step ? 'var(--accent)' : 'var(--border)'}`,
              transition: 'all 0.2s',
            }}
          >
            {i + 1}. {label}
          </div>
        ))}
      </div>

      {/* Step content */}
      <div className="card" style={{ marginBottom: 24 }}>
        {step === 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <div className="form-group">
              <label className="label">Title</label>
              <input
                className="input"
                value={form.title}
                onChange={(e) => setField('title', e.target.value)}
                placeholder="e.g. Frontend Developer Hiring 2026"
              />
            </div>
            <div className="form-group">
              <label className="label">Description</label>
              <textarea
                className="textarea"
                value={form.description}
                onChange={(e) => setField('description', e.target.value)}
                placeholder="Describe the assessment..."
              />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="form-group">
                <label className="label">Type</label>
                <select
                  className="select"
                  value={form.type}
                  onChange={(e) => setField('type', e.target.value)}
                >
                  {ASSESSMENT_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="label">Duration (minutes)</label>
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={form.durationMinutes}
                  onChange={(e) => setField('durationMinutes', e.target.value)}
                />
              </div>
            </div>
          </div>
        )}

        {step === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {form.sections.map((section, idx) => (
              <div
                key={section._key}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  padding: 16,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <GripVertical size={16} style={{ color: 'var(--text-muted)', cursor: 'grab' }} />
                    <span style={{ fontWeight: 600, fontSize: 14 }}>Section {idx + 1}</span>
                  </div>
                  <div style={{ display: 'flex', gap: 4 }}>
                    <button
                      className="btn-ghost btn-icon btn-sm"
                      disabled={idx === 0}
                      onClick={() => moveSection(idx, -1)}
                    >
                      <ChevronLeft size={14} />
                    </button>
                    <button
                      className="btn-ghost btn-icon btn-sm"
                      disabled={idx === form.sections.length - 1}
                      onClick={() => moveSection(idx, 1)}
                    >
                      <ChevronRight size={14} />
                    </button>
                    {form.sections.length > 1 && (
                      <button
                        className="btn-ghost btn-icon btn-sm"
                        style={{ color: 'var(--red)' }}
                        onClick={() => removeSection(idx)}
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                  <div className="form-group">
                    <label className="label">Title</label>
                    <input
                      className="input"
                      value={section.title}
                      onChange={(e) => updateSection(idx, 'title', e.target.value)}
                      placeholder="Section title"
                    />
                  </div>
                  <div className="form-group">
                    <label className="label">Time Limit (min)</label>
                    <input
                      className="input"
                      type="number"
                      value={section.timeLimitMinutes}
                      onChange={(e) => updateSection(idx, 'timeLimitMinutes', e.target.value)}
                      placeholder="Optional"
                    />
                  </div>
                  <div className="form-group">
                    <label className="label">Navigation</label>
                    <select
                      className="select"
                      value={section.navigationMode}
                      onChange={(e) => updateSection(idx, 'navigationMode', e.target.value)}
                    >
                      {NAV_MODES.map((m) => (
                        <option key={m} value={m}>{m.charAt(0) + m.slice(1).toLowerCase()}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            ))}
            <button className="btn btn-secondary btn-sm" onClick={addSection}>
              <Plus size={14} /> Add Section
            </button>
          </div>
        )}

        {step === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {form.sections.map((section, sIdx) => (
              <div
                key={section._key}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  padding: 16,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <span style={{ fontWeight: 600, fontSize: 14 }}>
                    {section.title || `Section ${sIdx + 1}`}
                    <span style={{ color: 'var(--text-muted)', fontWeight: 400, marginLeft: 8 }}>
                      ({section.questions?.length ?? 0} questions)
                    </span>
                  </span>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => setQuestionPickerOpen(sIdx)}
                  >
                    <Plus size={14} /> Add Question
                  </button>
                </div>
                {(section.questions ?? []).length === 0 ? (
                  <div style={{ padding: 16, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                    No questions added yet
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {section.questions.map((q, qIdx) => (
                      <div
                        key={q.id ?? qIdx}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '8px 12px',
                          background: 'var(--bg-secondary)',
                          borderRadius: 'var(--radius-sm)',
                          fontSize: 13,
                        }}
                      >
                        <span>{q.title ?? q.name ?? `Question ${qIdx + 1}`}</span>
                        <button
                          className="btn-ghost btn-icon btn-sm"
                          style={{ color: 'var(--red)' }}
                          onClick={() => removeQuestionFromSection(sIdx, qIdx)}
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {step === 3 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {[
              { key: 'shuffle_questions', label: 'Shuffle Questions' },
              { key: 'shuffle_options', label: 'Shuffle Options' },
              { key: 'allow_resume', label: 'Allow Resume' },
              { key: 'is_certifying', label: 'Certifying Assessment' },
            ].map(({ key, label }) => (
              <label
                key={key}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  cursor: 'pointer',
                  fontSize: 14,
                }}
              >
                <input
                  type="checkbox"
                  checked={form.settings[key]}
                  onChange={(e) => setSetting(key, e.target.checked)}
                  style={{ width: 18, height: 18, accentColor: 'var(--accent)' }}
                />
                {label}
              </label>
            ))}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="form-group">
                <label className="label">Access Code (optional)</label>
                <input
                  className="input"
                  value={form.settings.access_code}
                  onChange={(e) => setSetting('access_code', e.target.value)}
                  placeholder="Leave empty for no code"
                />
              </div>
              <div className="form-group">
                <label className="label">Passing Score (%)</label>
                <input
                  className="input"
                  type="number"
                  min={0}
                  max={100}
                  value={form.settings.passing_score}
                  onChange={(e) => setSetting('passing_score', e.target.value)}
                  placeholder="e.g. 70"
                />
              </div>
            </div>
          </div>
        )}

        {step === 4 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div>
                <div className="label" style={{ marginBottom: 4 }}>Title</div>
                <div style={{ fontSize: 14 }}>{form.title || '—'}</div>
              </div>
              <div>
                <div className="label" style={{ marginBottom: 4 }}>Type</div>
                <span className="badge badge-default">{form.type}</span>
              </div>
              <div>
                <div className="label" style={{ marginBottom: 4 }}>Duration</div>
                <div style={{ fontSize: 14 }}>{form.durationMinutes} min</div>
              </div>
              <div>
                <div className="label" style={{ marginBottom: 4 }}>Sections</div>
                <div style={{ fontSize: 14 }}>{form.sections.length}</div>
              </div>
            </div>
            {form.description && (
              <div>
                <div className="label" style={{ marginBottom: 4 }}>Description</div>
                <div style={{ fontSize: 14, color: 'var(--text-secondary)' }}>{form.description}</div>
              </div>
            )}
            <div>
              <div className="label" style={{ marginBottom: 8 }}>Sections Summary</div>
              {form.sections.map((s, i) => (
                <div
                  key={s._key}
                  style={{
                    padding: '8px 12px',
                    background: 'var(--bg-secondary)',
                    borderRadius: 'var(--radius-sm)',
                    marginBottom: 6,
                    fontSize: 13,
                    display: 'flex',
                    justifyContent: 'space-between',
                  }}
                >
                  <span>{s.title || `Section ${i + 1}`}</span>
                  <span style={{ color: 'var(--text-muted)' }}>
                    {s.questions?.length ?? 0} questions · {s.navigationMode}
                  </span>
                </div>
              ))}
            </div>
            <div>
              <div className="label" style={{ marginBottom: 8 }}>Settings</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {Object.entries(form.settings).map(([k, v]) =>
                  typeof v === 'boolean' ? (
                    <span key={k} className={`badge ${v ? 'badge-easy' : 'badge-default'}`}>
                      {k.replace(/_/g, ' ')}
                    </span>
                  ) : v ? (
                    <span key={k} className="badge badge-tag">
                      {k.replace(/_/g, ' ')}: {v}
                    </span>
                  ) : null
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Navigation buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <button
          className="btn btn-secondary"
          disabled={step === 0}
          onClick={() => setStep((s) => s - 1)}
        >
          <ChevronLeft size={16} /> Back
        </button>
        <div style={{ display: 'flex', gap: 10 }}>
          {step === STEPS.length - 1 ? (
            <>
              <button
                className="btn btn-secondary"
                disabled={saving}
                onClick={() => handleSave(false)}
              >
                <Save size={16} /> Save Draft
              </button>
              <button
                className="btn btn-primary"
                disabled={saving}
                onClick={() => handleSave(true)}
              >
                <Eye size={16} /> Publish
              </button>
            </>
          ) : (
            <button className="btn btn-primary" onClick={() => setStep((s) => s + 1)}>
              Next <ChevronRight size={16} />
            </button>
          )}
        </div>
      </div>

      {/* Question picker modal */}
      <Modal
        isOpen={questionPickerOpen !== null}
        onClose={() => setQuestionPickerOpen(null)}
        title="Add Question"
      >
        {bankQuestions.length === 0 ? (
          <div style={{ padding: 20, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
            No questions available in the bank
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 400, overflowY: 'auto' }}>
            {bankQuestions.map((q) => (
              <button
                key={q.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '10px 14px',
                  background: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  fontSize: 13,
                  color: 'var(--text-primary)',
                }}
                onClick={() => {
                  addQuestionToSection(questionPickerOpen, q);
                  setQuestionPickerOpen(null);
                  toast.success('Question added');
                }}
              >
                <span>{q.title ?? q.name}</span>
                {q.difficulty && (
                  <span className={`badge badge-${q.difficulty.toLowerCase()}`}>{q.difficulty}</span>
                )}
              </button>
            ))}
          </div>
        )}
      </Modal>
    </div>
  );
}
