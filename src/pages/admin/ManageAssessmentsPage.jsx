import { useState } from 'react';
import { Plus, Search, Calendar, Users, Settings } from 'lucide-react';
import Modal from '../../components/ui/Modal';

export default function ManageAssessmentsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const assessments = [
    { id: 1, title: 'Frontend Developer Hiring - React', date: '2026-07-20', candidates: 45, status: 'Active' },
    { id: 2, title: 'Java Backend Core Concepts', date: '2026-07-15', candidates: 120, status: 'Completed' },
  ];

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800 }}>Manage Assessments</h1>
          <p className="text-secondary">Create and monitor active assessments and exams</p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
          <Plus size={16} /> New Assessment
        </button>
      </div>

      <div className="dashboard-grid">
        {assessments.map(a => (
          <div key={a.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700, margin: 0 }}>{a.title}</h3>
              <span className={`badge ${a.status === 'Active' ? 'badge-easy' : 'badge-default'}`}>{a.status}</span>
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)', fontSize: '13px' }}>
                <Calendar size={14} /> Scheduled for {a.date}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)', fontSize: '13px' }}>
                <Users size={14} /> {a.candidates} Invited Candidates
              </div>
            </div>

            <div style={{ marginTop: 'auto', paddingTop: '16px', borderTop: '1px solid var(--border)', display: 'flex', gap: '8px' }}>
              <button className="btn btn-secondary w-full" style={{ justifyContent: 'center' }}>View Results</button>
              <button className="btn btn-secondary btn-icon"><Settings size={16} /></button>
            </div>
          </div>
        ))}
      </div>

      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)}
        title="Create Assessment"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
            <button className="btn btn-primary" onClick={() => setIsModalOpen(false)}>Draft Assessment</button>
          </>
        }
      >
        <div className="form-group mb-4">
          <label className="label">Assessment Title</label>
          <input type="text" className="input" placeholder="E.g., Midterm Evaluation 2026" />
        </div>
        <div className="form-group mb-4">
          <label className="label">Duration (Minutes)</label>
          <input type="number" className="input" defaultValue={60} />
        </div>
        <div className="form-group mb-4">
          <label className="label">Proctoring Level</label>
          <select className="select">
            <option>None</option>
            <option>Strict (Tab tracking, Webcam snapshot)</option>
          </select>
        </div>
        <div className="form-group">
          <label className="label" style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
            <input type="checkbox" defaultChecked /> Require Access Code
          </label>
        </div>
      </Modal>
    </div>
  );
}
