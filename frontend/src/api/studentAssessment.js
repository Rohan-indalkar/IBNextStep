import client from './client';

export function listAssignedAssessments() {
  return client.get('/student/assessments');
}

export function startAssessment(id) {
  return client.post(`/student/assessments/${id}/start`);
}

export function getQuestion(id, questionId) {
  return client.get(`/student/assessments/${id}/questions/${questionId}`);
}

export function navigate(id, questionIndex) {
  return client.post(`/student/assessments/${id}/navigate`, { questionIndex });
}

export function saveDraft(id, questionId, language, code) {
  return client.post(`/student/assessments/${id}/questions/${questionId}/draft`, { language, code });
}

export function runCode(id, questionId, language, code) {
  return client.post(`/student/assessments/${id}/questions/${questionId}/run`, { language, code });
}

export function submitQuestion(id, questionId, language, code) {
  return client.post(`/student/assessments/${id}/questions/${questionId}/submit`, { language, code });
}

export function reviewSubmissions(id) {
  return client.get(`/student/assessments/${id}/review`);
}

export function completeAssessment(id) {
  return client.post(`/student/assessments/${id}/complete`);
}

export function reportWarning(id, type) {
  return client.post(`/student/assessments/${id}/warning`, { type });
}
