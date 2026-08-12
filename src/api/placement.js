import client from './client';

export function searchPlacements({ status, type, page = 0, size = 20 }) {
  return client.get('/hr/placements', { params: { status, type, page, size } });
}

export function getPlacement(id) {
  return client.get(`/hr/placements/${id}`);
}

export function createPlacement(payload) {
  return client.post('/hr/placements', payload);
}

export function updatePlacement(id, payload) {
  return client.put(`/hr/placements/${id}`, payload);
}

export function uploadPlacementPdf(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post(`/hr/placements/${id}/pdf`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
}

export function publishPlacement(id) {
  return client.patch(`/hr/placements/${id}/publish`);
}

export function closePlacement(id) {
  return client.patch(`/hr/placements/${id}/close`);
}
