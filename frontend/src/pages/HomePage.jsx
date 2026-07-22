import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { motion } from 'framer-motion';
import { 
  Code2, ListChecks, FileText, BookOpen, UploadCloud, Layers,
  Settings, Users, PlayCircle, BarChart3, ShieldCheck, Box
} from 'lucide-react';
import heroImg from '../assets/hero.png';

const ASSESSMENT_TYPES = [
  { icon: <Code2 size={24} />, title: 'Coding', desc: 'Auto-graded programming problems with test cases.' },
  { icon: <ListChecks size={24} />, title: 'MCQ', desc: 'Single and multiple choice questions.' },
  { icon: <FileText size={24} />, title: 'Written', desc: 'Subjective questions with manual rubric grading.' },
  { icon: <BookOpen size={24} />, title: 'Reading Comprehension', desc: 'Passage-based sub-questions.' },
  { icon: <UploadCloud size={24} />, title: 'File Upload', desc: 'Submit files for design or architecture tasks.' },
  { icon: <Layers size={24} />, title: 'Mixed / Sectioned', desc: 'Combine multiple types into timed sections.' },
];

const STEPS = [
  { icon: <Settings size={32} />, title: '1. Create Assessment', desc: 'Admins build exams by selecting questions from the bank and setting rules.' },
  { icon: <Users size={32} />, title: '2. Assign Candidates', desc: 'Invite candidates and set specific deadlines and time limits.' },
  { icon: <PlayCircle size={32} />, title: '3. Candidates Take Exam', desc: 'A secure, proctored environment with an advanced IDE and auto-save.' },
  { icon: <BarChart3 size={32} />, title: '4. Evaluation & Reports', desc: 'Auto-grading combined with manual rubric scoring, generating detailed reports.' },
];

const STATS = [
  { icon: <Code2 size={24} className="text-accent" />, title: '8+ Languages', desc: 'Java, Python, C++, and more' },
  { icon: <Box size={24} className="text-accent" />, title: 'Docker-Sandboxed', desc: 'Secure code execution' },
  { icon: <ShieldCheck size={24} className="text-accent" />, title: 'Real-time Proctoring', desc: 'Tab-switch & behavior tracking' },
const FEATURES = [
  { icon: '⚡', title: 'AI-Powered Judge', desc: 'Our judge engine analyses your code in real-time — complexity, patterns, quality score, and smart feedback.' },
  { icon: '🧠', title: 'Smart Analysis', desc: 'Get time & space complexity estimates, anti-pattern detection, and personalised optimisation hints.' },
  { icon: '📝', title: 'Interview Prep', desc: 'Master technical, aptitude, logical, and HR questions with curated mock tests and performance tracking.' },
  { icon: '📂', title: 'File Storage', desc: 'Upload editorial PDFs, test data, and attachments. Local and MinIO S3 backends supported out of the box.' },
  { icon: '🏆', title: '25 Curated Problems', desc: 'From Two Sum to N-Queens — 8 easy, 10 medium, 7 hard problems covering every major pattern.' },
  { icon: '🔒', title: 'Role-Based Access', desc: 'Multi-tenant platform with Super Admin, Org Admin, Examiner, Instructor, and Candidate roles.' },
];

const STATS = [
  { value: '25', label: 'Coding Problems' },
  { value: '100+', label: 'Interview Questions' },
  { value: '6', label: 'Interview Categories' },
  { value: '∞', label: 'AI Insights' },
];

export default function HomePage() {
  const { isLoggedIn, isAdmin } = useAuth();

  return (
    <div style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* ── Hero ── */}
      <section className="hero" style={{ position: 'relative', minHeight: '85vh', paddingBottom: '40px' }}>
        <div style={{
          position: 'absolute', top: '-10%', left: '50%', transform: 'translateX(-50%)',
          width: '1000px', height: '1000px',
          background: 'radial-gradient(circle, rgba(124, 58, 237, 0.15) 0%, transparent 60%)',
          zIndex: 0, pointerEvents: 'none'
        }} />
        
        <div className="container" style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <motion.div 
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}
          >
            <motion.div 
              className="hero-badge"
              whileHover={{ scale: 1.05, boxShadow: '0 0 20px rgba(124,58,237,0.4)' }}
              style={{ cursor: 'pointer', background: 'rgba(20,20,35,0.6)', backdropFilter: 'blur(10px)' }}
            >
              ✨ The All-in-One Assessment Platform
            </motion.div>
            
            <h1 className="hero-title" style={{ maxWidth: '900px', textShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
              Evaluate Talent with<br />
              <span className="hero-gradient" style={{ background: 'linear-gradient(135deg, #a78bfa, #3b82f6)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Precision & Confidence
              </span>
            </h1>
            
            <p className="hero-subtitle" style={{ fontSize: '20px', lineHeight: '1.6', color: '#a0a0c0', maxWidth: '700px' }}>
              CodeSphere empowers teams to conduct coding tests, MCQs, and subjective exams in a secure, Docker-sandboxed environment with real-time proctoring.
            </p>
            
            <div className="hero-actions" style={{ gap: '20px', marginTop: '10px' }}>
              {!isLoggedIn ? (
                <>
                  <Link to="/register" className="btn btn-primary btn-lg" style={{ padding: '16px 32px', fontSize: '18px', borderRadius: '12px' }}>
                    Get Started
                  </Link>
                  <Link to="/login" className="btn btn-secondary btn-lg" style={{ padding: '16px 32px', fontSize: '18px', borderRadius: '12px' }}>
                    Login
                  </Link>
                </>
              ) : (
                <Link to={isAdmin ? "/admin" : "/assessments"} className="btn btn-primary btn-lg" style={{ padding: '16px 32px', fontSize: '18px', borderRadius: '12px' }}>
                  Go to Dashboard
                </Link>
              )}
            </div>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, y: 80 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 1, delay: 0.2, type: 'spring', damping: 20 }}
            style={{ marginTop: '60px', width: '100%', maxWidth: '1000px', borderRadius: '16px', overflow: 'hidden', border: '1px solid var(--border-bright)', boxShadow: '0 20px 60px rgba(0,0,0,0.5)' }}
          >
            <img src={heroImg} alt="CodeSphere Interface" style={{ width: '100%', height: 'auto', display: 'block' }} />
          </motion.div>
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
          <Link to="/interview" className="btn btn-secondary btn-lg border border-indigo-500/30 hover:border-indigo-500 bg-indigo-500/10 text-indigo-400">
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
      </section>

      {/* ── Trust Strip ── */}
      <section style={{ borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)', background: 'var(--bg-secondary)', padding: '40px 0' }}>
        <div className="container" style={{ display: 'flex', justifyContent: 'space-around', flexWrap: 'wrap', gap: '30px' }}>
          {STATS.map((stat, i) => (
            <motion.div 
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              style={{ display: 'flex', alignItems: 'center', gap: '16px' }}
            >
              <div style={{ background: 'rgba(124, 58, 237, 0.1)', padding: '12px', borderRadius: '12px' }}>
                {stat.icon}
              </div>
              <div>
                <h4 style={{ fontSize: '18px', fontWeight: 700, color: '#fff' }}>{stat.title}</h4>
                <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>{stat.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ── Assessment Types ── */}
      <section style={{ padding: '120px 0', position: 'relative' }}>
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            style={{ textAlign: 'center', marginBottom: '80px' }}
          >
            <h2 style={{ fontSize: '40px', fontWeight: 800, marginBottom: '16px' }}>
              Any format you need
            </h2>
            <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto', fontSize: '18px' }}>
              Mix and match question types to build the perfect evaluation for any role.
            </p>
          </motion.div>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
            {ASSESSMENT_TYPES.map((type, i) => (
              <motion.div 
                key={i}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
                className="card"
                style={{ padding: '32px', display: 'flex', flexDirection: 'column', gap: '16px' }}
              >
                <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(124, 58, 237, 0.1)', color: 'var(--accent-light)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {type.icon}
                </div>
                <h3 style={{ fontSize: '20px', fontWeight: 600 }}>{type.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '15px' }}>{type.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ── How It Works ── */}
      <section style={{ padding: '120px 0', background: 'var(--bg-secondary)', borderTop: '1px solid var(--border)' }}>
        <div className="container">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            style={{ textAlign: 'center', marginBottom: '80px' }}
          >
            <h2 style={{ fontSize: '40px', fontWeight: 800, marginBottom: '16px' }}>
              How it works
            </h2>
            <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto', fontSize: '18px' }}>
              A seamless workflow from creation to evaluation.
            </p>
          </motion.div>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '40px' }}>
            {STEPS.map((step, i) => (
              <motion.div 
                key={i}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}
              >
                <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'var(--bg-card)', border: '1px solid var(--accent)', color: 'var(--accent-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 0 20px rgba(124, 58, 237, 0.2)' }}>
                  {step.icon}
                </div>
                <h3 style={{ fontSize: '20px', fontWeight: 700 }}>{step.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '15px', lineHeight: '1.6' }}>{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer style={{ marginTop: 'auto', borderTop: '1px solid var(--border)', background: 'var(--bg-card)', padding: '60px 0 30px' }}>
        <div className="container" style={{ display: 'flex', flexDirection: 'column', gap: '40px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '40px' }}>
            <div>
              <div style={{ fontSize: '20px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                <span style={{ color: 'var(--accent)' }}>⚡</span> CodeSphere
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '14px', lineHeight: '1.6' }}>
                The next-generation assessment platform.
              </p>
            </div>
            <div>
              <h4 style={{ fontWeight: 600, marginBottom: '16px' }}>Platform</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <Link to="/problems" style={{ color: 'var(--text-secondary)', fontSize: '14px', textDecoration: 'none' }}>Practice</Link>
                <Link to="/assessments" style={{ color: 'var(--text-secondary)', fontSize: '14px', textDecoration: 'none' }}>Assessments</Link>
              </div>
            </div>
            <div>
              <h4 style={{ fontWeight: 600, marginBottom: '16px' }}>Legal</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <span style={{ color: 'var(--text-secondary)', fontSize: '14px', cursor: 'pointer' }}>Privacy Policy</span>
                <span style={{ color: 'var(--text-secondary)', fontSize: '14px', cursor: 'pointer' }}>Terms of Service</span>
              </div>
            </div>
          </div>
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '24px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
            &copy; {new Date().getFullYear()} CodeSphere. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
