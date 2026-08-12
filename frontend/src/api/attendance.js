import client from './client';

export function getStudentListForMarking(batchId, date) {
  return client.get(`/trainer/attendance/batches/${batchId}/students`, { params: { date } });
}

export function searchStudentInBatch(batchId, date, query) {
  return client.get(`/trainer/attendance/batches/${batchId}/students/search`, { params: { date, query } });
}

export function markAttendance(payload) {
  return client.post('/trainer/attendance/mark', payload);
}

export function editAttendance(payload) {
  return client.put('/trainer/attendance/edit', payload);
}

export function getDailySummary(batchId, date) {
  return client.get(`/trainer/attendance/batches/${batchId}/summary/daily`, { params: { date } });
}

export function getMonthlySummary(batchId, year, month) {
  return client.get(`/trainer/attendance/batches/${batchId}/summary/monthly`, { params: { year, month } });
}
