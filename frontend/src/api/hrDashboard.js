import client from './client';

export function getHrDashboard() {
  return client.get('/hr/dashboard/placements');
}
