import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import ProblemsPage from './pages/ProblemsPage';
import ProblemDetailPage from './pages/ProblemDetailPage';
import DashboardPage from './pages/DashboardPage';
import SubmissionsPage from './pages/SubmissionsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import PublicLayout from './layouts/PublicLayout';
import CandidateLayout from './layouts/CandidateLayout';
import AdminLayout from './layouts/AdminLayout';
import AssessmentLayout from './layouts/AssessmentLayout';
import QuestionBankPage from './pages/admin/QuestionBankPage';
import ManageAssessmentsPage from './pages/admin/ManageAssessmentsPage';
import LiveAssessmentPage from './pages/candidate/LiveAssessmentPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
            <Routes>
              {/* Public Routes with Navbar */}
              <Route element={<PublicLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/problems" element={<ProblemsPage />} />
                <Route path="/problems/:id" element={<ProblemDetailPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="*" element={
                  <div className="container" style={{ paddingTop: 80, textAlign: 'center' }}>
                    <div style={{ fontSize: 80 }}>404</div>
                    <h2 style={{ fontSize: 24, marginBottom: 12 }}>Page Not Found</h2>
                    <a href="/" className="btn btn-primary">Go Home</a>
                  </div>
                } />
              </Route>

              {/* Candidate Routes with Sidebar */}
              <Route element={<CandidateLayout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/submissions" element={<SubmissionsPage />} />
              </Route>

              {/* Admin Routes with Sidebar */}
              <Route path="/admin" element={<AdminLayout />}>
                <Route path="dashboard" element={<div>Admin Dashboard</div>} />
                <Route path="questions" element={<QuestionBankPage />} />
                <Route path="assessments" element={<ManageAssessmentsPage />} />
                <Route path="users" element={<div>Manage Users</div>} />
              </Route>

              {/* Assessment Routes */}
              <Route path="/assessment" element={<AssessmentLayout />}>
                <Route path=":id" element={<LiveAssessmentPage />} />
              </Route>
            </Routes>

        <Toaster
          position="bottom-right"
          toastOptions={{
            style: {
              background: 'var(--bg-card)',
              color: 'var(--text-primary)',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '14px',
            },
            success: { iconTheme: { primary: '#10b981', secondary: 'white' } },
            error: { iconTheme: { primary: '#ef4444', secondary: 'white' } },
          }}
        />
      </BrowserRouter>
    </AuthProvider>
  );
}
