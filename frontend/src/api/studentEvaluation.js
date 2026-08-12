import client, { API_BASE_URL } from './client';

export function getRubric() {
  return client.get('/trainer/student-evaluations/rubric');
}

export function getMetrics(studentId) {
  return client.get(`/trainer/student-evaluations/metrics/${studentId}`);
}

export function getBatchOverview(batchId) {
  return client.get(`/trainer/student-evaluations/batch/${batchId}`);
}

export function submitEvaluation(studentId, payload) {
  return client.post(`/trainer/student-evaluations/${studentId}`, payload);
}

export function updateEvaluation(id, payload) {
  return client.put(`/trainer/student-evaluations/${id}`, payload);
}

export function getCombined(studentId) {
  return client.get(`/trainer/student-evaluations/combined/${studentId}`);
}

export function getHistory(studentId) {
  return client.get(`/trainer/student-evaluations/student/${studentId}`);
}

export function getOne(id) {
  return client.get(`/trainer/student-evaluations/${id}`);
}

async function downloadBlob(path, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Failed to generate the export. Please try again.');
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export function exportPdf(id) {
  return downloadBlob(`/trainer/student-evaluations/${id}/export/pdf`, `evaluation_${id}.pdf`);
}

export function exportExcel(id) {
  return downloadBlob(`/trainer/student-evaluations/${id}/export/excel`, `evaluation_${id}.xlsx`);
}
