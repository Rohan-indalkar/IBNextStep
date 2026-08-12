import client from './client';

export function listAssignedQuizzes() {
  return client.get('/student/quizzes');
}

export function getQuizInstructions(id) {
  return client.get(`/student/quizzes/${id}`);
}

export function startQuiz(id) {
  return client.post(`/student/quizzes/${id}/start`);
}

export function autoSaveQuiz(id, answers) {
  return client.post(`/student/quizzes/${id}/autosave`, { answers });
}

export function submitQuiz(id, payload) {
  return client.post(`/student/quizzes/${id}/submit`, payload || {});
}

export function reportViolation(id, type) {
  return client.post(`/student/quizzes/${id}/violation`, { type });
}

export function getMyResults() {
  return client.get('/student/results');
}
