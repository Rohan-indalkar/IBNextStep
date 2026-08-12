import client, { API_BASE_URL } from './client';

export function getMyAttendanceRecords(from, to) {
  return client.get('/student/attendance/records', { params: { from, to } });
}

export function getMyMonthlyAttendance(year, month) {
  return client.get('/student/attendance/monthly', { params: { year, month } });
}

export async function downloadMonthlyReport(year, month) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}/student/attendance/monthly/export?year=${year}&month=${month}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Failed to generate the report. Please try again.');
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `attendance_${year}_${month}.xlsx`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
