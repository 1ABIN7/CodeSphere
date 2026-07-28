import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { motion } from 'framer-motion';
import heroImg from '../assets/hero.png';
import { Code, ListCheck, FileEdit, BookOpen, Upload, Layers, Settings, Users, CheckCircle, Trophy } from 'lucide-react';

// Motion variants respecting prefers-reduced-motion
const fadeUp = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const heroVariants = {
  hidden: { opacity: 0, scale: 0.95 },
  visible: { opacity: 1, scale: 1, transition: { duration: 0.6 } },
};

const assessmentTypes = [
  { icon: <Code size={24} />, title: 'Coding', desc: 'Write code, run test cases, get instant feedback.' },
  { icon: <ListCheck size={24} />, title: 'MCQ', desc: 'Multiple‑choice questions with instant scoring.' },
  { icon: <FileEdit size={24} />, title: 'Written', desc: 'Subjective answers evaluated by AI & reviewers.' },
  { icon: <BookOpen size={24} />, title: 'Reading Comprehension', desc: 'Passages with questions to assess understanding.' },
  { icon: <Upload size={24} />, title: 'File Upload', desc: 'Submit PDFs, images, or code archives for evaluation.' },
  { icon: <Layers size={24} />, title: 'Mixed/Sectioned', desc: 'Combine any of the above in one exam.' },
];

const howItWorks = [
  { icon: <Settings size={24} />, step: '1', title: 'Create Assessment', desc: 'Admin defines sections, questions, and time limits.' },
  { icon: <Users size={24} />, step: '2', title: 'Assign to Candidates', desc: 'Candidates receive a unique link or are auto‑assigned.' },
  { icon: <CheckCircle size={24} />, step: '3', title: 'Take Exam', desc: 'Candidates answer, code, and upload files in a secure UI.' },
  { icon: <Trophy size={24} />, step: '4', title: 'Get Results', desc: 'Automatic grading + manual review, certificates issued.' },
];

export default function HomePage() {
  const { isLoggedIn } = useAuth();
  // Stats derived from backend config (5 languages supported)
  const stats = [
    { value: '5', label: 'Languages Supported' },
    { value: '6', label: 'Assessment Types' },
    { value: 'Docker‑Sandboxed', label: 'Secure Execution' },
  ];

  return (
      <motion.div className="fade-in" initial="hidden" animate="visible" variants={fadeUp}>
        {/* ── Hero ── */}
        <section className="hero">
          <div className="hero-badge">✨ Production‑Ready Coding Platform</div>
          <h1 className="hero-title">
            Code Smarter with<br />
            <span className="hero-gradient">AI‑Powered Judging</span>
          </h1>
          <p className="hero-subtitle">
            The all‑in‑one assessment suite for coding tests, MCQs, written exams, and more.
          </p>
          <div className="hero-actions">
            <Link to="/problems" className="btn btn-primary btn-lg">
              🚀 Explore Problems
            </Link>
            <Link to="/interview" className="btn btn-secondary btn-lg">
              🎯 Interview Prep
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
          <img src={heroImg} alt="CodeSphere hero" className="hero-image" />
          <div className="hero-stats">
            {stats.map((s) => (
                <div key={s.label} className="stat-item">
                  <div className="stat-value">{s.value}</div>
                  <div className="stat-label">{s.label}</div>
                </div>
            ))}
          </div>
        </section>

        {/* ── Assessment Types ── */}
        <section className="section">
          <div className="container">
            <h2 className="section-title">Assessment Types</h2>
            <div className="assessment-grid">
              {assessmentTypes.map((a) => (
                  <motion.div className="card" key={a.title} variants={fadeUp} whileHover={{ y: -4 }}>
                    <div className="card-header">
                      {a.icon}
                      <h3 className="card-title">{a.title}</h3>
                    </div>
                    <p>{a.desc}</p>
                  </motion.div>
              ))}
            </div>
          </div>
        </section>

        {/* ── How It Works ── */}
        <section className="section bg-alt">
          <div className="container">
            <h2 className="section-title">How It Works</h2>
            <div className="how-it-works-grid">
              {howItWorks.map((h) => (
                  <motion.div className="card" key={h.step} variants={fadeUp}>
                    <div className="step-number">{h.step}</div>
                    {h.icon}
                    <h3 className="card-title">{h.title}</h3>
                    <p>{h.desc}</p>
                  </motion.div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Practice Hub Teaser ── */}
        <section className="section">
          <div className="container center-text">
            <h2 className="section-title">Practice Hub</h2>
            <p className="section-subtitle">
              Sharpen your skills with a curated set of coding problems and interview prep resources.
            </p>
            <Link to="/problems" className="btn btn-primary btn-lg">
              Browse Practice Problems
            </Link>
          </div>
        </section>

        {/* ── Trust / Stats Strip ── */}
        <section className="section dark-strip">
          <div className="container flex-center">
            {stats.map((s) => (
                <div key={s.label} className="stat-item">
                  <div className="stat-value">{s.value}</div>
                  <div className="stat-label">{s.label}</div>
                </div>
            ))}
          </div>
        </section>

        {/* ── Footer ── */}
        <footer className="footer">
          <div className="container flex-between">
            <div className="footer-brand">
              <div className="brand-icon">⚡</div>
              <span>CodeSphere</span>
            </div>
            <div className="footer-links">
              <Link to="/login" className="footer-link">Login</Link>
              <Link to="/register" className="footer-link">Register</Link>
              <Link to="/problems" className="footer-link">Practice</Link>
            </div>
          </div>
        </footer>
      </motion.div>
  );
}