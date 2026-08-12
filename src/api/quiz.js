import client from './client';

export function generateQuiz(payload) {
  return client.post('/trainer/quizzes/generate', payload);
}

export function updateQuiz(id, payload) {
  return client.put(`/trainer/quizzes/${id}`, payload);
}

export function editQuestion(id, order, question) {
  return client.put(`/trainer/quizzes/${id}/questions/${order}`, { question });
}

export function deleteQuestion(id, order) {
  return client.delete(`/trainer/quizzes/${id}/questions/${order}`);
}

export function addQuestion(id, question) {
  return client.post(`/trainer/quizzes/${id}/questions`, { question });
}

export function regenerateQuestion(id, order, payload) {
  return client.post(`/trainer/quizzes/${id}/questions/${order}/regenerate`, payload || {});
}

export function regenerateEntireQuiz(id) {
  return client.post(`/trainer/quizzes/${id}/regenerate`);
}

export function deleteQuiz(id) {
  return client.delete(`/trainer/quizzes/${id}`);
}

export function publishQuiz(id) {
  return client.post(`/trainer/quizzes/${id}/publish`);
}

export function listQuizzes(params) {
  return client.get('/trainer/quizzes', { params });
}

export function getQuiz(id) {
  return client.get(`/trainer/quizzes/${id}`);
}

export function getQuizAnalytics(id) {
  return client.get(`/trainer/quizzes/${id}/analytics`);
}

export function getQuizResults(id) {
  return client.get(`/trainer/quizzes/${id}/results`);
}
