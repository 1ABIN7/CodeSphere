import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const FEATURES = [
  { icon: '⚡', title: 'AI-Powered Judge', desc: 'Our judge engine analyses your code in real-time — complexity, patterns, quality score, and smart feedback.' },
  { icon: '🧠', title: 'Smart Analysis', desc: 'Get time & space complexity estimates, anti-pattern detection, and personalised optimisation hints.' },
  { icon: '📂', title: 'File Storage', desc: 'Upload editorial PDFs, test data, and attachments. Local and MinIO S3 backends supported out of the box.' },
  { icon: '🏆', title: '25 Curated Problems', desc: 'From Two Sum to N-Queens — 8 easy, 10 medium, 7 hard problems covering every major pattern.' },
  { icon: '📊', title: 'Skill Tracking', desc: 'Every accepted submission updates your skill proficiency score across tags and difficulty levels.' },
  { icon: '🔒', title: 'Role-Based Access', desc: 'Multi-tenant platform with Super Admin, Org Admin, Examiner, Instructor, and Candidate roles.' },
];

const STATS = [
  { value: '25', label: 'Coding Problems' },
  { value: '5', label: 'Languages Supported' },
  { value: '100+', label: 'Test Cases' },
  { value: '∞', label: 'AI Insights' },
];

export default function HomePage() {
  const { isLoggedIn } = useAuth();

  return (
    <div className="fade-in">
      {/* ── Hero ── */}
      <section className="hero">
        <div className="hero-badge">
          ✨ Production-Ready Coding Platform
        </div>
        <h1 className="hero-title">
          Code Smarter with<br />
          <span className="hero-gradient">AI-Powered Judging</span>
        </h1>
        <p className="hero-subtitle">
          Practice with 25 curated problems, get instant AI feedback on your code quality,
          time complexity, and submit in 5 languages.
        </p>
        <div className="hero-actions">
          <Link to="/problems" className="btn btn-primary btn-lg">
            🚀 Explore Problems
          </Link>
          {!isLoggedIn && (
            <Link to="/register" className="btn btn-secondary btn-lg">
              Get Started Free
            </Link>
          )}
          {isLoggedIn && (
            <Link to="/dashboard" className="btn btn-secondary btn-lg">
              📊 My Dashboard
            </Link>
          )}
        </div>

        <div className="hero-stats">
          {STATS.map(s => (
            <div key={s.label} className="stat-item">
              <div className="stat-value">{s.value}</div>
              <div className="stat-label">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features ── */}
      <section style={{ padding: '80px 0', background: 'var(--bg-secondary)' }}>
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: '48px' }}>
            <h2 style={{ fontSize: '36px', fontWeight: 800, letterSpacing: '-1px', marginBottom: '12px' }}>
              Everything you need to <span className="text-accent">level up</span>
            </h2>
            <p style={{ color: 'var(--text-secondary)', maxWidth: '480px', margin: '0 auto' }}>
              A corporate-grade platform for coding practice, technical interviews, and skill assessment.
            </p>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
            {FEATURES.map(f => (
              <div key={f.title} className="card slide-up" style={{ padding: '28px' }}>
                <div style={{ fontSize: '32px', marginBottom: '14px' }}>{f.icon}</div>
                <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '8px' }}>{f.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '14px', lineHeight: '1.7' }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section style={{ padding: '80px 0', textAlign: 'center' }}>
        <div className="container">
          <h2 style={{ fontSize: '32px', fontWeight: 800, marginBottom: '16px', letterSpacing: '-1px' }}>
            Ready to start coding?
          </h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '32px' }}>
            Browse 25 problems or sign up to track your progress.
          </p>
          <div style={{ display: 'flex', gap: '14px', justifyContent: 'center' }}>
            <Link to="/problems" className="btn btn-primary btn-lg">Browse Problems</Link>
            {!isLoggedIn && <Link to="/register" className="btn btn-secondary btn-lg">Create Account</Link>}
          </div>
        </div>
      </section>
    </div>
  );
}
