import client, { API_BASE_URL } from './client';

export function listAvailableMaterials() {
  return client.get('/student/study-materials');
}

export function getMaterial(id) {
  return client.get(`/student/study-materials/${id}`);
}

export async function downloadMaterialFile(id, fileId, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}/student/study-materials/${id}/files/${fileId}/download`, {
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
