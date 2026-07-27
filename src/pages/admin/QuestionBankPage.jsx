import { useState } from 'react';
import { Plus, Search, Filter, Edit, Trash2 } from 'lucide-react';
import Modal from '../../components/ui/Modal';

export default function QuestionBankPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Mock data for display
  const questions = [
    { id: 1, title: 'Two Sum', type: 'Coding', difficulty: 'Easy', author: 'admin@demo.com' },
    { id: 2, title: 'What is a closure in JavaScript?', type: 'MCQ', difficulty: 'Medium', author: 'instructor@demo.com' },
    { id: 3, title: 'Implement a LRU Cache', type: 'Coding', difficulty: 'Hard', author: 'admin@demo.com' }
  ];

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800 }}>Question Bank</h1>
          <p className="text-secondary">Manage and create questions for assessments</p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
          <Plus size={16} /> Create Question
        </button>
      </div>

      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', display: 'flex', gap: '12px' }}>
          <div className="input-group" style={{ maxWidth: '300px' }}>
            <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>
              <Search size={16} />
            </span>
            <input type="text" className="input" placeholder="Search questions..." style={{ paddingLeft: '36px' }} />
          </div>
          <button className="btn btn-secondary">
            <Filter size={16} /> Filter
          </button>
        </div>
        
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ padding: '12px 20px', textAlign: 'left', borderBottom: '1px solid var(--border)' }}>Title</th>
              <th style={{ padding: '12px 20px', textAlign: 'left', borderBottom: '1px solid var(--border)' }}>Type</th>
              <th style={{ padding: '12px 20px', textAlign: 'left', borderBottom: '1px solid var(--border)' }}>Difficulty</th>
              <th style={{ padding: '12px 20px', textAlign: 'left', borderBottom: '1px solid var(--border)' }}>Author</th>
              <th style={{ padding: '12px 20px', textAlign: 'right', borderBottom: '1px solid var(--border)' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {questions.map((q) => (
              <tr key={q.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '16px 20px', fontWeight: 500 }}>{q.title}</td>
                <td style={{ padding: '16px 20px' }}><span className="badge badge-default">{q.type}</span></td>
                <td style={{ padding: '16px 20px' }}>
                  <span className={`badge badge-${q.difficulty.toLowerCase()}`}>{q.difficulty}</span>
                </td>
                <td style={{ padding: '16px 20px', color: 'var(--text-secondary)' }}>{q.author}</td>
                <td style={{ padding: '16px 20px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                    <button className="btn-ghost btn-icon"><Edit size={16} /></button>
                    <button className="btn-ghost btn-icon" style={{ color: 'var(--red)' }}><Trash2 size={16} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)}
        title="Create New Question"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
            <button className="btn btn-primary" onClick={() => setIsModalOpen(false)}>Save Question</button>
          </>
        }
      >
        <div className="form-group mb-4">
          <label className="label">Question Type</label>
          <select className="select">
            <option>Coding (Algorithms)</option>
            <option>Multiple Choice (MCQ)</option>
            <option>Subjective</option>
          </select>
        </div>
        <div className="form-group mb-4">
          <label className="label">Title</label>
          <input type="text" className="input" placeholder="E.g., Reverse Linked List" />
        </div>
        <div className="form-group">
          <label className="label">Difficulty</label>
          <select className="select">
            <option>Easy</option>
            <option>Medium</option>
            <option>Hard</option>
          </select>
        </div>
      </Modal>
    </div>
  );
}
