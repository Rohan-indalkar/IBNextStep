import client from './client';

export function searchCompanies({ query, active, page = 0, size = 20 }) {
  return client.get('/hr/companies', { params: { query, active, page, size } });
}

export function getCompany(id) {
  return client.get(`/hr/companies/${id}`);
}

export function createCompany(payload) {
  return client.post('/hr/companies', payload);
}

export function updateCompany(id, payload) {
  return client.put(`/hr/companies/${id}`, payload);
}

export function uploadCompanyLogo(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post(`/hr/companies/${id}/logo`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
}

export function activateCompany(id) {
  return client.patch(`/hr/companies/${id}/activate`);
}

export function deactivateCompany(id) {
  return client.patch(`/hr/companies/${id}/deactivate`);
}
