import { useState, useEffect } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import Modal from '../ui/Modal';

const QUESTION_TYPES = [
  { value: 'CODING', label: 'Coding' },
  { value: 'MCQ_SINGLE', label: 'MCQ (Single Answer)' },
  { value: 'MCQ_MULTI', label: 'MCQ (Multiple Answers)' },
  { value: 'SUBJECTIVE', label: 'Subjective' },
  { value: 'READING_COMPREHENSION', label: 'Reading Comprehension' },
  { value: 'FILE_UPLOAD', label: 'File Upload' },
];

function emptyOption() {
  return { _key: Date.now() + Math.random(), text: '' };
}

function emptySubQuestion() {
  return { _key: Date.now() + Math.random(), text: '', options: [emptyOption(), emptyOption()] };
}

function defaultForm() {
  return {
    title: '',
    description: '',
    question_type: 'CODING',
    difficulty: 'MEDIUM',
    tags: [],
    problem_id: '',
    options: [emptyOption(), emptyOption()],
    correct_answer: '',
    correct_answers: [],
    min_words: '',
    max_words: '',
    passage_text: '',
    sub_questions: [emptySubQuestion()],
    max_file_size_mb: 10,
    accepted_file_types: '.pdf,.doc,.docx,.zip',
  };
}

export default function QuestionEditorModal({ isOpen, onClose, question, onSave }) {
  const [form, setForm] = useState(defaultForm);

  useEffect(() => {
    if (question) {
      setForm({
        title: question.title ?? '',
        description: question.description ?? '',
        question_type: question.question_type ?? question.questionType ?? 'CODING',
        difficulty: question.difficulty ?? 'MEDIUM',
        tags: question.tags ?? [],
        problem_id: question.problem_id ?? question.problemId ?? '',
        options: question.options?.length
          ? question.options.map((o) => ({ _key: Math.random(), text: typeof o === 'string' ? o : o.text ?? '' }))
          : [emptyOption(), emptyOption()],
        correct_answer: question.correct_answer ?? question.correctAnswer ?? '',
        correct_answers: question.correct_answers ?? question.correctAnswers ?? [],
        min_words: question.min_words ?? question.minWords ?? '',
        max_words: question.max_words ?? question.maxWords ?? '',
        passage_text: question.passage_text ?? question.passageText ?? '',
        sub_questions: question.sub_questions?.length
          ? question.sub_questions.map((sq) => ({
              _key: Math.random(),
              text: sq.text ?? '',
              options: (sq.options ?? []).map((o) => ({
                _key: Math.random(),
                text: typeof o === 'string' ? o : o.text ?? '',
              })),
            }))
          : [emptySubQuestion()],
        max_file_size_mb: question.max_file_size_mb ?? question.maxFileSizeMb ?? 10,
        accepted_file_types: question.accepted_file_types ?? question.acceptedFileTypes ?? '.pdf,.doc,.docx,.zip',
      });
    } else {
      setForm(defaultForm());
    }
  }, [question, isOpen]);

  const setField = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const updateOption = (idx, text) => {
    setForm((f) => {
      const options = [...f.options];
      options[idx] = { ...options[idx], text };
      return { ...f, options };
    });
  };

  const addOption = () =>
    setForm((f) => ({ ...f, options: [...f.options, emptyOption()] }));

  const removeOption = (idx) =>
    setForm((f) => ({ ...f, options: f.options.filter((_, i) => i !== idx) }));

  const toggleCorrectAnswer = (optionText) => {
    setForm((f) => {
      if (f.question_type === 'MCQ_SINGLE') {
        return { ...f, correct_answer: optionText };
      }
      const exists = f.correct_answers.includes(optionText);
      return {
        ...f,
        correct_answers: exists
          ? f.correct_answers.filter((a) => a !== optionText)
          : [...f.correct_answers, optionText],
      };
    });
  };

  const updateSubQuestion = (idx, key, value) => {
    setForm((f) => {
      const sub = [...f.sub_questions];
      sub[idx] = { ...sub[idx], [key]: value };
      return { ...f, sub_questions: sub };
    });
  };

  const updateSubQuestionOption = (sqIdx, optIdx, text) => {
    setForm((f) => {
      const sub = [...f.sub_questions];
      const opts = [...sub[sqIdx].options];
      opts[optIdx] = { ...opts[optIdx], text };
      sub[sqIdx] = { ...sub[sqIdx], options: opts };
      return { ...f, sub_questions: sub };
    });
  };

  const addSubQuestion = () =>
    setForm((f) => ({ ...f, sub_questions: [...f.sub_questions, emptySubQuestion()] }));

  const removeSubQuestion = (idx) =>
    setForm((f) => ({ ...f, sub_questions: f.sub_questions.filter((_, i) => i !== idx) }));

  const addSubQuestionOption = (sqIdx) => {
    setForm((f) => {
      const sub = [...f.sub_questions];
      sub[sqIdx] = { ...sub[sqIdx], options: [...sub[sqIdx].options, emptyOption()] };
      return { ...f, sub_questions: sub };
    });
  };

  const removeSubQuestionOption = (sqIdx, optIdx) => {
    setForm((f) => {
      const sub = [...f.sub_questions];
      sub[sqIdx] = {
        ...sub[sqIdx],
        options: sub[sqIdx].options.filter((_, i) => i !== optIdx),
      };
      return { ...f, sub_questions: sub };
    });
  };

  function handleSave() {
    const payload = { ...form };
    delete payload.options;
    delete payload.correct_answer;
    delete payload.correct_answers;
    delete payload.sub_questions;

    if (form.question_type === 'MCQ_SINGLE') {
      payload.options = form.options.map((o) => o.text);
      payload.correct_answer = form.correct_answer;
    } else if (form.question_type === 'MCQ_MULTI') {
      payload.options = form.options.map((o) => o.text);
      payload.correct_answers = form.correct_answers;
    } else if (form.question_type === 'READING_COMPREHENSION') {
      payload.passage_text = form.passage_text;
      payload.sub_questions = form.sub_questions.map((sq) => ({
        text: sq.text,
        options: sq.options.map((o) => o.text),
      }));
    } else if (form.question_type === 'SUBJECTIVE') {
      payload.min_words = form.min_words ? Number(form.min_words) : null;
      payload.max_words = form.max_words ? Number(form.max_words) : null;
    } else if (form.question_type === 'FILE_UPLOAD') {
      payload.max_file_size_mb = Number(form.max_file_size_mb);
      payload.accepted_file_types = form.accepted_file_types;
    }

    onSave(payload);
  }

  const type = form.question_type;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={question ? 'Edit Question' : 'Create Question'}
      style={{ maxWidth: 640 }}
      footer={
        <>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handleSave}>Save</button>
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {/* Common fields */}
        <div className="form-group">
          <label className="label">Question Type</label>
          <select
            className="select"
            value={type}
            onChange={(e) => setField('question_type', e.target.value)}
          >
            {QUESTION_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label className="label">Title</label>
          <input
            className="input"
            value={form.title}
            onChange={(e) => setField('title', e.target.value)}
            placeholder="Question title"
          />
        </div>

        {type !== 'READING_COMPREHENSION' && (
          <div className="form-group">
            <label className="label">Description</label>
            <textarea
              className="textarea"
              value={form.description}
              onChange={(e) => setField('description', e.target.value)}
              placeholder="Describe the question..."
            />
          </div>
        )}

        {type === 'CODING' && (
          <>
            <div className="form-group">
              <label className="label">Difficulty</label>
              <select
                className="select"
                value={form.difficulty}
                onChange={(e) => setField('difficulty', e.target.value)}
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
            <div className="form-group">
              <label className="label">Linked Problem ID (optional)</label>
              <input
                className="input"
                value={form.problem_id}
                onChange={(e) => setField('problem_id', e.target.value)}
                placeholder="Problem bank ID"
              />
            </div>
          </>
        )}

        {(type === 'MCQ_SINGLE' || type === 'MCQ_MULTI') && (
          <>
            <div className="form-group">
              <label className="label">Options</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {form.options.map((opt, idx) => (
                  <div key={opt._key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <input
                      type="checkbox"
                      checked={
                        type === 'MCQ_SINGLE'
                          ? form.correct_answer === opt.text
                          : form.correct_answers.includes(opt.text)
                      }
                      onChange={() => toggleCorrectAnswer(opt.text)}
                      style={{ width: 18, height: 18, accentColor: 'var(--accent)', flexShrink: 0 }}
                      title="Mark as correct"
                    />
                    <input
                      className="input"
                      value={opt.text}
                      onChange={(e) => updateOption(idx, e.target.value)}
                      placeholder={`Option ${idx + 1}`}
                      style={{ flex: 1 }}
                    />
                    {form.options.length > 2 && (
                      <button
                        className="btn-ghost btn-icon btn-sm"
                        style={{ color: 'var(--red)', flexShrink: 0 }}
                        onClick={() => removeOption(idx)}
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <button
                className="btn btn-ghost btn-sm"
                onClick={addOption}
                style={{ marginTop: 8, width: 'fit-content' }}
              >
                <Plus size={14} /> Add Option
              </button>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              {type === 'MCQ_SINGLE'
                ? 'Check the box next to the correct answer.'
                : 'Check all correct answers.'}
            </div>
          </>
        )}

        {type === 'SUBJECTIVE' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="label">Min Words</label>
              <input
                className="input"
                type="number"
                min={0}
                value={form.min_words}
                onChange={(e) => setField('min_words', e.target.value)}
                placeholder="Optional"
              />
            </div>
            <div className="form-group">
              <label className="label">Max Words</label>
              <input
                className="input"
                type="number"
                min={0}
                value={form.max_words}
                onChange={(e) => setField('max_words', e.target.value)}
                placeholder="Optional"
              />
            </div>
          </div>
        )}

        {type === 'READING_COMPREHENSION' && (
          <>
            <div className="form-group">
              <label className="label">Passage Text</label>
              <textarea
                className="textarea"
                rows={6}
                value={form.passage_text}
                onChange={(e) => setField('passage_text', e.target.value)}
                placeholder="Paste the reading passage here..."
              />
            </div>
            <div className="form-group">
              <label className="label">Sub-Questions</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {form.sub_questions.map((sq, sqIdx) => (
                  <div
                    key={sq._key}
                    style={{
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius-sm)',
                      padding: 12,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8,
                    }}
                  >
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span style={{ fontSize: 12, color: 'var(--text-muted)', flexShrink: 0 }}>
                        Q{sqIdx + 1}
                      </span>
                      <input
                        className="input"
                        value={sq.text}
                        onChange={(e) => updateSubQuestion(sqIdx, 'text', e.target.value)}
                        placeholder="Sub-question text"
                        style={{ flex: 1 }}
                      />
                      {form.sub_questions.length > 1 && (
                        <button
                          className="btn-ghost btn-icon btn-sm"
                          style={{ color: 'var(--red)' }}
                          onClick={() => removeSubQuestion(sqIdx)}
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4, paddingLeft: 24 }}>
                      {sq.options.map((opt, oIdx) => (
                        <div key={opt._key} style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                          <input
                            className="input"
                            value={opt.text}
                            onChange={(e) => updateSubQuestionOption(sqIdx, oIdx, e.target.value)}
                            placeholder={`Option ${oIdx + 1}`}
                            style={{ flex: 1 }}
                          />
                          {sq.options.length > 2 && (
                            <button
                              className="btn-ghost btn-icon btn-sm"
                              style={{ color: 'var(--red)' }}
                              onClick={() => removeSubQuestionOption(sqIdx, oIdx)}
                            >
                              <Trash2 size={12} />
                            </button>
                          )}
                        </div>
                      ))}
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => addSubQuestionOption(sqIdx)}
                        style={{ width: 'fit-content', fontSize: 12 }}
                      >
                        <Plus size={12} /> Add Option
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              <button
                className="btn btn-ghost btn-sm"
                onClick={addSubQuestion}
                style={{ marginTop: 8, width: 'fit-content' }}
              >
                <Plus size={14} /> Add Sub-Question
              </button>
            </div>
          </>
        )}

        {type === 'FILE_UPLOAD' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="label">Max File Size (MB)</label>
              <input
                className="input"
                type="number"
                min={1}
                value={form.max_file_size_mb}
                onChange={(e) => setField('max_file_size_mb', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="label">Accepted File Types</label>
              <input
                className="input"
                value={form.accepted_file_types}
                onChange={(e) => setField('accepted_file_types', e.target.value)}
                placeholder=".pdf,.doc,.zip"
              />
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
