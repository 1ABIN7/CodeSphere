import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { interviewAPI } from '../api';
import { toast } from 'react-hot-toast';

export default function InterviewPerformancePage() {
  const location = useLocation();
  const navigate = useNavigate();
  const recentResult = location.state?.result;
  
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPerformance();
  }, []);

  const fetchPerformance = async () => {
    try {
      const { data } = await interviewAPI.getPerformanceSummary();
      setSummary(data);
    } catch (err) {
      toast.error('Failed to load performance data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: 'calc(100vh - 64px)' }}>
        <div className="spinner" />
      </div>
    );
  }

  return (
    <div className="container fade-in" style={{ paddingTop: '40px', paddingBottom: '60px' }}>
      <div className="perf-header">
        <div>
          <h1 className="perf-title">
            <span className="hero-gradient">Your Performance</span>
          </h1>
          <p className="perf-subtitle">Track your progress across all interview categories.</p>
        </div>
        <button
          onClick={() => navigate('/interview')}
          className="btn btn-secondary"
        >
          ← Back to Practice
        </button>
      </div>

      {recentResult && (
        <div className="recent-result slide-up">
          <h2 style={{ fontSize: '20px', fontWeight: '800', color: 'var(--accent-light)', marginBottom: '12px' }}>
            Session Completed! 🎉
          </h2>
          <div style={{ display: 'flex', gap: '32px', fontSize: '15px' }}>
            <div>
              <span style={{ color: 'var(--text-secondary)' }}>Score: </span>
              <span className="font-mono" style={{ fontSize: '18px', fontWeight: '700', color: 'var(--text-primary)' }}>
                {recentResult.score}/{recentResult.maxScore}
              </span>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)' }}>Accuracy: </span>
              <span className="font-mono" style={{ fontSize: '18px', fontWeight: '700', color: 'var(--green)' }}>
                {recentResult.totalQuestions ? Math.round((recentResult.correctAnswers / recentResult.totalQuestions) * 100) : 0}%
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Global Stats */}
      <div className="dashboard-grid">
        <div className="card stat-card flex-col items-center" style={{ textAlign: 'center' }}>
          <span className="font-mono" style={{ fontSize: '48px', fontWeight: '900', color: 'var(--accent-light)', lineHeight: 1.2 }}>
            {summary?.totalSessionsCompleted || 0}
          </span>
          <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px' }}>
            Sessions Completed
          </span>
        </div>
        <div className="card stat-card flex-col items-center" style={{ textAlign: 'center' }}>
          <span className="font-mono" style={{ fontSize: '48px', fontWeight: '900', color: '#a78bfa', lineHeight: 1.2 }}>
            {summary?.totalQuestionsAttempted || 0}
          </span>
          <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px' }}>
            Questions Attempted
          </span>
        </div>
        <div className="card stat-card flex-col items-center" style={{ textAlign: 'center' }}>
          <span className="font-mono" style={{ fontSize: '48px', fontWeight: '900', color: 'var(--green)', lineHeight: 1.2 }}>
            {summary?.overallAccuracy ? summary.overallAccuracy.toFixed(1) : '0.0'}%
          </span>
          <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px' }}>
            Overall Accuracy
          </span>
        </div>
      </div>

      {/* Category Breakdown */}
      <h2 style={{ fontSize: '24px', fontWeight: '700', marginBottom: '24px' }}>Category Breakdown</h2>
      
      <div className="dashboard-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))' }}>
        {summary?.categoryStats?.map((stat, i) => (
          <div 
            key={stat.categoryId}
            className="card slide-up"
            style={{ animationDelay: `${i * 0.1}s`, animationFillMode: 'both' }}
          >
            <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '20px' }}>
              {stat.categoryDisplayName}
            </h3>
            
            <div className="skill-bar-container">
              <div className="skill-bar-item">
                <div className="skill-bar-header">
                  <span style={{ color: 'var(--text-secondary)' }}>Accuracy</span>
                  <span className="font-mono text-accent">{stat.accuracy ? stat.accuracy.toFixed(1) : 0}%</span>
                </div>
                <div className="skill-bar-track">
                  <div className="skill-bar-fill" style={{ width: `${stat.accuracy || 0}%` }} />
                </div>
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '16px', paddingTop: '16px', borderTop: '1px solid var(--border)' }}>
                <div>
                  <div style={{ fontSize: '11px', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '4px', fontWeight: '600' }}>Attempted</div>
                  <div className="font-mono" style={{ fontSize: '18px', color: 'var(--text-primary)', fontWeight: '600' }}>{stat.totalAttempted}</div>
                </div>
                <div>
                  <div style={{ fontSize: '11px', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '4px', fontWeight: '600' }}>Best Score</div>
                  <div className="font-mono" style={{ fontSize: '18px', color: 'var(--green)', fontWeight: '600' }}>{stat.bestScore}</div>
                </div>
              </div>
            </div>
          </div>
        ))}

        {(!summary?.categoryStats || summary.categoryStats.length === 0) && (
          <div style={{ 
            gridColumn: '1 / -1', padding: '60px', textAlign: 'center', 
            border: '1px dashed var(--border)', borderRadius: 'var(--radius-lg)', color: 'var(--text-muted)' 
          }}>
            No performance data yet. Start practicing!
          </div>
        )}
      </div>
    </div>
  );
}
