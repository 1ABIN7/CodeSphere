import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import ExamHeader from './ExamHeader';
import QuestionPalette from './QuestionPalette';
import SectionTabs from './SectionTabs';
import QuestionRenderer from './QuestionRenderer';
import QuestionNavigator from './QuestionNavigator';
import SubmitConfirmationModal from './SubmitConfirmationModal';
import { useAutoSave } from '../../hooks/useAutoSave';
import { useServerTimer } from '../../hooks/useServerTimer';
import { onProctorEvent } from '../../utils/proctorEvents';

interface ExamSession {
  exam: any;
  sections: any[];
  timeRemainingSeconds: number;
  answers: Record<string, any>;
}

const ExamContainer: React.FC = () => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const [session, setSession] = useState<ExamSession | null>(null);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  // Fetch exam session on mount
  useEffect(() => {
    if (!examId) return;
    axios.get(`/api/exams/${examId}/session`).then(res => setSession(res.data));
  }, [examId]);

  // Auto‑save hook (debounced 800 ms)
  const handleAnswerChange = useAutoSave(examId!);

  // Server‑authoritative timer
  const { secondsRemaining, startTimer } = useServerTimer(examId!);

  // Proctoring event listeners
  useEffect(() => {
    const handleBlur = () => onProctorEvent('blur');
    const handleFocus = () => onProctorEvent('focus');
    window.addEventListener('blur', handleBlur);
    window.addEventListener('focus', handleFocus);
    return () => {
      window.removeEventListener('blur', handleBlur);
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  // Navigation guards (beforeunload, back button)
  useEffect(() => {
    const beforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', beforeUnload);
    // Prevent back navigation
    history.pushState(null, '', location.href);
    const popstate = () => {
      history.pushState(null, '', location.href);
      alert('Back navigation is disabled during the exam');
    };
    window.addEventListener('popstate', popstate);
    return () => {
      window.removeEventListener('beforeunload', beforeUnload);
      window.removeEventListener('popstate', popstate);
    };
  }, []);

  if (!session) return <div>Loading exam...</div>;

  const allQuestions = session.sections.flatMap(sec => sec.questions);
  const currentQuestion = allQuestions[currentQuestionIdx];

  const goNext = () => {
    if (currentQuestionIdx < allQuestions.length - 1) setCurrentQuestionIdx(i => i + 1);
  };
  const goPrev = () => {
    if (currentQuestionIdx > 0) setCurrentQuestionIdx(i => i - 1);
  };

  const handleSubmit = () => {
    setShowSubmitModal(true);
  };

  return (
    <div className="exam-container">
      <ExamHeader examName={session.exam.title} secondsRemaining={secondsRemaining} />
      <SectionTabs sections={session.sections} />
      <div className="exam-body" style={{ display: 'flex' }}>
        <QuestionPalette
          questions={allQuestions}
          answers={session.answers}
          currentIdx={currentQuestionIdx}
          setCurrentIdx={setCurrentQuestionIdx}
        />
        <main style={{ flex: 1, padding: '1rem' }} onCopy={e => e.preventDefault()} onPaste={e => e.preventDefault()} onContextMenu={e => e.preventDefault()}>
          <QuestionRenderer
            question={currentQuestion}
            value={session.answers[currentQuestion.id]}
            onChange={value => handleAnswerChange(currentQuestion.id, value)}
          />
        </main>
      </div>
      <QuestionNavigator
        onPrev={goPrev}
        onNext={goNext}
        onMarkForReview={() => {/* mark logic placeholder */}}
        onClear={() => handleAnswerChange(currentQuestion.id, null)}
        onSubmit={handleSubmit}
      />
      {showSubmitModal && (
        <SubmitConfirmationModal
          session={session}
          onConfirm={async () => {
            await axios.post(`/api/exams/${examId}/submit`);
            navigate('/exam/${examId}/post-submit');
          }}
          onCancel={() => setShowSubmitModal(false)}
        />
      )}
    </div>
  );
};

export default ExamContainer;
