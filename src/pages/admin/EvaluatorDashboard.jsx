import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ChevronUp, Send, ClipboardCheck } from 'lucide-react';
import toast from 'react-hot-toast';

// TODO: backend endpoint pending — see EvaluationController
const MOCK_EVALUATIONS = [
  {
    id: 1,
    candidateName: 'Alice Johnson',
    assessmentTitle: 'Frontend Developer Hiring - React',
    questionType: 'SUBJECTIVE',
    submittedAt: '2026-07-18T14:30:00Z',
    questionText: 'Explain the concept of virtual DOM and its benefits in React.',
    answer: 'Virtual DOM is a lightweight representation of the real DOM...',
    rubricScores: { clarity: 0, accuracy: 0, completeness: 0 },
  },
  {
    id: 2,
    candidateName: 'Bob Smith',
    assessmentTitle: 'Frontend Developer Hiring - React',
    questionType: 'CODING',
    submittedAt: '2026-07-18T15:10:00Z',
    questionText: 'Implement a function to flatten a nested array.',
    answer: 'function flatten(arr) { return arr.flat(Infinity); }',
    rubricScores: { correctness: 0, efficiency: 0, readability: 0 },
  },
  {
    id: 3,
    candidateName: 'Carol White',
    assessmentTitle: 'Java Backend Core Concepts',
    questionType: 'WRITTEN',
    submittedAt: '2026-07-17T09:45:00Z',
    questionText: 'Describe the difference between HashMap and ConcurrentHashMap.',
    answer: 'HashMap is not thread-safe while ConcurrentHashMap is...',
    rubricScores: { technicalAccuracy: 0, depth: 0 },
  },
];

export default function EvaluatorDashboard() {
  const [items, setItems] = useState(MOCK_EVALUATIONS);
  const [expandedId, setExpandedId] = useState(null);
  const [feedbacks, setFeedbacks] = useState({});
  const [scores, setScores] = useState({});

  function toggleExpand(id) {
    setExpandedId((prev) => (prev === id ? null : id));
  }

  function setFeedback(id, text) {
    setFeedbacks((prev) => ({ ...prev, [id]: text }));
  }

  function setScore(id, rubricKey, val) {
    setScores((prev) => ({
      ...prev,
      [id]: { ...(prev[id] ?? {}), [rubricKey]: Number(val) },
    }));
  }

  // TODO: backend endpoint pending — see EvaluationController
  async function submitEvaluation(item) {
    try {
      // TODO: call real endpoint once EvaluationController is implemented
      await new Promise((r) => setTimeout(r, 500));
      toast.success(`Evaluation submitted for ${item.candidateName}`);
      setItems((prev) => prev.filter((e) => e.id !== item.id));
      setExpandedId(null);
    } catch {
      toast.error('Failed to submit evaluation');
    }
  }

  const rubricKeys = (item) => Object.keys(item.rubricScores);

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title" style={{ fontSize: 24 }}>Evaluations</h1>
        <p className="page-subtitle">
          Pending submissions awaiting manual review
          <span style={{ marginLeft: 8, color: 'var(--yellow)', fontSize: 12 }}>
            // TODO: backend endpoint pending — see EvaluationController
          </span>
        </p>
      </div>

      {items.length === 0 ? (
        <div className="empty-state">
          <ClipboardCheck size={48} style={{ opacity: 0.4, marginBottom: 16 }} />
          <div className="empty-title">All caught up</div>
          <div className="empty-subtitle">No pending evaluations</div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {items.map((item) => {
            const isOpen = expandedId === item.id;
            return (
              <div key={item.id} className="card" style={{ padding: 0 }}>
                <button
                  onClick={() => toggleExpand(item.id)}
                  style={{
                    width: '100%',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '16px 20px',
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--text-primary)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{item.candidateName}</div>
                    <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                      {item.assessmentTitle}
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', display: 'flex', gap: 8 }}>
                      <span className="badge badge-default">{item.questionType}</span>
                      <span>{new Date(item.submittedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  {isOpen ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                </button>

                <AnimatePresence>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      style={{ overflow: 'hidden' }}
                    >
                      <div style={{ padding: '0 20px 20px', borderTop: '1px solid var(--border)' }}>
                        <div style={{ marginTop: 16, marginBottom: 12 }}>
                          <div className="label" style={{ marginBottom: 4 }}>Question</div>
                          <div style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
                            {item.questionText}
                          </div>
                        </div>

                        <div style={{ marginBottom: 16 }}>
                          <div className="label" style={{ marginBottom: 4 }}>Answer</div>
                          <div
                            style={{
                              fontSize: 14,
                              padding: 12,
                              background: 'var(--bg-secondary)',
                              borderRadius: 'var(--radius-sm)',
                              fontFamily: 'var(--font-mono)',
                              whiteSpace: 'pre-wrap',
                            }}
                          >
                            {item.answer}
                          </div>
                        </div>

                        <div style={{ marginBottom: 16 }}>
                          <div className="label" style={{ marginBottom: 8 }}>Rubric Scores</div>
                          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                            {rubricKeys(item).map((key) => (
                              <div key={key} className="form-group" style={{ minWidth: 120 }}>
                                <label className="label" style={{ textTransform: 'capitalize' }}>
                                  {key}
                                </label>
                                <input
                                  className="input"
                                  type="number"
                                  min={0}
                                  max={10}
                                  value={scores[item.id]?.[key] ?? ''}
                                  onChange={(e) => setScore(item.id, key, e.target.value)}
                                  placeholder="0-10"
                                />
                              </div>
                            ))}
                          </div>
                        </div>

                        <div style={{ marginBottom: 16 }}>
                          <div className="label" style={{ marginBottom: 4 }}>Feedback</div>
                          <textarea
                            className="textarea"
                            rows={3}
                            value={feedbacks[item.id] ?? ''}
                            onChange={(e) => setFeedback(item.id, e.target.value)}
                            placeholder="Provide feedback to the candidate..."
                          />
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => submitEvaluation(item)}
                          >
                            <Send size={14} /> Submit Evaluation
                          </button>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
