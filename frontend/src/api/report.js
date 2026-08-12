import client, { API_BASE_URL } from './client';

export function getReportsSummary() {
  return client.get('/admin/reports/summary');
}

export function searchReports(query) {
  return client.get('/admin/reports', { params: { query } });
}

// Excel/PDF exports return raw bytes — fetch as a blob and trigger a
// browser download rather than routing them through the JSON-envelope client.
async function downloadBlob(path, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error('Failed to generate the report. Please try again.');
  }
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

export function exportUsersExcel() {
  return downloadBlob('/admin/reports/users/export', 'users_report.xlsx');
}

export function exportUsersPdf() {
  return downloadBlob('/admin/reports/users/export/pdf', 'users_report.pdf');
}
