import { useState } from 'react';
import { ClipboardList } from 'lucide-react';
import DataTable from '../../components/ui/DataTable';

// TODO: backend endpoint pending — see AuditLogController
const MOCK_LOGS = [
  { id: 1, timestamp: '2026-07-19T10:30:00Z', user: 'admin@demo.com', action: 'ASSESSMENT_CREATED', resource: 'Frontend Developer Hiring - React', ip: '192.168.1.100' },
  { id: 2, timestamp: '2026-07-19T10:35:00Z', user: 'admin@demo.com', action: 'ASSESSMENT_PUBLISHED', resource: 'Frontend Developer Hiring - React', ip: '192.168.1.100' },
  { id: 3, timestamp: '2026-07-19T11:00:00Z', user: 'examiner@demo.com', action: 'QUESTION_CREATED', resource: 'Two Sum', ip: '10.0.0.45' },
  { id: 4, timestamp: '2026-07-19T11:20:00Z', user: 'admin@demo.com', action: 'USER_ROLE_UPDATED', resource: 'instructor@demo.com → ROLE_EXAMINER', ip: '192.168.1.100' },
  { id: 5, timestamp: '2026-07-19T12:00:00Z', user: 'candidate@demo.com', action: 'ASSESSMENT_SUBMITTED', resource: 'Java Backend Core Concepts', ip: '172.16.0.22' },
  { id: 6, timestamp: '2026-07-19T12:15:00Z', user: 'admin@demo.com', action: 'ASSESSMENT_DELETED', resource: 'Old Draft Assessment', ip: '192.168.1.100' },
  { id: 7, timestamp: '2026-07-18T09:00:00Z', user: 'examiner@demo.com', action: 'EVALUATION_SUBMITTED', resource: 'Alice Johnson — React Assessment', ip: '10.0.0.45' },
  { id: 8, timestamp: '2026-07-18T14:30:00Z', user: 'admin@demo.com', action: 'ASSESSMENT_CLONED', resource: 'Java Backend Core Concepts (copy)', ip: '192.168.1.100' },
];

const ACTION_TYPES = ['ALL', 'ASSESSMENT_CREATED', 'ASSESSMENT_PUBLISHED', 'QUESTION_CREATED', 'ASSESSMENT_SUBMITTED', 'ASSESSMENT_DELETED', 'USER_ROLE_UPDATED', 'EVALUATION_SUBMITTED', 'ASSESSMENT_CLONED'];

function actionBadgeColor(action) {
  if (action.includes('CREATED')) return 'badge-easy';
  if (action.includes('PUBLISHED')) return 'badge-tag';
  if (action.includes('DELETED')) return 'badge-hard';
  if (action.includes('SUBMITTED')) return 'badge-medium';
  if (action.includes('UPDATED')) return 'badge-pending';
  return 'badge-default';
}

export default function AuditLogPage() {
  const [actionFilter, setActionFilter] = useState('ALL');

  const filtered = MOCK_LOGS.filter(
    (l) => actionFilter === 'ALL' || l.action === actionFilter
  );

  const columns = [
    {
      key: 'timestamp',
      label: 'Timestamp',
      render: (v) => (
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-secondary)' }}>
          {new Date(v).toLocaleString()}
        </span>
      ),
    },
    { key: 'user', label: 'User' },
    {
      key: 'action',
      label: 'Action',
      render: (v) => <span className={`badge ${actionBadgeColor(v)}`}>{v}</span>,
    },
    { key: 'resource', label: 'Resource' },
    {
      key: 'ip',
      label: 'IP Address',
      render: (v) => <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>{v}</span>,
    },
  ];

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title" style={{ fontSize: 24 }}>Audit Logs</h1>
        <p className="page-subtitle">
          System activity log
          <span style={{ marginLeft: 8, color: 'var(--yellow)', fontSize: 12 }}>
            // TODO: backend endpoint pending — see AuditLogController
          </span>
        </p>
      </div>

      <div className="filter-tabs" style={{ marginBottom: 20, flexWrap: 'wrap' }}>
        {ACTION_TYPES.map((a) => (
          <button
            key={a}
            className={`filter-tab ${actionFilter === a ? 'active' : ''}`}
            onClick={() => setActionFilter(a)}
            style={{ fontSize: 11 }}
          >
            {a === 'ALL' ? 'All' : a.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        data={filtered}
        loading={false}
        emptyMessage="No audit logs found"
        emptyIcon={<ClipboardList size={48} />}
        pageSize={15}
      />
    </div>
  );
}
