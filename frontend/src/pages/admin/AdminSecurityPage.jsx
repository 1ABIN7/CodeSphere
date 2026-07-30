import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { securityAPI } from '../../api';

const CONTROLS = [
  { icon: '🔐', title: 'Authentication', text: 'JWT sessions, Google sign-in, password hashing, and token revocation are active.' },
  { icon: '🛡', title: 'Role-based access', text: 'Candidate, evaluator, and admin actions are protected by server-side roles.' },
  { icon: '⏱', title: 'Assessment integrity', text: 'Server-enforced deadlines, session controls, and proctoring event capture are available.' },
  { icon: '📁', title: 'File safety', text: 'Uploads are size-limited and validated before they are stored or reviewed.' },
];

export default function AdminSecurityPage() {
  const [logs, setLogs] = useState([]);
  const [proctoringEvents, setProctoringEvents] = useState([]);
  const [action, setAction] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async (nextAction = action) => {
    setLoading(true);
    try { const [audit, proctoring] = await Promise.all([securityAPI.auditLogs({ page: 0, size: 25, sortBy: 'timestamp', sortDir: 'desc', action: nextAction || undefined }), securityAPI.proctoringEvents()]); setLogs(audit.data.content ?? []); setProctoringEvents(proctoring.data ?? []); }
    catch (error) { toast.error(error.response?.data?.message || 'Unable to load security activity.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(''); }, []);
  const changeAction = (value) => { setAction(value); load(value); };

  return <div className="container fade-in admin-security-page">
    <section className="admin-security-hero"><div className="admin-security-shield">⌾</div><div><div className="admin-security-eyebrow"><span /> Platform protection</div><h1>Security center</h1><p>Review the safeguards protecting candidates, assessments, and organization activity.</p></div></section>
    <section className="admin-security-controls">{CONTROLS.map((control) => <article className="card admin-security-control" key={control.title}><div>{control.icon}</div><h2>{control.title}</h2><p>{control.text}</p><span><i /> Active</span></article>)}</section>
    <section className="card admin-security-audit"><header><div><h2>Security activity</h2><p>Recent authenticated actions recorded by the platform.</p></div><select className="select" value={action} onChange={(event) => changeAction(event.target.value)}><option value="">All activity</option><option value="LOGIN">Logins</option><option value="LOGOUT">Logouts</option><option value="ASSESSMENT_START">Assessment starts</option><option value="ASSESSMENT_SUBMIT">Assessment submissions</option><option value="QUESTION_APPROVED">Question approvals</option><option value="ROLE_UPDATED">Role changes</option></select></header>{loading ? <div className="loading-center"><div className="spinner" /></div> : logs.length === 0 ? <div className="empty-state"><div className="empty-icon">✓</div><div className="empty-title">No matching activity</div><div className="empty-subtitle">Security-relevant platform actions will appear here.</div></div> : <div className="admin-security-log-list">{logs.map((log) => <article className="admin-security-log" key={log.id}><div className="admin-security-log-icon">{actionIcon(log.action)}</div><div><strong>{label(log.action)}</strong><p>{log.username || 'System'} · {log.resource || log.resourceType || 'Platform activity'}</p>{log.ipAddress && <small>IP address: {log.ipAddress}</small>}</div><time>{log.timestamp ? new Date(log.timestamp).toLocaleString() : '—'}</time></article>)}</div>}</section>
    <section className="card admin-security-proctoring"><header><div><h2>Assessment monitoring</h2><p>Recent tab switches, focus loss, and fullscreen exits recorded during assessments.</p></div><span className="badge badge-default">{proctoringEvents.length} events</span></header>{loading ? <div className="loading-center"><div className="spinner" /></div> : proctoringEvents.length === 0 ? <div className="empty-state"><div className="empty-icon">◷</div><div className="empty-title">No proctoring events yet</div><div className="empty-subtitle">Integrity signals will appear when candidates leave an active assessment.</div></div> : <div className="admin-security-log-list">{proctoringEvents.map((event) => <article className="admin-security-log" key={event.id}><div className="admin-security-log-icon">{eventIcon(event.eventType)}</div><div><strong>{label(event.eventType)}</strong><p>{event.candidateName} · Assessment #{event.assessmentId} · <span className={`admin-security-severity ${String(event.severity).toLowerCase()}`}>{event.severity}</span></p></div><time>{event.timestamp ? new Date(event.timestamp).toLocaleString() : '—'}</time></article>)}</div>}</section>
  </div>;
}

function label(value) { return String(value || 'Activity').replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()); }
function actionIcon(value) { if (value === 'LOGIN') return '→'; if (value === 'LOGOUT') return '←'; if (String(value).includes('ASSESSMENT')) return '◷'; if (String(value).includes('ROLE')) return '♙'; return '•'; }
function eventIcon(value) { if (value === 'TAB_SWITCH') return '↗'; if (value === 'FULLSCREEN_EXIT') return '⛶'; return '◉'; }
