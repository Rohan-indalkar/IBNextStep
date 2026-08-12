import client from './client';

export function searchUsers({ query, role, page = 0, size = 20 }) {
  return client.get('/admin/users', { params: { query, role, page, size } });
}

export function getUser(id) {
  return client.get(`/admin/users/${id}`);
}

export function createUser(payload) {
  return client.post('/admin/users', payload);
}

export function updateUser(id, payload) {
  return client.put(`/admin/users/${id}`, payload);
}

export function activateUser(id) {
  return client.patch(`/admin/users/${id}/activate`);
}

export function deactivateUser(id) {
  return client.patch(`/admin/users/${id}/deactivate`);
}

export function resetUserPassword(id) {
  return client.post(`/admin/users/${id}/reset-password`);
}

export function bulkImportUsers(file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post('/admin/users/bulk-import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}
