import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar'; // eslint-disable-line no-unused-vars
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';
import HomePage from './pages/HomePage';
import ProblemsPage from './pages/ProblemsPage';
import ProblemDetailPage from './pages/ProblemDetailPage';
import InterviewPrepPage from './pages/InterviewPrepPage';
import InterviewSessionPage from './pages/InterviewSessionPage';
import InterviewPerformancePage from './pages/InterviewPerformancePage';
import DashboardPage from './pages/DashboardPage';
import SubmissionsPage from './pages/SubmissionsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import PublicLayout from './layouts/PublicLayout';
import CandidateLayout from './layouts/CandidateLayout';
import AdminLayout from './layouts/AdminLayout';
import AssessmentLayout from './layouts/AssessmentLayout';
import QuestionBankPage from './pages/admin/QuestionBankPage';
import ManageAssessmentsPage from './pages/admin/ManageAssessmentsPage'; // eslint-disable-line no-unused-vars
import LiveAssessmentPage from './pages/candidate/LiveAssessmentPage';
import AssessmentDashboard from './pages/candidate/AssessmentDashboard';
import AssessmentPage from './pages/candidate/AssessmentPage';
import AssessmentResultPage from './pages/candidate/AssessmentResultPage';
import ProfilePage from './pages/ProfilePage';
import CertificationPage from './pages/CertificationPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AssessmentListPage from './pages/admin/AssessmentListPage';
import AssessmentWizardPage from './pages/admin/AssessmentWizardPage';
import EvaluatorDashboard from './pages/admin/EvaluatorDashboard';
import ReportsDashboard from './pages/admin/ReportsDashboard';
import AuditLogPage from './pages/admin/AuditLogPage';
import NotificationsPage from './pages/admin/NotificationsPage';
import QuestionImportPage from './pages/admin/QuestionImportPage';
import QuestionApprovalPage from './pages/admin/QuestionApprovalPage';

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
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/reset-password" element={<ResetPasswordPage />} />
                <Route path="/verify-email" element={<VerifyEmailPage />} />
                <Route path="/auth/callback" element={<AuthCallbackPage />} />
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
                <Route path="/assessments" element={<AssessmentDashboard />} />
                <Route path="/assessments/:id/result" element={<AssessmentResultPage />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/certifications" element={<CertificationPage />} />
              </Route>

              {/* Admin Routes with Sidebar */}
              <Route path="/admin" element={<AdminLayout />}>
                <Route path="dashboard" element={<AdminDashboardPage />} />
                <Route path="assessments" element={<AssessmentListPage />} />
                <Route path="assessments/new" element={<AssessmentWizardPage />} />
                <Route path="assessments/:id/edit" element={<AssessmentWizardPage />} />
                <Route path="questions" element={<QuestionBankPage />} />
                <Route path="questions/import" element={<QuestionImportPage />} />
                <Route path="questions/approval" element={<QuestionApprovalPage />} />
                <Route path="evaluations" element={<EvaluatorDashboard />} />
                <Route path="reports" element={<ReportsDashboard />} />
                <Route path="audit-logs" element={<AuditLogPage />} />
                <Route path="notifications" element={<NotificationsPage />} />
                <Route path="users" element={<div>Manage Users</div>} />
              </Route>

              {/* Assessment Routes (full-screen, no chrome) */}
              <Route path="/assessments/:id/session" element={<AssessmentLayout />}>
                <Route index element={<AssessmentPage />} />
              </Route>

              {/* Legacy assessment route */}
              <Route path="/assessment" element={<AssessmentLayout />}>
                <Route path=":id" element={<LiveAssessmentPage />} />
              </Route>
              <Route path="/" element={<HomePage />} />
              <Route path="/problems" element={<PrivateRoute><ProblemsPage /></PrivateRoute>} />
              <Route path="/problems/:id" element={<PrivateRoute><ProblemDetailPage /></PrivateRoute>} />
              <Route path="/interview" element={<PrivateRoute><InterviewPrepPage /></PrivateRoute>} />
              <Route path="/interview/session/:id" element={<PrivateRoute><InterviewSessionPage /></PrivateRoute>} />
              <Route path="/interview/performance" element={<PrivateRoute><InterviewPerformancePage /></PrivateRoute>} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/submissions" element={<SubmissionsPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="*" element={
                <div className="container" style={{ paddingTop: 80, textAlign: 'center' }}>
                  <div style={{ fontSize: 80 }}>404</div>
                  <h2 style={{ fontSize: 24, marginBottom: 12 }}>Page Not Found</h2>
                  <a href="/" className="btn btn-primary">Go Home</a>
                </div>
              } />
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
