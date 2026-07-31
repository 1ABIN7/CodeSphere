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
  const { isLoggedIn, isAdmin } = useAuth();
  // Stats derived from backend config (5 languages supported)
  const stats = [
    { value: '5', label: 'Languages Supported' },
    { value: '6', label: 'Assessment Types' },
    { value: 'Docker‑Sandboxed', label: 'Secure Execution' },
  ];

  return (
      <motion.div className="fade-in home-page" initial="hidden" animate="visible" variants={fadeUp}>
        <section className="home-hero">
          <div className="container home-hero-grid">
            <motion.div className="home-hero-copy" variants={fadeUp}>
              <div className="home-kicker"><span>⚡</span> Assessment and practice, together</div>
              <h1>Build skills.<br /><span>Measure what matters.</span></h1>
              <p>CodeSphere gives candidates a focused place to practice and gives teams the tools to create, deliver, and review assessments.</p>
              <div className="home-hero-actions">
                <Link to={isLoggedIn ? (isAdmin ? '/admin' : '/dashboard') : '/register'} className="btn btn-primary btn-lg">
                  {isLoggedIn ? (isAdmin ? 'Open admin dashboard' : 'Open my dashboard') : 'Get started'}
                </Link>
                <Link to="/problems" className="btn btn-secondary btn-lg">Explore problem bank</Link>
              </div>
              <div className="home-trust-row">
                <span>✓ Coding and written assessments</span><span>✓ Secure judging</span><span>✓ Clear candidate results</span>
              </div>
            </motion.div>
            <motion.div className="home-product-preview" variants={heroVariants}>
              <div className="home-preview-topbar"><span /><span /><span /><strong>CodeSphere workspace</strong></div>
              <div className="home-preview-content">
                <div className="home-preview-copy"><small>YOUR PROGRESS</small><strong>Keep moving forward.</strong><span>Practice, assessments, and results in one calm workspace.</span></div>
                <img src={heroImg} alt="CodeSphere assessment workspace" />
              </div>
              <div className="home-preview-metrics">
                <div><strong>125+</strong><span>Practice problems</span></div>
                <div><strong>6</strong><span>Assessment types</span></div>
                <div><strong>5</strong><span>Languages supported</span></div>
              </div>
            </motion.div>
          </div>
        </section>

        <section className="section home-section">
          <div className="container">
            <div className="home-section-heading"><div><span>ONE PLATFORM, MANY WAYS TO EVALUATE</span><h2>Everything you need for a meaningful assessment.</h2></div><p>Mix coding, knowledge checks, writing, reading, and file review without sending candidates across multiple tools.</p></div>
            <div className="assessment-grid home-feature-grid">
              {assessmentTypes.map((a) => (
                  <motion.div className="card home-feature-card" key={a.title} variants={fadeUp} whileHover={{ y: -4 }}>
                    <div className="home-feature-icon">
                      {a.icon}
                    </div>
                    <h3>{a.title}</h3>
                    <p>{a.desc}</p>
                  </motion.div>
              ))}
            </div>
          </div>
        </section>

        <section className="section home-workflow-section">
          <div className="container">
            <div className="home-section-heading centered"><div><span>HOW IT WORKS</span><h2>From setup to results in four simple steps.</h2></div></div>
            <div className="how-it-works-grid home-workflow-grid">
              {howItWorks.map((h) => (
                  <motion.div className="home-workflow-card" key={h.step} variants={fadeUp}>
                    <div className="step-number">{h.step}</div>
                    <div className="home-workflow-icon">{h.icon}</div>
                    <h3>{h.title}</h3>
                    <p>{h.desc}</p>
                  </motion.div>
              ))}
            </div>
          </div>
        </section>

        <section className="section home-cta-section">
          <div className="container center-text">
            <div className="home-cta-card">
              <div><span>READY WHEN YOU ARE</span><h2>Start practicing with a problem that fits your level.</h2><p>Explore the CodeSphere problem bank, save your progress, and grow from the next challenge.</p></div>
              <Link to="/problems" className="btn btn-primary btn-lg">Browse practice problems</Link>
            </div>
          </div>
        </section>

        <section className="home-stats-strip">
          <div className="container flex-center">
            {stats.map((s) => (
                <div key={s.label} className="stat-item">
                  <div className="stat-value">{s.value}</div>
                  <div className="stat-label">{s.label}</div>
                </div>
            ))}
          </div>
        </section>

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
