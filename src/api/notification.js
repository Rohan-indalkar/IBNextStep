import client from './client';

export function composeNotification(payload) {
  return client.post('/admin/notifications', payload);
}
