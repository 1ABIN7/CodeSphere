import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import toast from 'react-hot-toast';
import { assessmentAPI, problemsAPI, proctoringAPI, submissionsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import { getSectionSecondsRemaining } from '../utils/assessmentTimer';

const QUESTION_TYPES = {
  MCQ_SINGLE: 'MCQ_SINGLE',
  MCQ_MULTI: 'MCQ_MULTI',
  CODING: 'CODING',
  SQL: 'SQL',
  API_IMPLEMENTATION: 'API_IMPLEMENTATION',
  DEBUGGING: 'DEBUGGING',
  SUBJECTIVE: 'SUBJECTIVE',
  WRITTEN: 'WRITTEN',
  READING_COMPREHENSION: 'READING_COMPREHENSION',
  FILE_UPLOAD: 'FILE_UPLOAD',
};

const STARTER_CODE = {
  java: `public class Main {\n  public static void main(String[] args) {\n    System.out.println("hello");\n  }\n}`,
  python: `def solve():\n    print("hello")\n\nsolve()` ,
  cpp: `#include <iostream>\nusing namespace std;\nint main(){ cout << "hello"; }`,
  c: `#include <stdio.h>\nint main(){ puts("hello"); }`,
  javascript: `console.log('hello');`,
};

const LANGUAGES = [
  { value: 'java', label: 'Java', monacoLan: 'java' },
  { value: 'python', label: 'Python', monacoLan: 'python' },
  { value: 'cpp', label: 'C++', monacoLan: 'cpp' },
  { value: 'c', label: 'C', monacoLan: 'c' },
  { value: 'javascript', label: 'JavaScript', monacoLan: 'javascript' },
];

function getInitialState() {
  try {
    return JSON.parse(localStorage.getItem('codesphere-assessment-progress') || '{}');
  } catch {
    return {};
  }
}

function saveProgress(assessmentId, payload) {
  const next = getInitialState();
  next[String(assessmentId)] = payload;
  localStorage.setItem('codesphere-assessment-progress', JSON.stringify(next));
}

function formatTime(seconds) {
  const mins = Math.max(0, Math.floor(seconds / 60));
  const secs = Math.max(0, seconds % 60);
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function LoadingSkeleton() {
  return (
    <div className="container fade-in" style={{ paddingBottom: 40 }}>
      <div className="card" style={{ padding: 24 }}>
        <div className="assessment-skeleton" />
        <div className="assessment-skeleton" />
        <div className="assessment-skeleton" />
      </div>
    </div>
  );
}

export default function AssessmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [assessment, setAssessment] = useState(null);
  const [session, setSession] = useState(null);
  const [sections, setSections] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeSection, setActiveSection] = useState(0);
  const [activeQuestion, setActiveQuestion] = useState(0);
  const [answers, setAnswers] = useState({});
  const [saveState, setSaveState] = useState('idle');
  const [timeLeft, setTimeLeft] = useState(0);
  const [sectionTimeLeft, setSectionTimeLeft] = useState(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [fullscreenWarning, setFullscreenWarning] = useState(false);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);
  const [language, setLanguage] = useState('java');
  const [code, setCode] = useState(STARTER_CODE.java);
  const [codeByQuestion, setCodeByQuestion] = useState({});
  const [starterCodeByQuestion, setStarterCodeByQuestion] = useState({});
  const [uploading, setUploading] = useState(false);
  const [fileName, setFileName] = useState('');
  const [dragActive, setDragActive] = useState(false);
  const [codeVerdict, setCodeVerdict] = useState(null);
  const [codeOutput, setCodeOutput] = useState('');
  const [codingSubmissionPending, setCodingSubmissionPending] = useState(false);
  const [codingHistory, setCodingHistory] = useState([]);
  const [readingView, setReadingView] = useState(null);
  const saveTimerRef = useRef(null);
  const sessionRef = useRef(null);

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }

    let active = true;
    const bootstrap = async () => {
      setLoading(true);
      setError('');
      try {
        const [assessmentRes, sectionsRes, questionsRes] = await Promise.all([
          assessmentAPI.getById(id),
          assessmentAPI.getSections(id),
          assessmentAPI.getQuestions(id),
        ]);

        if (!active) return;
        setAssessment(assessmentRes.data || null);
        const loadedSections = sectionsRes.data || [];
        setSections(loadedSections);
        const saved = getInitialState()[String(id)] || {};
        setAnswers(saved.answers || {});
        setActiveQuestion(saved.activeQuestion || 0);

        try {
          const sessionRes = await assessmentAPI.startSession(id);
          if (!active) return;
          const activeSession = sessionRes.data || { id: null };
          const serverSection = Math.max(0, activeSession.currentSectionIndex || 0);
          sessionRef.current = activeSession;
          setSession(activeSession);
          setActiveSection(serverSection);
          setTimeLeft(Math.max(0, (assessmentRes.data?.durationMinutes || 60) * 60 - Math.floor((Date.now() - new Date(activeSession.startedAt || Date.now()).getTime()) / 1000)));
          const initialSectionQuestions = loadedSections[serverSection]
            ? await assessmentAPI.getSectionQuestions(loadedSections[serverSection].id).catch(() => questionsRes)
            : questionsRes;
          if (active) setQuestions(initialSectionQuestions.data || []);
        } catch (startErr) {
          // TODO: replace with real resume endpoint once backend exposes it.
          const fallback = await assessmentAPI.getSession(id).catch(() => null);
          if (active) {
            if (fallback?.data) {
              sessionRef.current = fallback.data;
              setSession(fallback.data);
              const serverSection = Math.max(0, fallback.data.currentSectionIndex || 0);
              setActiveSection(serverSection);
              const initialSectionQuestions = loadedSections[serverSection]
                ? await assessmentAPI.getSectionQuestions(loadedSections[serverSection].id).catch(() => questionsRes)
                : questionsRes;
              setQuestions(initialSectionQuestions.data || []);
            } else {
              throw startErr;
            }
          }
        }
      } catch (err) {
        if (active) {
          const message = err.response?.data?.message || err.message || 'We could not initialize the assessment session.';
          setError(message);
          toast.error(message);
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    bootstrap();
    return () => { active = false; };
  }, [id, isLoggedIn, navigate]);

  useEffect(() => {
    if (!session?.id && !assessment?.id) return;
    const tick = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(tick);
  }, [session, assessment]);

  useEffect(() => {
    setSectionTimeLeft(getSectionSecondsRemaining(sections[activeSection], session));
  }, [activeSection, sections, session]);

  useEffect(() => {
    if (sectionTimeLeft === null) return undefined;
    const timer = setInterval(() => setSectionTimeLeft((value) => value > 0 ? value - 1 : 0), 1000);
    return () => clearInterval(timer);
  }, [sectionTimeLeft === null]);

  useEffect(() => {
    if (sectionTimeLeft !== 0 || !sections.length) return;
    if (activeSection + 1 < sections.length) { toast('Section time is up. Moving to the next section.'); navigateSection(activeSection + 1); }
    else { toast('Final section time is up. Submit your assessment now.'); }
  }, [sectionTimeLeft, activeSection, sections.length]);

  useEffect(() => {
    if (!session?.id) return;
    const progressPayload = {
      assessmentId: id,
      started: true,
      activeSection,
      activeQuestion,
      answers,
      completed: false,
    };
    saveProgress(id, progressPayload);
  }, [activeSection, activeQuestion, answers, id, session]);

  useEffect(() => {
    const handleFullscreenChange = () => {
      if (!document.fullscreenElement) {
        setFullscreenWarning(true);
      }
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  useEffect(() => {
    if (assessment?.isFullscreenRequired) {
      document.documentElement.requestFullscreen?.().catch(() => {});
    }
  }, [assessment]);

  useEffect(() => {
    const question = questions[activeQuestion];
    if (!session?.id || question?.questionType !== QUESTION_TYPES.READING_COMPREHENSION) {
      setReadingView(null);
      return undefined;
    }
    let active = true;
    const refresh = async () => {
      try {
        const { data } = await assessmentAPI.getReadingView(session.id, question.id);
        if (active) setReadingView(data);
      } catch { if (active) setReadingView(null); }
    };
    refresh();
    const timer = setInterval(refresh, 1000);
    return () => { active = false; clearInterval(timer); };
  }, [session?.id, activeQuestion, questions]);

  const persistAnswer = async (questionId, value) => {
    if (!session?.id) return;
    setSaveState('saving');
    try {
      await assessmentAPI.saveAnswer(session.id, questionId, value);
      setSaveState('saved');
    } catch {
      setSaveState('error');
    }
  };

  const debouncedSave = useMemo(() => {
    return (questionId, value) => {
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
      saveTimerRef.current = setTimeout(() => persistAnswer(questionId, value), 600);
    };
  }, [session]);

  const handleAnswerChange = (questionId, value) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
    if (session?.id) debouncedSave(questionId, value);
  };

  const navigateSection = async (nextIndex) => {
    const target = sections[nextIndex];
    if (!target) return;
    const previousSection = sections[activeSection];
    const isSequential = previousSection?.navigationMode === 'SEQUENTIAL';
    if (isSequential && nextIndex > activeSection) {
      toast.error('This section is locked until the current one is completed.');
      return;
    }
    try {
      const transition = await assessmentAPI.navigateSection(session.id, nextIndex);
      const activeSession = transition.data || session;
      const serverIndex = activeSession.currentSectionIndex ?? nextIndex;
      sessionRef.current = activeSession;
      setSession(activeSession);
      setActiveSection(serverIndex);
      setActiveQuestion(0);
      const serverTarget = sections[serverIndex];
      if (!serverTarget) return;
      const { data } = await assessmentAPI.getSectionQuestions(serverTarget.id);
      setQuestions(data || []);
    } catch (err) { toast.error(err.response?.data?.message || 'Unable to load this section.'); }
    persistAnswer('__navigation__', JSON.stringify({ section: nextIndex, question: 0 }));
  };

  const handleSubmitSection = async () => {
    if (!session?.id) return;
    setSaveState('saved');
    saveProgress(id, {
      assessmentId: id, started: true, activeSection, activeQuestion, answers, completed: false,
    });
    toast.success('Progress saved. You can continue or submit the assessment when ready.');
  };

  const handleSubmitAssessment = async () => {
    if (!session?.id) return;
    setSubmissionPending(true);
    try {
      const res = await assessmentAPI.submitAssessment(id, {});
      setSubmissionResult(res.data || { status: 'PENDING' });
      toast.success('Assessment submitted successfully.');
      if (assessment?.resultsVisible !== false) {
        sessionStorage.setItem(`assessment-result-${id}`, JSON.stringify(res.data || { status: 'PENDING' }));
        navigate(`/assessments/${id}/result`);
      } else {
        toast.success('Your response was submitted. Results will be released later.');
        navigate('/assessments');
      }
    } catch {
      toast.error('Submission failed. Please try again.');
    } finally {
      setSubmissionPending(false);
    }
  };

  const handleCodingSubmit = async () => {
    if (!session?.id) return;
    const question = questions[activeQuestion];
    const taskLabel = question?.questionType === QUESTION_TYPES.API_IMPLEMENTATION ? 'HTTP tests' : question?.questionType === QUESTION_TYPES.SQL ? 'SQL tests' : 'the judge';
    setCodingSubmissionPending(true);
    setCodeVerdict('RUNNING');
    setCodeOutput(`Running ${taskLabel}…`);
    try {
      if (question?.questionType === QUESTION_TYPES.API_IMPLEMENTATION && language !== 'javascript') setLanguage('javascript');
      if (question?.questionType === QUESTION_TYPES.SQL) {
        const { data } = await assessmentAPI.runSql(session.id, question.id, code);
        setCodeVerdict(data.status || 'COMPLETED');
        setCodeOutput(`${data.passedTestCases ?? 0}/${data.totalTestCases ?? 0} SQL test cases passed · ${data.scorePercent ?? 0}%`);
        return;
      }
      if (question?.questionType === QUESTION_TYPES.API_IMPLEMENTATION) {
        const { data } = await assessmentAPI.runApi(session.id, question.id, code);
        setCodeVerdict(data.status || 'COMPLETED');
        setCodeOutput(`${data.passedTestCases ?? 0}/${data.totalTestCases ?? 0} HTTP checks passed · ${data.scorePercent ?? 0}%${data.message ? ` · ${data.message}` : ''}`);
        return;
      }
      if (!question?.codingProblemId) {
        toast.error('This coding question is not linked to a coding problem yet.');
        return;
      }
      await assessmentAPI.saveAnswer(session.id, question.id, code);
      const res = await assessmentAPI.submitCodingAnswer(question.codingProblemId, { language, code }, session.id, question.id);
      setCodeVerdict(res.data?.status || 'PENDING');
      setCodeOutput(res.data?.submissionId ? `Judge submission #${res.data.submissionId} queued.` : res.data?.output || '');
      if (res.data?.submissionId) {
        const submissionId = res.data.submissionId;
        const poll = async (remaining = 12) => {
          try {
            const { data } = await submissionsAPI.getById(submissionId);
            setCodingHistory((items) => [{ ...data, id: submissionId }, ...items.filter((item) => item.id !== submissionId)].slice(0, 8));
            setCodeVerdict(data.status || 'PENDING');
            if (data.status && !['PENDING', 'RUNNING'].includes(data.status)) { setCodeOutput(`${data.testCasesPassed ?? 0}/${data.totalTestCases ?? 0} test cases passed · ${data.execTime ?? 0} ms · ${data.execMemory ?? 0} KB${data.errorMessage ? `\n\n${data.errorMessage}` : ''}`); return; }
          } catch { /* judge may still be writing the submission */ }
          if (remaining > 0) setTimeout(() => poll(remaining - 1), 1500);
        };
        setTimeout(() => poll(), 1000);
      }
    } catch (error) {
      setCodeVerdict('ERROR');
      setCodeOutput(error.response?.data?.message || 'The coding submission could not be started. Please try again.');
    } finally {
      setCodingSubmissionPending(false);
    }
  };

  const handleUpload = async (file) => {
    if (!session?.id) return;
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!['pdf', 'docx', 'pptx', 'zip'].includes(extension) || file.size > 50 * 1024 * 1024) {
      toast.error('Upload a PDF, DOCX, PPTX, or ZIP file no larger than 50 MB.');
      return;
    }
    const question = questions[activeQuestion];
    const formData = new FormData();
    formData.append('file', file);
    setUploading(true);
    try {
      const res = await assessmentAPI.uploadFile(session.id, question?.id, formData);
      setFileName(res.data?.fileName || file.name);
      toast.success('File uploaded.');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Unable to upload this file.');
    } finally {
      setUploading(false);
    }
  };

  const currentSection = sections[activeSection] || null;
  const currentQuestion = questions[activeQuestion] || null;

  useEffect(() => {
    const type = currentQuestion?.questionType || currentQuestion?.type;
    if (!['CODING', 'DEBUGGING', 'SQL', 'API_IMPLEMENTATION'].includes(type)) return;
    const defaultCode = type === 'SQL' ? '' : STARTER_CODE[type === 'API_IMPLEMENTATION' ? 'javascript' : language];
    setCode(codeByQuestion[currentQuestion.id] ?? defaultCode ?? '');
    setCodeVerdict(null);
    setCodeOutput('');
    setCodingHistory([]);
  }, [currentQuestion?.id]);

  useEffect(() => {
    if (currentQuestion?.questionType !== QUESTION_TYPES.DEBUGGING || !currentQuestion?.codingProblemId || codeByQuestion[currentQuestion.id] !== undefined) return;
    let active = true;
    problemsAPI.getById(currentQuestion.codingProblemId).then(({ data }) => {
      const templates = data?.starterCode || {};
      const repairCode = templates[language] || templates.java || Object.values(templates)[0];
      if (!active || !repairCode) return;
      setStarterCodeByQuestion((saved) => ({ ...saved, [currentQuestion.id]: repairCode }));
      setCodeByQuestion((saved) => ({ ...saved, [currentQuestion.id]: repairCode }));
      setCode(repairCode);
    }).catch(() => { /* A generic template remains available if this task has no repair file. */ });
    return () => { active = false; };
  }, [currentQuestion?.id]);

  const updateCodeForQuestion = (nextCode) => {
    setCode(nextCode);
    if (currentQuestion?.id) setCodeByQuestion((saved) => ({ ...saved, [currentQuestion.id]: nextCode }));
  };

  const resetCodeForQuestion = () => {
    if (!currentQuestion) return;
    const type = currentQuestion.questionType || currentQuestion.type;
    const fallback = type === 'SQL' ? '' : STARTER_CODE[type === 'API_IMPLEMENTATION' ? 'javascript' : language];
    updateCodeForQuestion(starterCodeByQuestion[currentQuestion.id] ?? fallback ?? '');
  };

  if (loading) return <LoadingSkeleton />;
  if (error) {
    return (
      <div className="container fade-in">
        <div className="card empty-state">
          <div className="empty-icon">⚠️</div>
          <div className="empty-title">Assessment could not be loaded</div>
          <div className="empty-subtitle">{error}</div>
          <button className="btn btn-primary mt-4" onClick={() => window.location.reload()}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="container fade-in" style={{ paddingBottom: 60 }}>
      {fullscreenWarning && (
        <div className="card" style={{ borderColor: 'rgba(245, 158, 11, 0.45)', marginBottom: 16 }}>
          <strong>Fullscreen warning:</strong> Please stay in fullscreen mode while the assessment is active.
        </div>
      )}

      <div className="card" style={{ padding: 20, marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div className="badge badge-tag">{assessment?.assessmentType || 'MIXED'}</div>
            <h1 style={{ fontSize: 24, fontWeight: 800, marginTop: 8 }}>{assessment?.title || 'Assessment'}</h1>
          </div>
          <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--accent-light)' }}>
            ⏱ {formatTime(timeLeft)}
          </div>
        </div>
        <div style={{ marginTop: 12, color: 'var(--text-secondary)' }}>
          This is a client-side countdown display only. The server enforces the real deadline.
        </div>
      </div>

      <div className="assessment-shell">
        <aside className="assessment-sidebar">
          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Sections</h3>
          <div className="section-tabs">
            {sections.map((section, index) => (
              <button
                key={section.id}
                className={`section-tab ${index === activeSection ? 'active' : ''}`}
                onClick={() => navigateSection(index)}
                disabled={section.navigationMode === 'SEQUENTIAL' && index > activeSection}
              >
                <span>{section.title}</span>
                <span>{index < activeSection ? '✓' : '•'}</span>
              </button>
            ))}
          </div>

          <h3 style={{ fontSize: 15, fontWeight: 700, marginTop: 20, marginBottom: 12 }}>Questions</h3>
          <div className="question-grid question-navigation-list">
            {questions.map((q, index) => {
              const value = answers[q.id];
              const state = value ? 'answered' : 'unanswered';
              return (
                <button
                  key={q.id}
                  className={`question-pill ${state}`}
                  onClick={() => setActiveQuestion(index)}
                >
                  <span>{index + 1}</span>
                  <small>{q.title || q.questionType || 'Question'}</small>
                </button>
              );
            })}
          </div>
        </aside>

        <section className="assessment-main">
          <div className="card" style={{ padding: 24 }}>
            <div className="card-header" style={{ marginBottom: 16 }}>
              <div>
                <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Section</div>
                <h2 style={{ fontSize: 20, fontWeight: 700 }}>{currentSection?.title || 'Section'}</h2>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                {sectionTimeLeft !== null && <span className="badge badge-tag">Section: {formatTime(sectionTimeLeft)}</span>}
                <button className="btn btn-secondary btn-sm" onClick={() => setShowConfirm(true)}>Submit assessment</button>
                <button className="btn btn-primary btn-sm" onClick={handleSubmitSection}>Save progress</button>
              </div>
            </div>

            <div style={{ color: 'var(--text-secondary)', marginBottom: 12 }}>
              {saveState === 'saving' && 'Saving…'}
              {saveState === 'saved' && 'Saved just now'}
              {saveState === 'error' && 'Autosave failed'}
            </div>

            {currentQuestion ? (
              <div>
                <div style={{ marginBottom: 16 }}>
                  <div className="text-muted" style={{ fontSize: 12, textTransform: 'uppercase' }}>Question {activeQuestion + 1}</div>
                  {currentQuestion.title && <div style={{ fontSize: 14, fontWeight: 700, marginTop: 4, color: 'var(--accent)' }}>{currentQuestion.title}</div>}
                  <div style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>{currentQuestion.prompt || currentQuestion.content || 'Question prompt'}</div>
                </div>

                {renderQuestion(currentQuestion, answers, handleAnswerChange, language, setLanguage, code, updateCodeForQuestion, handleCodingSubmit, codeVerdict, codeOutput, codingSubmissionPending, uploading, fileName, dragActive, setDragActive, handleUpload, readingView, codingHistory, resetCodeForQuestion)}
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-icon">📋</div>
                <div className="empty-title">No questions available</div>
                <div className="empty-subtitle">The backend does not expose a session snapshot yet.</div>
              </div>
            )}
          </div>
        </section>
      </div>

      {showConfirm && (
        <div className="modal-backdrop">
          <div className="card" style={{ maxWidth: 480, width: '100%' }}>
            <h3 style={{ fontSize: 20, fontWeight: 700, marginBottom: 12 }}>Submit assessment?</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 16 }}>
              You have answered {Object.keys(answers).length} of {questions.length} questions. Are you sure you want to finish this assessment?
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button className="btn btn-secondary" onClick={() => setShowConfirm(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSubmitAssessment} disabled={submissionPending}>
                {submissionPending ? 'Submitting…' : 'Confirm submit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function renderQuestion(currentQuestion, answers, onAnswerChange, language, setLanguage, code, setCode, onCodingSubmit, codeVerdict, codeOutput, codingSubmissionPending, uploading, fileName, dragActive, setDragActive, onUpload, readingView = null, codingHistory = [], onResetCode = () => {}) {
  const type = currentQuestion?.questionType || currentQuestion?.type || 'MCQ_SINGLE';
  const value = answers[currentQuestion?.id] || '';
  const options = currentQuestion?.options || [];

  switch (type) {
    case QUESTION_TYPES.MCQ_SINGLE:
      return (
        <div className="question-stack">
          {options.map((option, index) => (
            <label key={option + index} className="choice-option">
              <input
                type="radio"
                name={`q-${currentQuestion.id}`}
                checked={value === String.fromCharCode(65 + index)}
                onChange={() => onAnswerChange(currentQuestion.id, String.fromCharCode(65 + index))}
              />
              <span>{String.fromCharCode(65 + index)}. {option}</span>
            </label>
          ))}
        </div>
      );
    case QUESTION_TYPES.MCQ_MULTI:
      return (
        <div className="question-stack">
          {(options || []).map((option, index) => {
            const label = String.fromCharCode(65 + index);
            const selectedValues = Array.isArray(value) ? value : String(value || '').split(',').filter(Boolean);
            const selected = selectedValues.includes(label);
            return (
              <label key={option + index} className="choice-option">
                <input
                  type="checkbox"
                  checked={selected}
                  onChange={() => {
                    const next = [...selectedValues];
                    const exists = next.includes(label);
                    const updated = exists ? next.filter((entry) => entry !== label) : [...next, label];
                    onAnswerChange(currentQuestion.id, updated.join(','));
                  }}
                />
                <span>{label}. {option}</span>
              </label>
            );
          })}
        </div>
      );
    case QUESTION_TYPES.CODING:
    case QUESTION_TYPES.API_IMPLEMENTATION:
    case QUESTION_TYPES.DEBUGGING:
    case QUESTION_TYPES.SQL:
      const isSql = currentQuestion.questionType === QUESTION_TYPES.SQL;
      const isApi = currentQuestion.questionType === QUESTION_TYPES.API_IMPLEMENTATION;
      return (
        <div>
          <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
            {!isSql && !isApi && <select className="select" value={language} onChange={(event) => setLanguage(event.target.value)}>
              {LANGUAGES.map((lang) => <option key={lang.value} value={lang.value}>{lang.label}</option>)}
            </select>}
            <span className="badge badge-tag">{isSql ? 'SQL query task' : currentQuestion.questionType === QUESTION_TYPES.API_IMPLEMENTATION ? 'API implementation task' : currentQuestion.questionType === QUESTION_TYPES.DEBUGGING ? 'Debugging task' : 'Coding task'} · {currentQuestion.title || 'Untitled task'}</span>
            <button className="btn btn-secondary btn-sm" onClick={onResetCode}>Reset</button>
            <button className="btn btn-secondary btn-sm" onClick={onCodingSubmit} disabled={codingSubmissionPending}>{codingSubmissionPending ? (isSql ? 'Running SQL tests…' : currentQuestion.questionType === QUESTION_TYPES.API_IMPLEMENTATION ? 'Running HTTP tests…' : 'Submitting…') : (isSql ? 'Run SQL tests' : currentQuestion.questionType === QUESTION_TYPES.API_IMPLEMENTATION ? 'Run HTTP tests' : 'Submit to judge')}</button>
          </div>
          <div className="coding-container">
            <Editor
              height="320px"
              language={isSql ? 'sql' : isApi ? 'javascript' : (LANGUAGES.find((entry) => entry.value === language)?.monacoLan || 'javascript')}
              value={code}
              onChange={(next) => setCode(next || '')}
              theme="vs-dark"
            />
          </div>
          {isSql && <div className="text-secondary" style={{ marginTop: 10 }}>
            Write one read-only query. Use the exact value stated in the question (for example, <code>120000</code>) rather than a <code>?</code> parameter placeholder. A final semicolon is optional.
          </div>}
          {isApi && <div className="text-secondary" style={{ marginTop: 10 }}>Export a Node HTTP server that listens on port 3000. Your service is tested privately without internet access.</div>}
          {codeVerdict && <div className={`feedback-box ${codeVerdict === 'ERROR' || codeVerdict === 'WRONG_ANSWER' ? 'incorrect' : 'correct'}`}>{codeVerdict === 'RUNNING' ? 'RUNNING — your request was received.' : codeVerdict}</div>}
          {codeOutput && <pre className="sample-code" style={{ marginTop: 12 }}>{codeOutput}</pre>}
          {codingHistory.length > 0 && <div className="card" style={{ marginTop: 14, padding: 14 }}><strong>Submission history</strong>{codingHistory.map((submission) => <div key={submission.id} className="text-secondary" style={{ marginTop: 7 }}>{submission.status} · {submission.testCasesPassed ?? 0}/{submission.totalTestCases ?? 0} tests · {submission.execTime ?? 0} ms · {submission.execMemory ?? 0} KB</div>)}</div>}
        </div>
      );
    case QUESTION_TYPES.SUBJECTIVE:
    case QUESTION_TYPES.WRITTEN:
      return (
        <div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}><button type="button" className="btn btn-secondary btn-sm" onMouseDown={(event) => { event.preventDefault(); document.execCommand('bold'); }}>Bold</button><button type="button" className="btn btn-secondary btn-sm" onMouseDown={(event) => { event.preventDefault(); document.execCommand('italic'); }}>Italic</button><button type="button" className="btn btn-secondary btn-sm" onMouseDown={(event) => { event.preventDefault(); document.execCommand('insertUnorderedList'); }}>List</button></div>
          <RichTextAnswer questionId={currentQuestion.id} value={value} onChange={(nextValue) => onAnswerChange(currentQuestion.id, nextValue)} />
          <div style={{ marginTop: 8, color: 'var(--text-secondary)' }}>
            Word count: {String(value || '').replace(/<[^>]*>/g, ' ').trim().split(/\s+/).filter(Boolean).length}
          </div>
        </div>
      );
    case QUESTION_TYPES.READING_COMPREHENSION:
      const isReading = readingView?.phase === 'READING';
      const subQuestions = readingView?.subQuestions ?? currentQuestion.subQuestions ?? [];
      return (
        <div>
          <div className="card" style={{ background: 'rgba(10,10,18,0.7)', marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, fontWeight: 700, marginBottom: 8 }}><span>Passage</span>{isReading && <span className="badge badge-tag">Reading time: {formatTime(readingView.readingDurationSeconds || 0)}</span>}</div>
            <div style={{ color: 'var(--text-secondary)' }}>{readingView?.passageText || currentQuestion.passageText || 'Reading passage goes here.'}</div>
          </div>
          {isReading && <div className="empty-state"><div className="empty-title">Read the passage</div><div className="empty-subtitle">Questions unlock when the reading timer ends.</div></div>}
          {!isReading && <div className="question-stack">
            {subQuestions.map((subQuestion) => (
              <div key={subQuestion.id} className="card" style={{ padding: 16, marginBottom: 12 }}>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>{subQuestion.prompt}</div>
                {renderQuestion(subQuestion, answers, onAnswerChange, language, setLanguage, code, setCode, onCodingSubmit, codeVerdict, codeOutput, codingSubmissionPending, uploading, fileName, dragActive, setDragActive, onUpload)}
              </div>
            ))}
          </div>}
        </div>
      );
    case QUESTION_TYPES.FILE_UPLOAD:
      return (
        <div>
          <label
            className={`upload-zone ${dragActive ? 'active' : ''}`}
            onDragOver={(event) => { event.preventDefault(); setDragActive(true); }}
            onDragLeave={() => setDragActive(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragActive(false);
              const file = event.dataTransfer.files?.[0];
              if (file) onUpload(file);
            }}
          >
            <input type="file" accept=".pdf,.docx,.pptx,.zip" style={{ display: 'none' }} onChange={(event) => event.target.files?.[0] && onUpload(event.target.files[0])} />
            <div style={{ fontWeight: 700, marginBottom: 8 }}>Drop a file here</div>
            <div style={{ color: 'var(--text-secondary)' }}>PDF, DOCX, PPTX, or ZIP · maximum 50 MB.</div>
          </label>
          {uploading && <div className="mt-4">Uploading…</div>}
          {fileName && <div className="mt-4">Uploaded: {fileName}</div>}
        </div>
      );
    default:
      return <div className="empty-state">TODO: render this question type.</div>;
  }
}

function RichTextAnswer({ questionId, value, onChange }) {
  const editorRef = useRef(null);

  useEffect(() => {
    if (editorRef.current) editorRef.current.innerHTML = value || '';
  }, [questionId]);

  return <div ref={editorRef} className="textarea" contentEditable suppressContentEditableWarning role="textbox" aria-multiline="true" style={{ minHeight: 220, overflowY: 'auto' }} onInput={(event) => onChange(event.currentTarget.innerHTML)} />;
}
