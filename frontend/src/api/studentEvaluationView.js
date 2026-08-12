import client from './client';

export function getMyEvaluations() {
  return client.get('/student/evaluations');
}

export function getMyCombinedEvaluation() {
  return client.get('/student/evaluations/combined');
}
