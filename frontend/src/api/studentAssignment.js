import client, { API_BASE_URL } from './client';

export function listAvailableAssignments() {
  return client.get('/student/assignments');
}

export function getAssignmentDetail(id) {
  return client.get(`/student/assignments/${id}`);
}

export function submitAssignment(id, data, files) {
  const formData = new FormData();
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  (files || []).forEach((f) => formData.append('files', f));
  return client.post(`/student/assignments/${id}/submit`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export async function downloadReferenceFile(id, fileId, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}/student/assignments/${id}/files/${fileId}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Failed to download the file. Please try again.');
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'file';
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
