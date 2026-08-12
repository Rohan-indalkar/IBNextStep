import client from './client';

export function getStudentPlacementDashboard() {
  return client.get('/student/dashboard/placements');
}
