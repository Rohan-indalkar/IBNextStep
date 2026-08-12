import client from './client';

// Maps to TrainerBatchController (/api/trainer/batches/*).
export function getMyBatches() {
  return client.get('/trainer/batches/mine');
}

export function getMyBatchStats() {
  return client.get('/trainer/batches/stats');
}

export function getBatchRoster(batchId) {
  return client.get(`/trainer/batches/${batchId}/students`);
}