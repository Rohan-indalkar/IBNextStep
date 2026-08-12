import client from './client';

export function getMyResume() {
  return client.get('/student/resume');
}

export function uploadResume(file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post('/student/resume', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
}
