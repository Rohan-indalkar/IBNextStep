import client from './client';

export function searchApplications({ companyId, departmentId, placementId, status, page = 0, size = 20 }) {
  return client.get('/hr/applications', { params: { companyId, departmentId, placementId, status, page, size } });
}

export function getApplication(id) {
  return client.get(`/hr/applications/${id}`);
}

export function shortlistApplication(id) {
  return client.patch(`/hr/applications/${id}/shortlist`);
}

export function rejectApplication(id, reason) {
  return client.patch(`/hr/applications/${id}/reject`, { reason });
}

export function selectApplication(id) {
  return client.patch(`/hr/applications/${id}/select`);
}

export function notSelectApplication(id, reason) {
  return client.patch(`/hr/applications/${id}/not-select`, { reason });
}

export function scheduleRound(id, payload) {
  return client.post(`/hr/applications/${id}/interview-rounds`, payload);
}

export function rescheduleRound(id, roundNumber, payload) {
  return client.put(`/hr/applications/${id}/interview-rounds/${roundNumber}/reschedule`, payload);
}

export function cancelRound(id, roundNumber, reason) {
  return client.patch(`/hr/applications/${id}/interview-rounds/${roundNumber}/cancel`, { reason });
}

export function updateRoundResult(id, roundNumber, payload) {
  return client.patch(`/hr/applications/${id}/interview-rounds/${roundNumber}/result`, payload);
}
