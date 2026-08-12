import client from './client';

export function getAllRubricConfigs() {
  return client.get('/admin/evaluation-rubrics');
}

export function getRubricConfig(trainerType) {
  return client.get(`/admin/evaluation-rubrics/${trainerType}`);
}

export function updateRubricConfig(trainerType, skills) {
  return client.put(`/admin/evaluation-rubrics/${trainerType}`, { skills });
}
