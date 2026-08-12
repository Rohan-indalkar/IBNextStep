import client from './client';

export function searchBatches({ name, page = 0, size = 20 }) {
  return client.get('/admin/batches', { params: { name, page, size } });
}

export function getBatch(id) {
  return client.get(`/admin/batches/${id}`);
}

export function createBatch(payload) {
  return client.post('/admin/batches', payload);
}

export function updateBatch(id, payload) {
  return client.put(`/admin/batches/${id}`, payload);
}

export function deactivateBatch(id) {
  return client.patch(`/admin/batches/${id}/deactivate`);
}

export function assignTechnicalTrainer(id, trainerId) {
  return client.put(`/admin/batches/${id}/technical-trainer`, { trainerId });
}

export function changeTechnicalTrainer(id, trainerId) {
  return client.put(`/admin/batches/${id}/technical-trainer/change`, { trainerId });
}

export function assignSoftSkillTrainer(id, trainerId) {
  return client.put(`/admin/batches/${id}/soft-skill-trainer`, { trainerId });
}

export function changeSoftSkillTrainer(id, trainerId) {
  return client.put(`/admin/batches/${id}/soft-skill-trainer/change`, { trainerId });
}

export function getTimetable(id) {
  return client.get(`/admin/batches/${id}/timetable`);
}

export function addTimetableEntry(id, payload) {
  return client.post(`/admin/batches/${id}/timetable`, payload);
}

export function assignStudents(id, studentIds) {
  return client.put(`/admin/batches/${id}/students`, { studentIds });
}

export function removeStudent(id, studentId) {
  return client.delete(`/admin/batches/${id}/students/${studentId}`);
}

export function bulkImportStudents(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post(`/admin/batches/${id}/students/bulk-import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}
