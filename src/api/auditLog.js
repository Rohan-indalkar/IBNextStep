import client from './client';

export function searchAuditLogs({ query, page = 0, size = 20 }) {
  return client.get('/admin/audit-logs', { params: { query, page, size } });
}
