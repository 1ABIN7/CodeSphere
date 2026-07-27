import { useState } from 'react';
import { CheckCircle, XCircle, Clock, Eye } from 'lucide-react';
import toast from 'react-hot-toast';

export default function QuestionApprovalPage() {
  const [questions] = useState([
    { id: 1, title: 'Sample pending question', type: 'MCQ', difficulty: 'Medium', submittedBy: 'instructor@demo.com', submittedAt: '2026-07-18' },
  ]);

  const handleApprove = (_id) => {
    // TODO: backend endpoint pending — see QuestionBankController (approve)
    toast.error('Approval endpoint not yet available');
  };

  const handleReject = (_id) => {
    // TODO: backend endpoint pending — see QuestionBankController (reject)
    toast.error('Rejection endpoint not yet available');
  };

  return (
    <div className="fade-in">
      <h1 style={{ fontSize: '24px', fontWeight: 800, marginBottom: 8 }}>Question Approval</h1>
      <p className="text-secondary" style={{ marginBottom: 24 }}>Review and approve questions submitted by instructors</p>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)' }}>
              <th style={{ textAlign: 'left', padding: '12px 16px', fontSize: 12, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Title</th>
              <th style={{ textAlign: 'left', padding: '12px 16px', fontSize: 12, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Type</th>
              <th style={{ textAlign: 'left', padding: '12px 16px', fontSize: 12, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Submitted By</th>
              <th style={{ textAlign: 'left', padding: '12px 16px', fontSize: 12, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Status</th>
              <th style={{ textAlign: 'right', padding: '12px 16px', fontSize: 12, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {questions.map((q) => (
              <tr key={q.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '12px 16px' }}>{q.title}</td>
                <td style={{ padding: '12px 16px' }}>
                  <span className="badge badge-info">{q.type}</span>
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{q.submittedBy}</td>
                <td style={{ padding: '12px 16px' }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color: 'var(--accent)', fontSize: 13 }}>
                    <Clock size={14} /> Pending
                  </span>
                </td>
                <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                    <button className="btn btn-secondary btn-sm" onClick={() => handleApprove(q.id)}>
                      <CheckCircle size={14} /> Approve
                    </button>
                    <button className="btn btn-sm" style={{ color: '#ef4444' }} onClick={() => handleReject(q.id)}>
                      <XCircle size={14} /> Reject
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {questions.length === 0 && (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--text-muted)' }}>
            <Eye size={32} style={{ marginBottom: 12 }} />
            <p>No questions pending approval</p>
          </div>
        )}
      </div>
    </div>
  );
}
