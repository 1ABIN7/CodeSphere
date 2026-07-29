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
  listCandidates: () => api.get('/v1/users/candidates'),
  list: () => api.get('/v1/users'),
};

// ─── Administration ───
export const adminAPI = {
  getDashboard: (params) => api.get('/v1/admin/dashboard', { params }),
  getAssessmentReports: () => api.get('/v1/admin/reports/assessments'),
  getQuestionAnalytics: () => api.get('/v1/admin/reports/questions'),
  getCandidateAnalytics: () => api.get('/v1/admin/reports/candidates'),
  updateUserRole: (id, role) => api.put(`/v1/admin/users/${id}/role`, null, { params: { newRole: role } }),
};

export const questionBankAPI = {
  list: (params) => api.get('/v1/questions', { params }),
  create: (data) => api.post('/v1/questions', data),
  update: (id, data) => api.put(`/v1/questions/${id}`, data),
  delete: (id) => api.delete(`/v1/questions/${id}`),
  import: (formData) => api.post('/v1/questions/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  export: (format) => api.get(`/v1/questions/export?format=${format}`, { responseType: 'blob' }),
  submitForApproval: (id) => api.post(`/v1/questions/${id}/submit-for-approval`),
  approve: (id) => api.post(`/v1/questions/${id}/approve`),
  reject: (id, feedback) => api.post(`/v1/questions/${id}/reject`, { feedback }),
  versions: (id) => api.get(`/v1/questions/${id}/versions`),
  restoreVersion: (id, version) => api.post(`/v1/questions/${id}/restore/${version}`),
  getRubric: (id) => api.get(`/v1/questions/${id}/rubric`),
  saveRubric: (id, criteria) => api.post(`/v1/questions/${id}/rubric`, criteria),
};

export const evaluationAPI = {
  getPending: () => api.get('/v1/evaluations/pending'),
  submit: (answerId, data) => api.put(`/v1/evaluations/${answerId}`, data),
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
  list: () => api.get('/v1/assessments'),
  listAvailable: () => api.get('/v1/assessments/available'),
  getById: (id) => api.get(`/v1/assessments/${id}`),
  create: (data) => api.post('/v1/assessments', data),
  update: (id, data) => api.put(`/v1/assessments/${id}`, data),
  publish: (id) => api.put(`/v1/assessments/${id}/publish`),
  unpublish: (id) => api.put(`/v1/assessments/${id}/unpublish`),
  assign: (id, userId, deadline) => api.post(`/v1/assessments/${id}/assign`, null, { params: { userId, deadline } }),
  getAssignments: (id) => api.get(`/v1/assessments/${id}/assigned-candidates`),
  getAssignmentStatus: (id) => api.get(`/v1/assessments/${id}/assignment-status`),
  addSection: (assessmentId, data) => api.post(`/v1/assessments/${assessmentId}/sections`, data),
  addQuestionToSection: (assessmentId, sectionId, data) => api.post(`/v1/sections/${sectionId}/questions`, data, { params: { assessmentId } }),
  getSections: (id) => api.get(`/v1/assessments/${id}/sections`),
  getQuestions: (id) => api.get(`/v1/assessments/${id}/questions`),
  startSession: (assessmentId) => api.post(`/v1/assessment-sessions/${assessmentId}/start`),
  getSession: (assessmentId) => api.get(`/assessment-sessions/${assessmentId}`),
  saveAnswer: (sessionId, questionId, value) => api.post(`/v1/assessment-sessions/${sessionId}/answers`, { questionId, value }),
  submitSession: (assessmentId) => api.post(`/v1/assessment-sessions/${assessmentId}/submit`),
  submitAssessment: (assessmentId, payload) => api.post(`/v1/assessment-sessions/${assessmentId}/submit`, payload),
  submitCodingAnswer: (problemId, payload, sessionId, questionId) => api.post(`/v1/problems/${problemId}/submit`, payload, { params: { assessmentSessionId: sessionId, assessmentQuestionId: questionId } }),
  uploadFile: (sessionId, questionId, formData) => api.post(`/v1/assessment-sessions/${sessionId}/files?questionId=${questionId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  getResult: (assessmentId) => api.get(`/v1/assessment-sessions/${assessmentId}/result`),
  getResultHistory: () => api.get('/v1/assessment-sessions/results'),
  getReadingView: (sessionId, questionId) => api.get(`/v1/comprehension/session/${sessionId}/passage/${questionId}`),
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
