import client, { API_BASE_URL } from './client';

export function listResumes(status) {
  return client.get('/trainer/resumes', { params: status ? { status } : {} });
}

export function getStudentResume(studentId) {
  return client.get(`/trainer/students/${studentId}/resume`);
}

export function analyzeResume(studentId, refresh = false) {
  return client.get(`/trainer/students/${studentId}/resume/analyze`, { params: { refresh } });
}

export function autoReview(studentId, refresh = false) {
  return client.post(`/trainer/students/${studentId}/resume/auto-review`, null, { params: { refresh } });
}

export function autoReviewAll(refresh = false) {
  return client.post('/trainer/resumes/auto-review-all', null, { params: { refresh } });
}

export function reviewResume(studentId, payload) {
  return client.post(`/trainer/students/${studentId}/resume/review`, payload);
}

export async function downloadResumeFile(studentId, filename) {
  const token = localStorage.getItem('ibns_token');
  const res = await fetch(`${API_BASE_URL}/trainer/students/${studentId}/resume/file`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Failed to download the resume. Please try again.');
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'resume.pdf';
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
