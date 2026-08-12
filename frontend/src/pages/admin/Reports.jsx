import { useEffect, useState, useCallback } from 'react';
import { Card, CardContent, Typography, Box, Button, TextField, InputAdornment, Grid, Stack } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { LoadingState, EmptyState, ErrorState } from '../../components/States';
import { useToast } from '../../context/ToastContext';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import { getReportsSummary, searchReports, exportUsersExcel, exportUsersPdf } from '../../api/report';

export default function Reports() {
  const toast = useToast();
  const [summary, setSummary] = useState(null);
  const [reports, setReports] = useState([]);
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query, 300);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    Promise.all([getReportsSummary(), searchReports(debouncedQuery || undefined)])
      .then(([summaryRes, reportsRes]) => {
        setSummary(summaryRes.data.data);
        setReports(reportsRes.data.data);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedQuery]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleExport(kind) {
    setExporting(kind);
    setError('');
    try {
      if (kind === 'excel') await exportUsersExcel();
      else await exportUsersPdf();
      toast.success(`${kind === 'excel' ? 'Excel' : 'PDF'} export downloaded.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setExporting('');
    }
  }

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader title="Reports" subtitle="Organisation snapshot and exportable data, straight from the platform." />

      {error && <ErrorState message={error} onRetry={load} />}

      {summary && (
        <Grid container spacing={2} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="Students" value={summary.totalStudents} /></Grid>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="Trainers" value={summary.totalTrainers} /></Grid>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="HR" value={summary.totalHr} /></Grid>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="Active batches" value={summary.activeBatches} accent /></Grid>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="Courses" value={summary.totalCourses} /></Grid>
          <Grid item xs={12} sm={6} md={4} lg={2}><StatCard label="Active placement drives" value={summary.activePlacementDrives} accent /></Grid>
        </Grid>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Users report</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Full export of every user in the system.
          </Typography>
          <Stack direction="row" spacing={2}>
            <Button variant="contained" color="primary" onClick={() => handleExport('excel')} disabled={exporting === 'excel'}>
              {exporting === 'excel' ? 'Exporting…' : 'Export Excel'}
            </Button>
            <Button variant="outlined" color="primary" onClick={() => handleExport('pdf')} disabled={exporting === 'pdf'}>
              {exporting === 'pdf' ? 'Exporting…' : 'Export PDF'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
            <Typography variant="h6">Available reports</Typography>
            <TextField
              size="small"
              placeholder="Search reports…"
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
              sx={{ minWidth: 220 }}
            />
          </Box>

          {loading ? (
            <LoadingState rows={3} />
          ) : reports.length === 0 ? (
            <EmptyState title="No reports match your search" />
          ) : (
            <Stack spacing={1.5}>
              {reports.map((r) => (
                <Box key={r.id} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5, flexWrap: 'wrap', gap: 1 }}>
                    <Typography variant="subtitle2">{r.name}</Typography>
                    <Typography variant="caption" color="text.secondary">{r.category}</Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">{r.description}</Typography>
                </Box>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </AppShell>
  );
}
