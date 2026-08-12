import client, { API_BASE_URL } from './client';

export function browsePlacements(page = 0, size = 20) {
  return client.get('/student/placements', { params: { page, size } });
}

export function getPlacement(id) {
  return client.get(`/student/placements/${id}`);
}

export function applyToPlacement(id) {
  return client.post(`/student/placements/${id}/apply`);
}

export function getMyApplications() {
  return client.get('/student/placements/applications');
}

export async function downloadPlacementPdf(id, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}/student/placements/${id}/pdf`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Failed to download the PDF. Please try again.');
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'placement.pdf';
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
