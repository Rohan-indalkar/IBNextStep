import { useEffect, useState, useCallback } from 'react';
import { TextField, InputAdornment, Box, Typography } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import DataTable from '../../components/DataTable';
import { ErrorState } from '../../components/States';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import { searchAuditLogs } from '../../api/auditLog';

export default function AuditLogs() {
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query, 350);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchAuditLogs({ query: debouncedQuery || undefined, page, size: 15 })
      .then((res) => {
        const paged = res.data.data;
        const sorted = [...paged.content].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        setRows(sorted);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedQuery, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [debouncedQuery]);

  const columns = [
    { key: 'timestamp', header: 'When', render: (r) => (r.timestamp ? new Date(r.timestamp).toLocaleString() : '—') },
    { key: 'userEmail', header: 'User' },
    { key: 'role', header: 'Role' },
    { key: 'action', header: 'Action' },
    { key: 'details', header: 'Details', render: (r) => r.details || '—' },
  ];

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader title="Audit logs" subtitle="Every recorded action, most recent first." />

      <Box sx={{ mb: 2, maxWidth: 320 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Search action, user, details…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchOutlined fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
          sx={{ bgcolor: 'background.paper' }}
        />
      </Box>

      {error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <DataTable
          columns={columns}
          rows={rows}
          loading={loading}
          emptyLabel="No audit log entries found."
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
        Audit records are permanent and kept for compliance — there's currently no way to hide or archive
        entries from this view without changing the backend, so none is offered here.
      </Typography>
    </AppShell>
  );
}
