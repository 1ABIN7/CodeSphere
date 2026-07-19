import { Outlet } from 'react-router-dom';
import Sidebar from '../components/ui/Sidebar';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, Users, FileQuestion, Calendar } from 'lucide-react';

const ADMIN_NAV = [
  { path: '/admin/dashboard', label: 'Overview', icon: <LayoutDashboard size={18} /> },
  { path: '/admin/questions', label: 'Question Bank', icon: <FileQuestion size={18} /> },
  { path: '/admin/assessments', label: 'Assessments', icon: <Calendar size={18} /> },
  { path: '/admin/users', label: 'Manage Users', icon: <Users size={18} /> },
];

export default function AdminLayout() {
  const { user } = useAuth();

  return (
    <div className="app-layout">
      <Sidebar items={ADMIN_NAV} userRole={user?.role} />
      <div className="app-main">
        <header className="app-header">
          <div className="header-greeting">Admin Console</div>
        </header>
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
