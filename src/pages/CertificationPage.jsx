import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';
import {
  Award, Calendar, ExternalLink, Shield,
  Hash, Building2, BarChart3,
} from 'lucide-react';

const MOCK_CERTIFICATIONS = [
  {
    id: 1,
    title: 'JavaScript Fundamentals',
    issuer: 'CodeSphere',
    date: '2026-06-20',
    score: 88,
    code: 'JSC-2026-0821',
    isValid: true,
  },
  {
    id: 2,
    title: 'React Developer',
    issuer: 'CodeSphere',
    date: '2026-05-15',
    score: 92,
    code: 'RDA-2026-0334',
    isValid: true,
  },
  {
    id: 3,
    title: 'SQL Proficiency',
    issuer: 'CodeSphere',
    date: '2026-03-22',
    score: 79,
    code: 'SPT-2026-0112',
    isValid: true,
  },
];

const cardAnim = { hidden: { opacity: 0, y: 16 }, show: { opacity: 1, y: 0 } };

export default function CertificationPage() {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [certifications, setCertifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    const fetchCerts = async () => {
      setLoading(true);
      try {
        // TODO: backend endpoint pending — see CertificationController
        // const res = await certificationsAPI.getMy();
        // setCertifications(res.data?.content || res.data || []);
        throw new Error('endpoint pending');
      } catch {
        setCertifications(MOCK_CERTIFICATIONS);
      } finally {
        setLoading(false);
      }
    };
    fetchCerts();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleVerify = (code) => {
    toast.success(`Verification link copied for code: ${code}`);
  };

  if (loading) {
    return (
      <div className="loading-center" style={{ minHeight: '60vh' }}>
        <div className="spinner" />
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <div className="page-header">
        <h1 className="page-title">My Certifications</h1>
        <p className="page-subtitle">View and verify your earned certifications</p>
      </div>

      {certifications.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-icon">🏅</div>
            <div className="empty-title">No Certifications Yet</div>
            <div className="empty-subtitle">
              Complete and pass assessments to earn certifications
            </div>
          </div>
        </div>
      ) : (
        <motion.div
          initial="hidden"
          animate="show"
          variants={{ show: { transition: { staggerChildren: 0.1 } } }}
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
            gap: 20,
          }}
        >
          {certifications.map(cert => (
            <motion.div key={cert.id} className="card" variants={cardAnim} style={{ display: 'flex', flexDirection: 'column' }}>
              {/* Card top */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14, marginBottom: 16 }}>
                <div style={{
                  width: 52, height: 52, borderRadius: 12,
                  background: cert.isValid ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}>
                  <Award size={26} style={{ color: cert.isValid ? 'var(--green)' : 'var(--red)' }} />
                </div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 4 }}>{cert.title}</h3>
                  <div style={{ fontSize: 13, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Building2 size={13} /> {cert.issuer}
                  </div>
                </div>
                <span
                  className={`badge ${cert.isValid ? 'badge-accepted' : 'badge-wrong'}`}
                  style={{ flexShrink: 0 }}
                >
                  {cert.isValid ? 'Valid' : 'Invalid'}
                </span>
              </div>

              {/* Details */}
              <div style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12,
                marginBottom: 16, padding: '12px 0',
                borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)',
              }}>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 4 }}>
                    Date Issued
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Calendar size={13} style={{ color: 'var(--accent-light)' }} /> {cert.date}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 4 }}>
                    Score
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <BarChart3 size={13} style={{ color: cert.score >= 80 ? 'var(--green)' : 'var(--yellow)' }} />
                    <span style={{ color: cert.score >= 80 ? 'var(--green)' : 'var(--yellow)' }}>{cert.score}%</span>
                  </div>
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 4 }}>
                    Verification Code
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, fontFamily: 'monospace', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Hash size={13} style={{ color: 'var(--accent-light)' }} /> {cert.code}
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: cert.isValid ? 'var(--green)' : 'var(--red)' }}>
                  <Shield size={14} />
                  {cert.isValid ? 'Certificate is valid' : 'Certificate revoked'}
                </div>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => handleVerify(cert.code)}
                >
                  <ExternalLink size={14} /> Verify
                </button>
              </div>
            </motion.div>
          ))}
        </motion.div>
      )}
    </div>
  );
}
