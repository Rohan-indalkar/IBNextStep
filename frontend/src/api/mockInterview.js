import client from './client';

export function createMockInterviews(payload) {
  return client.post('/trainer/mock-interviews', payload);
}

export function rescheduleMockInterview(id, payload) {
  return client.put(`/trainer/mock-interviews/${id}/reschedule`, payload);
}

export function cancelMockInterview(id, reason) {
  return client.patch(`/trainer/mock-interviews/${id}/cancel`, { reason });
}

export function markConducted(id) {
  return client.patch(`/trainer/mock-interviews/${id}/conducted`);
}

export function submitEvaluation(id, payload) {
  return client.post(`/trainer/mock-interviews/${id}/evaluation`, payload);
}

export function publishEvaluation(id) {
  return client.patch(`/trainer/mock-interviews/${id}/publish`);
}

export function getMockInterview(id) {
  return client.get(`/trainer/mock-interviews/${id}`);
}

export function searchMockInterviews(params) {
  return client.get('/trainer/mock-interviews', { params });
}

export function getMockInterviewAnalytics(params) {
  return client.get('/trainer/mock-interviews/analytics', { params });
}
