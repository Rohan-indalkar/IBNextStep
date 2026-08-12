import client from './client';

export function searchDepartments({ name, page = 0, size = 20 }) {
  return client.get('/admin/departments', { params: { name, page, size } });
}

export function getDepartment(id) {
  return client.get(`/admin/departments/${id}`);
}

export function createDepartment(payload) {
  return client.post('/admin/departments', payload);
}

export function updateDepartment(id, payload) {
  return client.put(`/admin/departments/${id}`, payload);
}

export function deleteDepartment(id) {
  return client.delete(`/admin/departments/${id}`);
}
