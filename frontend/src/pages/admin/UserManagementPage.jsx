import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { adminAPI, usersAPI } from '../../api';

const ROLES = ['ROLE_CANDIDATE', 'ROLE_EXAMINER', 'ROLE_INSTRUCTOR', 'ROLE_ORG_ADMIN', 'ROLE_SUPER_ADMIN'];
const label = (role) => role?.replace('ROLE_', '').replaceAll('_', ' ') || 'Candidate';

export default function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [changingId, setChangingId] = useState(null);

  useEffect(() => {
    usersAPI.list().then(({ data }) => setUsers(data ?? []))
      .catch(() => toast.error('Unable to load organization users.'))
      .finally(() => setLoading(false));
  }, []);

  const changeRole = async (user, role) => {
    if (role === user.role) return;
    setChangingId(user.id);
    try {
      await adminAPI.updateUserRole(user.id, role);
      setUsers((items) => items.map((item) => item.id === user.id ? { ...item, role } : item));
      toast.success(`${user.fullName || user.username}'s role was updated.`);
    } catch (error) { toast.error(error.response?.data?.message || 'Unable to update this role.'); }
    finally { setChangingId(null); }
  };

  return <div className="container fade-in">
    <div className="page-header"><h1 className="page-title">User management</h1><p className="page-subtitle">Review organization accounts and manage access roles.</p></div>
    <div className="card">
      {loading ? <div className="loading-center"><div className="spinner" /></div> : <div className="table-container"><table><thead><tr><th>User</th><th>Email</th><th>Joined</th><th>Role</th></tr></thead><tbody>{users.map((user) => <tr key={user.id}><td><strong>{user.fullName || user.username}</strong><div className="text-secondary" style={{ fontSize: 13 }}>{user.username}</div></td><td>{user.email}</td><td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</td><td><select className="select" style={{ minWidth: 170 }} value={user.role} disabled={changingId === user.id} onChange={(event) => changeRole(user, event.target.value)}>{ROLES.map((role) => <option key={role} value={role}>{label(role)}</option>)}</select></td></tr>)}{users.length === 0 && <tr><td colSpan="4" style={{ textAlign: 'center', padding: 28 }}>No users found.</td></tr>}</tbody></table></div>}
    </div>
  </div>;
}
