import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Plus,
  Edit3,
  Copy,
  Trash2,
  CheckCircle2,
  XCircle,
  ClipboardList,
} from 'lucide-react';
import DataTable from '../../components/ui/DataTable';
import Modal from '../../components/ui/Modal';
import { assessmentsAPI } from '../../api';

export default function AssessmentListPage() {
  const navigate = useNavigate();
  const [assessments, setAssessments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadAssessments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await assessmentsAPI.getAllAssessments();
      setAssessments(res.data?.content ?? res.data ?? []);
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to load assessments');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAssessments();
  }, [loadAssessments]);

  const filtered = assessments.filter((a) => {
    if (statusFilter === 'DRAFT') return !a.published;
    if (statusFilter === 'PUBLISHED') return a.published;
    return true;
  });

  async function handleTogglePublish(a) {
    try {
      if (a.published) {
        await assessmentsAPI.unpublishAssessment(a.id);
        toast.success('Assessment unpublished');
      } else {
        await assessmentsAPI.publishAssessment(a.id);
        toast.success('Assessment published');
      }
      loadAssessments();
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Action failed');
    }
  }

  async function handleClone(a) {
    try {
      await assessmentsAPI.cloneAssessment(a.id);
      toast.success('Assessment cloned');
      loadAssessments();
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Clone failed');
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await assessmentsAPI.deleteAssessment(deleteTarget.id);
      toast.success('Assessment deleted');
      setDeleteTarget(null);
      loadAssessments();
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Delete failed');
    }
  }

  const columns = [
    { key: 'title', label: 'Title' },
    { key: 'type', label: 'Type', render: (v) => <span className="badge badge-default">{v ?? '—'}</span> },
    { key: 'durationMinutes', label: 'Duration', render: (v) => (v ? `${v}m` : '—') },
    {
      key: 'published',
      label: 'Status',
      render: (v) => (
        <span className={`badge ${v ? 'badge-easy' : 'badge-pending'}`}>
          {v ? 'Published' : 'Draft'}
        </span>
      ),
    },
    {
      key: 'questionCount',
      label: 'Questions',
      render: (v) => v ?? '—',
    },
    {
      key: 'actions',
      label: 'Actions',
      sortable: false,
      render: (_, row) => (
        <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
          <button
            className="btn-ghost btn-icon"
            title="Edit"
            onClick={(e) => {
              e.stopPropagation();
              navigate(`/admin/assessments/${row.id}/edit`);
            }}
          >
            <Edit3 size={15} />
          </button>
          <button
            className="btn-ghost btn-icon"
            title="Clone"
            onClick={(e) => {
              e.stopPropagation();
              handleClone(row);
            }}
          >
            <Copy size={15} />
          </button>
          <button
            className="btn-ghost btn-icon"
            title={row.published ? 'Unpublish' : 'Publish'}
            onClick={(e) => {
              e.stopPropagation();
              handleTogglePublish(row);
            }}
          >
            {row.published ? <XCircle size={15} /> : <CheckCircle2 size={15} />}
          </button>
          <button
            className="btn-ghost btn-icon"
            title="Delete"
            style={{ color: 'var(--red)' }}
            onClick={(e) => {
              e.stopPropagation();
              setDeleteTarget(row);
            }}
          >
            <Trash2 size={15} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: 24 }}>Assessments</h1>
          <p className="page-subtitle">Manage your assessments and exams</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/admin/assessments/new')}>
          <Plus size={16} /> New Assessment
        </button>
      </div>

      <div className="filter-tabs" style={{ marginBottom: 20 }}>
        {['ALL', 'DRAFT', 'PUBLISHED'].map((f) => (
          <button
            key={f}
            className={`filter-tab ${statusFilter === f ? 'active' : ''}`}
            onClick={() => setStatusFilter(f)}
          >
            {f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        data={filtered}
        loading={loading}
        error={error}
        onRetry={loadAssessments}
        emptyMessage="No assessments found"
        emptyIcon={<ClipboardList size={48} />}
        onRowClick={(row) => navigate(`/admin/assessments/${row.id}/edit`)}
        pageSize={10}
      />

      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Delete Assessment"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </button>
            <button className="btn" style={{ background: 'var(--red)', color: '#fff' }} onClick={handleDelete}>
              Delete
            </button>
          </>
        }
      >
        <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
          Are you sure you want to delete <strong>{deleteTarget?.title}</strong>? This action cannot be undone.
        </p>
      </Modal>
    </div>
  );
}
