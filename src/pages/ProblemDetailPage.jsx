import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { problemsAPI, submissionsAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const LANGUAGES = [
  { value: 'java', label: 'Java', monacoLang: 'java' },
  { value: 'python', label: 'Python', monacoLang: 'python' },
  { value: 'cpp', label: 'C++', monacoLang: 'cpp' },
  { value: 'c', label: 'C', monacoLang: 'c' },
  { value: 'javascript', label: 'JavaScript', monacoLang: 'javascript' },
];

const STARTER_CODE = {
  java: `import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Read input and solve the problem
        System.out.println("answer");
    }
}`,
  python: `import sys
input = sys.stdin.readline

def solve():
    # Read input and solve the problem
    pass

solve()`,
  cpp: `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    // Read input and solve the problem
    cout << "answer" << endl;
    return 0;
}`,
  c: `#include <stdio.h>
#include <stdlib.h>

int main() {
    // Read input and solve the problem
    printf("answer\\n");
    return 0;
}`,
  javascript: `const readline = require('readline');
const rl = readline.createInterface({ input: process.stdin });
const lines = [];
rl.on('line', l => lines.push(l.trim()));
rl.on('close', () => {
    // Solve the problem using lines[]
    console.log('answer');
});`,
};

const STATUS_META = {
  ACCEPTED: { emoji: '✅', label: 'Accepted', cls: 'accepted' },
  WRONG_ANSWER: { emoji: '❌', label: 'Wrong Answer', cls: 'wrong' },
  TIME_LIMIT_EXCEEDED: { emoji: '⏱', label: 'Time Limit Exceeded', cls: 'tle' },
  RUNTIME_ERROR: { emoji: '💥', label: 'Runtime Error', cls: 'wrong' },
  COMPILATION_ERROR: { emoji: '⚙', label: 'Compilation Error', cls: 'wrong' },
  PARTIALLY_ACCEPTED: { emoji: '🟡', label: 'Partially Accepted', cls: 'tle' },
  RUNNING: { emoji: '⚙', label: 'Judging...', cls: 'tle' },
};

function DiffBadge({ d }) {
  const cls = d === 'EASY' ? 'badge-easy' : d === 'MEDIUM' ? 'badge-medium' : 'badge-hard';
  return <span className={`badge ${cls}`}>{d}</span>;
}

export default function ProblemDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isLoggedIn, isAdmin } = useAuth();

  const [problem, setProblem] = useState(null);
  const [loading, setProblemLoading] = useState(true);
  const [language, setLanguage] = useState('java');
  const [code, setCode] = useState(STARTER_CODE.java);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [activeTab, setActiveTab] = useState('description'); // description | submissions | analysis
  const [selectedTc, setSelectedTc] = useState(null);

  useEffect(() => {
    const fetchProblem = async () => {
      setProblemLoading(true);
      try {
        const res = await problemsAPI.getById(id);
        setProblem(res.data);
        // Set starter code from problem if available
        const sc = res.data.starterCode?.[language];
        if (sc) setCode(sc);
      } catch {
        toast.error('Could not load problem. Showing demo data.');
        setProblem(MOCK_PROBLEM);
      } finally {
        setProblemLoading(false);
      }
    };
    fetchProblem();
  }, [id]);

  const handleLanguageChange = (lang) => {
    setLanguage(lang);
    const sc = problem?.starterCode?.[lang] || STARTER_CODE[lang];
    setCode(sc);
  };

  const handleSubmit = async () => {
    if (!isLoggedIn) { toast.error('Please log in to submit code'); navigate('/login'); return; }
    setSubmitting(true);
    setResult(null);
    try {
      const res = await submissionsAPI.submit({ problemId: parseInt(id), language, code });
      setResult(res.data);
      setActiveTab('submissions');
      const status = res.data.status;
      if (status === 'ACCEPTED') toast.success('🎉 Accepted! All test cases passed.');
      else toast.error(`${STATUS_META[status]?.label || status}`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Submission failed. Make sure the backend is running.';
      toast.error(msg);
      // Show mock result for demo
      setResult(MOCK_RESULT);
      setActiveTab('submissions');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return (
    <div className="loading-center" style={{ height: 'calc(100vh - 64px)' }}>
      <div className="spinner" />
    </div>
  );

  const p = problem || MOCK_PROBLEM;

  return (
    <div className="problem-layout fade-in">
      {/* ── Left Panel ── */}
      <div className="problem-panel">
        <div className="problem-header">
          <div className="problem-meta">
            <span style={{ fontSize: 18, fontWeight: 800, marginRight: 4 }}>#{id}</span>
            <DiffBadge d={p.difficulty} />
            {(p.tags || []).slice(0, 3).map(t => (
              <span key={t} className="badge badge-tag">{t}</span>
            ))}
            <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, fontSize: 13, color: 'var(--text-secondary)' }}>
              <span>⏱ {p.timeLimit || 1000}ms</span>
              <span>💾 {((p.memoryLimit || 262144) / 1024).toFixed(0)}MB</span>
            </div>
          </div>
          <h1 style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.5px' }}>{p.title}</h1>

          {/* Acceptance Rate */}
          {p.acceptanceRate && (
            <div style={{ display: 'flex', gap: 20, marginTop: 12, fontSize: 13, color: 'var(--text-secondary)' }}>
              <span>✅ {parseFloat(p.acceptanceRate).toFixed(1)}% acceptance</span>
              <span>📊 {p.totalSubmissions || 0} submissions</span>
            </div>
          )}
        </div>

        {/* Tabs */}
        <div className="tabs">
          {['description', 'submissions', 'analysis'].map(t => (
            <button key={t} className={`tab ${activeTab === t ? 'active' : ''}`} onClick={() => setActiveTab(t)}>
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        {activeTab === 'description' && (
          <div className="problem-description">
            <p>{p.description}</p>

            {p.inputFormat && (
              <>
                <div className="section-title">Input Format</div>
                <p>{p.inputFormat}</p>
              </>
            )}
            {p.outputFormat && (
              <>
                <div className="section-title">Output Format</div>
                <p>{p.outputFormat}</p>
              </>
            )}
            {p.constraints && (
              <>
                <div className="section-title">Constraints</div>
                <pre style={{ whiteSpace: 'pre-wrap' }}>{p.constraints}</pre>
              </>
            )}

            {/* Sample Test Cases */}
            {(p.sampleTestCases || []).length > 0 && (
              <>
                <div className="section-title">Examples</div>
                {(p.sampleTestCases || []).map((tc, i) => (
                  <div key={tc.id || i} className="sample-testcase">
                    <div className="sample-label">Example {i + 1}</div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                      <div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>INPUT</div>
                        <pre className="sample-code">{tc.inputData}</pre>
                      </div>
                      <div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>OUTPUT</div>
                        <pre className="sample-code">{tc.expectedOutput}</pre>
                      </div>
                    </div>
                    {tc.explanation && (
                      <div style={{ marginTop: 10, fontSize: 13, color: 'var(--text-secondary)' }}>
                        💡 {tc.explanation}
                      </div>
                    )}
                  </div>
                ))}
              </>
            )}

            {/* Hints */}
            {(p.hints || []).length > 0 && (
              <>
                <div className="section-title">Hints</div>
                <div className="hints-list">
                  {p.hints.map((h, i) => (
                    <details key={i} className="hint-item" style={{ cursor: 'pointer' }}>
                      <summary style={{ fontWeight: 600, color: 'var(--accent-light)', userSelect: 'none' }}>
                        Hint {i + 1}
                      </summary>
                      <p style={{ marginTop: 8 }}>{h}</p>
                    </details>
                  ))}
                </div>
              </>
            )}
          </div>
        )}

        {activeTab === 'submissions' && result && (
          <div className="fade-in">
            {/* Verdict */}
            <div style={{
              display: 'flex', alignItems: 'center', gap: 14, padding: '20px',
              background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', marginBottom: 20
            }}>
              <span style={{ fontSize: 36 }}>{STATUS_META[result.status]?.emoji || '?'}</span>
              <div>
                <div className={`result-status ${STATUS_META[result.status]?.cls || ''}`} style={{ fontSize: 22, fontWeight: 800 }}>
                  {STATUS_META[result.status]?.label || result.status}
                </div>
                <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
                  {result.testCasesPassed}/{result.totalTestCases} test cases passed
                  {result.execTime && ` • ${result.execTime}ms`}
                  {result.score != null && ` • Score: ${result.score}/100`}
                </div>
              </div>
            </div>

            {/* Test Case Dots */}
            {(result.testCaseResults || []).length > 0 && (
              <div style={{ marginBottom: 20 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 10 }}>
                  Test Cases
                </div>
                <div className="test-cases-grid">
                  {result.testCaseResults.map((tc, i) => (
                    <div
                      key={i}
                      className={`tc-dot ${tc.status === 'ACCEPTED' ? 'passed' : 'failed'}`}
                      onClick={() => setSelectedTc(tc)}
                      title={tc.isSample ? `Case ${i + 1}: ${tc.status}` : `Case ${i + 1}: Hidden`}
                    >
                      {i + 1}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Selected Test Case Detail */}
            {selectedTc && (
              <div className="card" style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>
                  Test Case Detail — {selectedTc.status}
                </div>
                {selectedTc.isSample ? (
                  <div style={{ display: 'grid', gap: 12 }}>
                    <div>
                      <div className="sample-label">Input</div>
                      <pre className="sample-code">{selectedTc.input}</pre>
                    </div>
                    <div>
                      <div className="sample-label">Expected</div>
                      <pre className="sample-code">{selectedTc.expectedOutput}</pre>
                    </div>
                    <div>
                      <div className="sample-label">Your Output</div>
                      <pre className="sample-code" style={{ color: selectedTc.status === 'ACCEPTED' ? 'var(--green)' : 'var(--red)' }}>
                        {selectedTc.actualOutput || '(empty)'}
                      </pre>
                    </div>
                    {selectedTc.errorOutput && (
                      <div>
                        <div className="sample-label">Error</div>
                        <pre className="sample-code" style={{ color: 'var(--red)' }}>{selectedTc.errorOutput}</pre>
                      </div>
                    )}
                  </div>
                ) : (
                  <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                    🔒 This is a hidden test case. Input/output is not shown.
                  </div>
                )}
              </div>
            )}

            {result.errorMessage && (
              <div className="card" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--red)', marginBottom: 8 }}>Compilation / Runtime Error</div>
                <pre className="font-mono" style={{ fontSize: 12, color: 'var(--red)', whiteSpace: 'pre-wrap' }}>
                  {result.errorMessage}
                </pre>
              </div>
            )}
          </div>
        )}

        {activeTab === 'submissions' && !result && (
          <div className="empty-state">
            <div className="empty-icon">📬</div>
            <div className="empty-title">No Submission Yet</div>
            <div className="empty-subtitle">Write your code and hit Submit to see results here.</div>
          </div>
        )}

        {activeTab === 'analysis' && result && (
          <AIAnalysisPanel aiFeedback={result.aiFeedback} complexityAnalysis={result.complexityAnalysis} />
        )}

        {activeTab === 'analysis' && !result && (
          <div className="empty-state">
            <div className="empty-icon">🤖</div>
            <div className="empty-title">No Analysis Yet</div>
            <div className="empty-subtitle">Submit your code to get AI-powered analysis.</div>
          </div>
        )}
      </div>

      {/* ── Right Panel: Code Editor ── */}
      <div className="editor-panel">
        <div className="editor-toolbar">
          <select
            className="select"
            value={language}
            onChange={(e) => handleLanguageChange(e.target.value)}
            style={{ minWidth: 140 }}
          >
            {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
          </select>
          <span style={{ fontSize: 12, color: 'var(--text-muted)', marginLeft: 'auto' }}>
            Monaco Editor
          </span>
        </div>

        <div className="editor-wrapper">
          <Editor
            height="calc(100vh - 64px - 52px - 60px)"
            language={LANGUAGES.find(l => l.value === language)?.monacoLang || 'java'}
            value={code}
            onChange={(val) => setCode(val || '')}
            theme="vs-dark"
            options={{
              fontSize: 14,
              fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
              fontLigatures: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              wordWrap: 'on',
              padding: { top: 16 },
              lineNumbersMinChars: 3,
              renderLineHighlight: 'gutter',
              smoothScrolling: true,
              cursorBlinking: 'phase',
              cursorSmoothCaretAnimation: 'on',
            }}
          />
        </div>

        <div className="editor-footer">
          <div style={{ display: 'flex', gap: 8, fontSize: 13, color: 'var(--text-secondary)' }}>
            {result && (
              <span className={`badge ${result.status === 'ACCEPTED' ? 'badge-accepted' : 'badge-wrong'}`}>
                {STATUS_META[result.status]?.label}
              </span>
            )}
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => setCode(STARTER_CODE[language])}
            >
              Reset
            </button>
            <button
              className={`btn btn-success ${submitting ? 'btn-ghost' : ''}`}
              onClick={handleSubmit}
              disabled={submitting}
              id="submit-btn"
            >
              {submitting ? (
                <><div className="spinner" style={{ width: 16, height: 16 }} /> Judging...</>
              ) : (
                '🚀 Submit'
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function AIAnalysisPanel({ aiFeedback, complexityAnalysis }) {
  if (!aiFeedback && !complexityAnalysis) return null;

  const timeComplex = complexityAnalysis?.estimatedTimeComplexity || 'Unknown';
  const spaceComplex = complexityAnalysis?.estimatedSpaceComplexity || 'Unknown';
  const quality = aiFeedback?.codeQualityScore || 0;
  const issues = aiFeedback?.codeQualityIssues || [];
  const antiPatterns = aiFeedback?.antiPatterns || [];
  const optimizations = aiFeedback?.optimizationSuggestions || [];
  const perfHints = aiFeedback?.performanceHints || [];

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 20 }}>
        <div className="section-title">Complexity Analysis</div>
        <div className="analysis-grid">
          <div className="complexity-badge">
            <div className="complexity-label">⏱ Time Complexity</div>
            <div className="complexity-value">{timeComplex}</div>
          </div>
          <div className="complexity-badge">
            <div className="complexity-label">💾 Space Complexity</div>
            <div className="complexity-value">{spaceComplex}</div>
          </div>
        </div>
      </div>

      {/* Code Quality Score */}
      <div style={{ marginBottom: 20 }}>
        <div className="section-title">Code Quality Score</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 20, padding: '16px 20px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)' }}>
          <QualityRing score={quality} />
          <div>
            <div style={{ fontSize: 28, fontWeight: 800, color: quality >= 70 ? 'var(--green)' : quality >= 40 ? 'var(--yellow)' : 'var(--red)' }}>
              {quality}/100
            </div>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
              {quality >= 70 ? 'Clean code ✨' : quality >= 40 ? 'Needs improvement 🔧' : 'Refactoring recommended ⚠️'}
            </div>
          </div>
        </div>
      </div>

      {perfHints.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <div className="section-title">Performance Hints</div>
          <div className="hints-list">
            {perfHints.map((h, i) => <div key={i} className="hint-item">{h}</div>)}
          </div>
        </div>
      )}

      {optimizations.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <div className="section-title">💡 Optimizations</div>
          <div className="hints-list">
            {optimizations.map((o, i) => <div key={i} className="hint-item">{o}</div>)}
          </div>
        </div>
      )}

      {issues.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <div className="section-title">⚠️ Code Quality Issues</div>
          <div className="hints-list">
            {issues.map((issue, i) => <div key={i} className="hint-item" style={{ borderLeftColor: 'var(--yellow)' }}>{issue}</div>)}
          </div>
        </div>
      )}

      {antiPatterns.length > 0 && (
        <div>
          <div className="section-title">🚫 Anti-Patterns Detected</div>
          <div className="hints-list">
            {antiPatterns.map((ap, i) => <div key={i} className="hint-item" style={{ borderLeftColor: 'var(--red)' }}>{ap}</div>)}
          </div>
        </div>
      )}
    </div>
  );
}

function QualityRing({ score }) {
  const r = 34, circ = 2 * Math.PI * r;
  const offset = circ - (score / 100) * circ;
  const color = score >= 70 ? '#10b981' : score >= 40 ? '#f59e0b' : '#ef4444';
  return (
    <div className="quality-ring">
      <svg width="80" height="80" viewBox="0 0 80 80">
        <circle cx="40" cy="40" r={r} stroke="var(--bg-hover)" strokeWidth="6" fill="none" />
        <circle cx="40" cy="40" r={r} stroke={color} strokeWidth="6" fill="none"
          strokeDasharray={circ} strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 1s ease' }}
          strokeLinecap="round" />
      </svg>
      <span className="quality-ring-value" style={{ color }}>{score}</span>
    </div>
  );
}

// Mock data
const MOCK_PROBLEM = {
  id: 1, title: 'Two Sum', difficulty: 'EASY', timeLimit: 1000, memoryLimit: 262144,
  description: 'Given an array of integers nums and an integer target, return the indices of the two numbers such that they add up to target.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nReturn the answer with the smaller index first.',
  inputFormat: 'First line: n (size of array)\nSecond line: n space-separated integers\nThird line: target integer',
  outputFormat: 'Two space-separated indices (0-indexed)',
  constraints: '2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9',
  tags: ['array', 'hash-map'], acceptanceRate: 65.2, totalSubmissions: 1240,
  sampleTestCases: [
    { id: 1, inputData: '4\n2 7 11 15\n9', expectedOutput: '0 1', isSample: true, explanation: '2 + 7 = 9' },
    { id: 2, inputData: '3\n3 2 4\n6', expectedOutput: '1 2', isSample: true, explanation: '2 + 4 = 6' },
  ],
  hints: ['Try using a hash map to store values you\'ve seen.', 'For each number, check if target - number exists in the map.'],
  starterCode: {},
};

const MOCK_RESULT = {
  status: 'WRONG_ANSWER', testCasesPassed: 2, totalTestCases: 6, execTime: 45, score: 33,
  testCaseResults: [
    { testCaseId: 1, status: 'ACCEPTED', actualOutput: '0 1', expectedOutput: '0 1', input: '4\n2 7 11 15\n9', isSample: true, execTime: 22 },
    { testCaseId: 2, status: 'ACCEPTED', actualOutput: '1 2', expectedOutput: '1 2', input: '3\n3 2 4\n6', isSample: true, execTime: 23 },
    { testCaseId: 3, status: 'WRONG_ANSWER', actualOutput: '0 1', expectedOutput: '0 1', isSample: false, execTime: 30 },
    { testCaseId: 4, status: 'WRONG_ANSWER', isSample: false, execTime: 28 },
    { testCaseId: 5, status: 'WRONG_ANSWER', isSample: false, execTime: 31 },
    { testCaseId: 6, status: 'WRONG_ANSWER', isSample: false, execTime: 29 },
  ],
  aiFeedback: {
    codeQualityScore: 68,
    codeQualityIssues: ['No comments found. Adding comments improves readability.'],
    antiPatterns: [],
    optimizationSuggestions: ['💡 Consider using a HashMap/Set for O(1) lookups instead of nested loops.'],
    performanceHints: ['❌ Check your logic carefully. Consider edge cases:', '   • Empty inputs or arrays of size 1', '   • Very large or very small numbers'],
  },
  complexityAnalysis: {
    estimatedTimeComplexity: 'O(n²)',
    estimatedSpaceComplexity: 'O(1) — Constant space',
  },
};
