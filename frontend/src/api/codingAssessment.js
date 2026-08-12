import client from './client';

export function createAssessment(payload) {
  return client.post('/trainer/assessments', payload);
}

export function updateAssessment(id, payload) {
  return client.put(`/trainer/assessments/${id}`, payload);
}

export function deleteAssessment(id) {
  return client.delete(`/trainer/assessments/${id}`);
}

export function publishAssessment(id) {
  return client.post(`/trainer/assessments/${id}/publish`);
}

export function archiveAssessment(id) {
  return client.post(`/trainer/assessments/${id}/archive`);
}

export function duplicateAssessment(id) {
  return client.post(`/trainer/assessments/${id}/duplicate`);
}

export function listAssessments(params) {
  return client.get('/trainer/assessments', { params });
}

export function getAssessment(id) {
  return client.get(`/trainer/assessments/${id}`);
}

export function addQuestion(id, payload) {
  return client.post(`/trainer/assessments/${id}/questions`, payload);
}

export function editQuestion(id, questionId, payload) {
  return client.put(`/trainer/assessments/${id}/questions/${questionId}`, payload);
}

export function deleteQuestion(id, questionId) {
  return client.delete(`/trainer/assessments/${id}/questions/${questionId}`);
}

export function listQuestions(id) {
  return client.get(`/trainer/assessments/${id}/questions`);
}

export function listTestCases(questionId) {
  return client.get(`/trainer/assessments/questions/${questionId}/test-cases`);
}

export function generateQuestionsPreview(payload) {
  return client.post('/trainer/assessments/questions/generate', payload);
}

export function saveAiQuestions(id, requests) {
  return client.post(`/trainer/assessments/${id}/questions/ai`, requests);
}

export function regenerateQuestion(id, questionId, payload) {
  return client.post(`/trainer/assessments/${id}/questions/${questionId}/regenerate`, payload);
}

export function getSubmissions(id) {
  return client.get(`/trainer/assessments/${id}/submissions`);
}

export function getSessions(id) {
  return client.get(`/trainer/assessments/${id}/sessions`);
}

export function getAnalytics(id) {
  return client.get(`/trainer/assessments/${id}/analytics`);
}
