import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto-logout on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ─── Auth ───
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  refresh: (token) => api.post(`/auth/refresh?refreshToken=${token}`),
  health: () => api.get('/auth/health'),
};

// ─── Problems ───
export const problemsAPI = {
  list: (params) => api.get('/problems', { params }),
  getById: (id) => api.get(`/problems/${id}`),
  create: (data) => api.post('/problems', data),
  update: (id, data) => api.put(`/problems/${id}`, data),
  delete: (id) => api.delete(`/problems/${id}`),
  publish: (id, publish) => api.patch(`/problems/${id}/publish?publish=${publish}`),
  getTestCases: (id) => api.get(`/problems/${id}/test-cases`),
  addTestCase: (id, data) => api.post(`/problems/${id}/test-cases`, data),
};

// ─── Submissions ───
export const submissionsAPI = {
  submit: (data) => api.post('/submissions', data),
  getById: (id) => api.get(`/submissions/${id}`),
  getAnalysis: (id) => api.get(`/submissions/${id}/analysis`),
  getMySubmissions: (params) => api.get('/submissions/me', { params }),
  getForProblem: (problemId, params) => api.get(`/submissions/problem/${problemId}`, { params }),
};

// ─── Users ───
export const usersAPI = {
  getMyProfile: () => api.get('/users/me/profile'),
  updateProfile: (data) => api.put('/users/me/profile', data),
};

// ─── Administration ───
export const adminAPI = {
  getDashboard: (params) => api.get('/v1/admin/dashboard', { params }),
};

// ─── Exams ───
export const examsAPI = {
  getMyExams: () => api.get('/users/me/exams'),
};

// ─── Certifications ───
export const certificationsAPI = {
  getMyCertifications: () => api.get('/users/me/certifications'),
};

// ─── Files ───
export const filesAPI = {
  upload: (formData) => api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  download: (id) => api.get(`/files/${id}`, { responseType: 'blob' }),
  getMetadata: (id) => api.get(`/files/${id}/metadata`),
  delete: (id) => api.delete(`/files/${id}`),
  listByEntity: (entityType, entityId) => api.get(`/files/entity/${entityType}/${entityId}`),
};

// ─── Assessments ───
export const assessmentAPI = {
  list: () => api.get('/assessments'),
  getById: (id) => api.get(`/assessments/${id}`),
  getSections: (id) => api.get(`/assessments/${id}/sections`),
  startSession: (assessmentId) => api.post(`/assessment-sessions/${assessmentId}/start`),
  getSession: (assessmentId) => api.get(`/assessment-sessions/${assessmentId}`),
  saveAnswer: (sessionId, questionId, value) => api.post(`/assessment-sessions/${sessionId}/answers`, { questionId, value }),
  submitSession: (assessmentId) => api.post(`/assessment-sessions/${assessmentId}/submit`),
  submitAssessment: (assessmentId, payload) => api.post(`/assessment-sessions/${assessmentId}/submit`, payload),
  submitCodingAnswer: (sessionId, questionId, payload) => api.post(`/assessment-sessions/${sessionId}/coding-submissions`, { questionId, ...payload }),
  uploadFile: (sessionId, questionId, formData) => api.post(`/assessment-sessions/${sessionId}/files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  getResult: (assessmentId) => api.get(`/assessment-sessions/${assessmentId}/result`),
};

// ─── Interview Prep ───
export const interviewAPI = {
  getCategories: () => api.get('/interview/categories'),
  startSession: (data) => api.post('/interview/sessions', data),
  getNextQuestion: (sessionId) => api.get(`/interview/sessions/${sessionId}/next`),
  submitAnswer: (sessionId, questionId, data) => api.post(`/interview/sessions/${sessionId}/answers?questionId=${questionId}`, data),
  completeSession: (sessionId) => api.post(`/interview/sessions/${sessionId}/complete`),
  getSessionResult: (sessionId) => api.get(`/interview/sessions/${sessionId}/result`),
  getPerformanceSummary: () => api.get('/interview/performance'),
};

// ─── Proctoring ───
export const proctoringAPI = {
  recordEvent: (sessionId, data) => api.post(`/proctoring/sessions/${sessionId}/events`, data),
  uploadSnapshot: (sessionId, formData, eventType = 'WEBCAM_SNAPSHOT') => api.post(`/proctoring/sessions/${sessionId}/snapshot?eventType=${eventType}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getConfig: (assessmentId) => api.get(`/proctoring/assessments/${assessmentId}/config`),
};

export default api;
