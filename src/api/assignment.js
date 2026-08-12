import client from './client';

function buildFormData(data, files) {
  const formData = new FormData();
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  (files || []).forEach((f) => formData.append('files', f));
  return formData;
}

export function generateAssignmentWithAi(payload) {
  return client.post('/trainer/assignments/generate-ai', payload);
}

export function createAssignment(data, files) {
  return client.post('/trainer/assignments', buildFormData(data, files), {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function updateAssignment(id, data, files) {
  return client.put(`/trainer/assignments/${id}`, buildFormData(data, files), {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deleteAssignment(id) {
  return client.delete(`/trainer/assignments/${id}`);
}

export function deleteAssignmentFile(id, fileId) {
  return client.delete(`/trainer/assignments/${id}/files/${fileId}`);
}

export function publishAssignmentNow(id) {
  return client.patch(`/trainer/assignments/${id}/publish`);
}

export function scheduleAssignment(id, scheduledAt) {
  return client.patch(`/trainer/assignments/${id}/schedule`, { scheduledAt });
}

export function closeAssignment(id) {
  return client.patch(`/trainer/assignments/${id}/close`);
}

export function getAssignment(id) {
  return client.get(`/trainer/assignments/${id}`);
}

export function dashboardAssignments(params) {
  return client.get('/trainer/assignments', { params });
}

export function getSubmissions(id) {
  return client.get(`/trainer/assignments/${id}/submissions`);
}

export function getSubmissionDetail(id, studentId) {
  return client.get(`/trainer/assignments/${id}/submissions/${studentId}`);
}

export function gradeSubmission(id, studentId, payload) {
  return client.post(`/trainer/assignments/${id}/submissions/${studentId}/feedback`, payload);
}
