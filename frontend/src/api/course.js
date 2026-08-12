import client from './client';

export function searchCourses({ name, page = 0, size = 20 }) {
  return client.get('/admin/courses', { params: { name, page, size } });
}

export function searchTrainerCourses({ name, page = 0, size = 20 }) {
  return client.get('/trainer/courses', { params: { name, page, size } });
}

export function getCourse(id) {
  return client.get(`/admin/courses/${id}`);
}

export function createCourse(payload) {
  return client.post('/admin/courses', payload);
}

export function updateCourse(id, payload) {
  return client.put(`/admin/courses/${id}`, payload);
}

export function assignCourseSkills(id, skillIds) {
  return client.put(`/admin/courses/${id}/skills`, { skillIds });
}

export function deactivateCourse(id) {
  return client.patch(`/admin/courses/${id}/deactivate`);
}

// --- Skill catalog ---
export function listSkills() {
  return client.get('/admin/courses/skills');
}

export function createSkill(payload) {
  return client.post('/admin/courses/skills', payload);
}

export function updateSkill(id, payload) {
  return client.put(`/admin/courses/skills/${id}`, payload);
}
