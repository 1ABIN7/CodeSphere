import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
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

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app">
          <Navbar />
          <main className="main-content">
            <Routes>
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
          </main>
        </div>

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
