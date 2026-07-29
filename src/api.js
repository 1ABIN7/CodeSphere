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

// ─── Assessments ───
export const assessmentsAPI = {
  getMyAssessments: (params) => api.get('/assessments/mine', { params }),
  getAllAssessments: () => api.get('/v1/assessments'),
  getById: (id) => api.get(`/assessments/${id}`),
  create: (data) => api.post('/v1/assessments', data),
  update: (id, data) => api.put(`/v1/assessments/${id}`, data),
  deleteAssessment: (id) => api.delete(`/v1/assessments/${id}`),
  cloneAssessment: (id) => api.post(`/v1/assessments/${id}/clone`),
  publishAssessment: (id) => api.put(`/v1/assessments/${id}/publish`),
  unpublishAssessment: (id) => api.put(`/v1/assessments/${id}/unpublish`),
  startSession: (assessmentId) => api.post(`/assessments/${assessmentId}/session/start`),
  getSession: (assessmentId) => api.get(`/assessments/${assessmentId}/session`),
  saveAnswer: (assessmentId, questionId, data) =>
    api.post(`/assessments/${assessmentId}/session/answers`, { questionId, ...data }),
  autosaveAll: (assessmentId, answers) =>
    api.put(`/assessments/${assessmentId}/session/answers`, { answers }),
  submit: (assessmentId) => api.post(`/assessments/${assessmentId}/session/submit`),
  getResult: (assessmentId) => api.get(`/assessments/${assessmentId}/result`),
  // TODO: backend endpoint pending — see AssessmentController
  uploadFile: (assessmentId, questionId, formData) =>
    api.post(`/assessments/${assessmentId}/questions/${questionId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  // TODO: backend endpoint pending — see AssessmentController
  runCode: (assessmentId, questionId, data) =>
    api.post(`/assessments/${assessmentId}/questions/${questionId}/run`, data),
};

// ─── Question Bank ───
export const questionBankAPI = {
  list: (params) => api.get('/v1/questions', { params }),
  create: (data) => api.post('/v1/questions', data),
  update: (id, data) => api.put(`/v1/questions/${id}`, data),
  delete: (id) => api.delete(`/v1/questions/${id}`),
};

export const adminAPI = {
  getDashboard: (params) => api.get('/v1/admin/dashboard', { params }),
  getAssessmentReports: () => api.get('/v1/admin/reports/assessments'),
};

export const evaluationAPI = {
  getPending: () => api.get('/v1/evaluations/pending'),
  submit: (answerId, data) => api.put(`/v1/evaluations/${answerId}`, data),
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

// ─── Auth (extended) ───
export const authExtendedAPI = {
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  // TODO: backend endpoint pending — see AuthController
  resetPassword: (data) => api.post('/auth/reset-password', data),
  // TODO: backend endpoint pending — see AuthController
  verifyEmail: (token) => api.get(`/auth/verify-email?token=${token}`),
  // TODO: backend endpoint pending — see AuthController
};

export default api;
