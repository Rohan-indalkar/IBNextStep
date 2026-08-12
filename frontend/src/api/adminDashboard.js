import client from './client';

export function getAdminDashboardStats() {
  return client.get('/admin/dashboard/stats');
}
