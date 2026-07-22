import { Outlet } from 'react-router-dom';
import Sidebar from '../components/ui/Sidebar';
import { useAuth } from '../context/AuthContext';
import { BookOpen, Trophy, Clock, Target, ClipboardCheck } from 'lucide-react';

const CANDIDATE_NAV = [
  { path: '/dashboard', label: 'Dashboard', icon: <Target size={18} /> },
  { path: '/assessments', label: 'Assessments', icon: <ClipboardCheck size={18} /> },
  { path: '/problems', label: 'Practice Hub', icon: <BookOpen size={18} /> },
  { path: '/submissions', label: 'My Submissions', icon: <Clock size={18} /> },
  { path: '/certifications', label: 'Certifications', icon: <Trophy size={18} /> },
];

export default function CandidateLayout() {
  const { user } = useAuth();

  return (
    <div className="app-layout">
      <Sidebar items={CANDIDATE_NAV} userRole={user?.role} />
      <div className="app-main">
        <header className="app-header">
          <div className="header-greeting">Welcome back, {user?.username}</div>
        </header>
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
