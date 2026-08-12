import client from './client';

function buildFormData(data, files) {
  const formData = new FormData();
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  (files || []).forEach((f) => formData.append('files', f));
  return formData;
}

export function searchMaterials(params) {
  return client.get('/trainer/study-materials', { params });
}

export function getMaterial(id) {
  return client.get(`/trainer/study-materials/${id}`);
}

export function uploadMaterial(data, files) {
  return client.post('/trainer/study-materials', buildFormData(data, files), {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function updateMaterial(id, data, files) {
  return client.put(`/trainer/study-materials/${id}`, buildFormData(data, files), {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deleteMaterial(id) {
  return client.delete(`/trainer/study-materials/${id}`);
}

export function deleteMaterialFile(id, fileId) {
  return client.delete(`/trainer/study-materials/${id}/files/${fileId}`);
}

export function publishMaterialNow(id) {
  return client.patch(`/trainer/study-materials/${id}/publish`);
}

export function scheduleMaterial(id, scheduledAt) {
  return client.patch(`/trainer/study-materials/${id}/schedule`, { scheduledAt });
}

export function unpublishMaterial(id) {
  return client.patch(`/trainer/study-materials/${id}/unpublish`);
}
