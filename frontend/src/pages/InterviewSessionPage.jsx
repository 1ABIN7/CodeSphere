import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { interviewAPI } from '../api';
import { toast } from 'react-hot-toast';

export default function InterviewSessionPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [answer, setAnswer] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [startTime, setStartTime] = useState(null);

  useEffect(() => {
    fetchNextQuestion();
  }, [id]);

  const fetchNextQuestion = async () => {
    try {
      setLoading(true);
      setFeedback(null);
      setAnswer('');
      
      const { data } = await interviewAPI.getNextQuestion(id);
      
      if (!data) {
        // No more questions, complete the session
        handleComplete();
        return;
      }
      
      setQuestion(data);
      setStartTime(Date.now());
    } catch (err) {
      toast.error('Failed to load next question');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!answer) {
      toast.error('Please provide an answer');
      return;
    }
    
    try {
      setSubmitting(true);
      const timeTaken = Math.floor((Date.now() - startTime) / 1000);
      
      const { data } = await interviewAPI.submitAnswer(id, question.id, {
        userAnswer: answer,
        timeTakenSeconds: timeTaken
      });
      
      setFeedback(data);
    } catch (err) {
      toast.error('Failed to submit answer');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleComplete = async () => {
    try {
      const { data } = await interviewAPI.completeSession(id);
      navigate('/interview/performance', { state: { result: data } });
    } catch (err) {
      toast.error('Failed to complete session');
    }
  };

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: 'calc(100vh - 64px)' }}>
        <div className="spinner" />
      </div>
    );
  }

  if (!question) {
    return null; // Will redirect shortly
  }

  const isMCQ = question.questionType.startsWith('MCQ') || question.questionType === 'TRUE_FALSE';
  const isCoding = question.questionType === 'CODING';

  return (
    <div className="session-container fade-in">
      <div className="session-header">
        <div className="session-meta">
          <span className="badge badge-tag">{question.topic}</span>
          <span className={`badge ${
            question.difficulty === 'EASY' ? 'badge-easy' : 
            question.difficulty === 'MEDIUM' ? 'badge-medium' : 'badge-hard'
          }`}>
            {question.difficulty}
          </span>
        </div>
        <button onClick={handleComplete} className="btn btn-ghost btn-sm text-muted">
          End Session Early
        </button>
      </div>

      <div className="question-card">
        <div className="question-text">
          {question.questionText}
        </div>

        <div className="question-input">
          {isMCQ ? (
            <div>
              {question.options?.map((opt) => {
                const isSelected = answer === opt.id;
                const isDisabled = feedback !== null;
                return (
                  <label 
                    key={opt.id}
                    className={`mcq-option ${isSelected ? 'selected' : ''} ${isDisabled ? 'disabled' : ''}`}
                  >
                    <input 
                      type="radio" 
                      name="answer" 
                      value={opt.id}
                      checked={isSelected}
                      onChange={(e) => setAnswer(e.target.value)}
                      disabled={isDisabled}
                      className="mcq-radio"
                    />
                    <span className="mcq-text" style={{ fontSize: '15px' }}>{opt.text}</span>
                  </label>
                );
              })}
            </div>
          ) : isCoding ? (
            <div className="coding-container">
              <Editor
                height="100%"
                defaultLanguage="javascript"
                theme="vs-dark"
                value={answer}
                onChange={setAnswer}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  readOnly: feedback !== null
                }}
              />
            </div>
          ) : (
            <textarea
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
              disabled={feedback !== null}
              className="textarea w-full"
              rows={4}
              placeholder="Type your answer here..."
              style={{ fontSize: '15px' }}
            />
          )}
        </div>

        {feedback && (
          <div className={`feedback-box slide-up ${feedback.isCorrect ? 'correct' : 'incorrect'}`}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span style={{ fontSize: '20px' }}>{feedback.isCorrect ? '✅' : '❌'}</span>
              <h3 style={{ fontSize: '18px', fontWeight: '700', color: 'inherit' }}>
                {feedback.isCorrect ? 'Correct!' : 'Incorrect'}
              </h3>
            </div>
            
            {!feedback.isCorrect && (
              <div style={{ marginBottom: '12px', fontSize: '14px', opacity: 0.9 }}>
                <strong>Correct Answer:</strong> {feedback.correctAnswer}
              </div>
            )}
            
            {feedback.explanation && (
              <div style={{ 
                marginTop: '12px', paddingTop: '12px', 
                borderTop: '1px solid currentColor', opacity: 0.8, fontSize: '14px', lineHeight: 1.6 
              }}>
                {feedback.explanation}
              </div>
            )}
          </div>
        )}

        <div className="session-actions">
          {!feedback ? (
            <button
              onClick={handleSubmit}
              disabled={submitting || !answer}
              className="btn btn-primary btn-lg"
            >
              {submitting ? (
                <><div className="spinner" style={{ width: '16px', height: '16px', borderTopColor: 'white' }} /> Submitting...</>
              ) : (
                'Submit Answer'
              )}
            </button>
          ) : (
            <button
              onClick={fetchNextQuestion}
              className="btn btn-secondary btn-lg"
            >
              Next Question →
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
