import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Button, TextField, MenuItem, Stack, Typography, InputAdornment } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import StatusBadge from '../../components/StatusBadge';
import ActionMenu from '../../components/ActionMenu';
import { ErrorState } from '../../components/States';
import { useConfirm } from '../../components/ConfirmDialog';
import { useToast } from '../../context/ToastContext';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import { searchBatches, createBatch, deactivateBatch } from '../../api/batch';
import { searchCourses } from '../../api/course';

export default function Batches() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const toast = useToast();

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [nameFilter, setNameFilter] = useState('');
  const debouncedName = useDebouncedValue(nameFilter, 350);
  const [courseFilter, setCourseFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [courses, setCourses] = useState([]);

  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState('');
  const [courseId, setCourseId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchBatches({
      name: debouncedName || undefined,
      courseId: courseFilter || undefined,
      page,
      size: 10,
    })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedName, courseFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [debouncedName, courseFilter]);

  useEffect(() => {
    searchCourses({ page: 0, size: 100 })
      .then((res) => setCourses(res.data.data.content))
      .catch(() => {});
  }, []);

  function openCreate() {
    setName('');
    setCourseId('');
    setStartDate('');
    setEndDate('');
    setFormError('');
    setCreateOpen(true);
  }

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await createBatch({
        name,
        courseId,
        startDate: startDate || null,
        endDate: endDate || null,
      });
      toast.success('Batch created.');
      setCreateOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(b) {
    const ok = await confirm(`Deactivate ${b.name}? Active students will no longer see this batch.`, {
      title: 'Deactivate batch?',
      tone: 'danger',
    });
    if (!ok) return;
    try {
      await deactivateBatch(b.id);
      toast.success('Batch deactivated.');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'courseName', header: 'Course', render: (r) => r.courseName || '—' },
    {
      key: 'dates',
      header: 'Dates',
      render: (r) => (r.startDate ? `${r.startDate} to ${r.endDate || 'ongoing'}` : '—'),
    },
    { key: 'studentCount', header: 'Students', render: (r) => r.studentCount ?? 0 },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Manage batch', icon: <SettingsOutlinedIcon fontSize="small" />, onClick: () => navigate(`/app/admin/batches/${r.id}`) },
            r.status === 'ACTIVE' && { label: 'Deactivate batch', icon: <BlockOutlinedIcon fontSize="small" />, color: 'error', onClick: () => handleDeactivate(r) },
          ].filter(Boolean)}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader
        title="Batches"
        subtitle="Trainer assignments, timetables and student rosters for each cohort."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + New batch
          </Button>
        }
      />

      <Box sx={{ mb: 2, maxWidth: 320 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Search by name…"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
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
          emptyLabel="No batches yet — create one to get started."
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      <Modal open={createOpen} title="New batch" onClose={() => setCreateOpen(false)}>
        <Box component="form" onSubmit={handleCreate} sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          <TextField
            label="Name *"
            required
            fullWidth
            value={name}
            onChange={(e) => setName(e.target.value)}
            size="small"
          />
          <TextField
            select
            label="Course *"
            required
            fullWidth
            value={courseId}
            onChange={(e) => setCourseId(e.target.value)}
            size="small"
          >
            <MenuItem value="" disabled>Select a course…</MenuItem>
            {courses.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Start date"
            type="date"
            fullWidth
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            size="small"
          />
          <TextField
            label="End date (optional)"
            type="date"
            fullWidth
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            size="small"
          />

          {formError && <Typography color="error" variant="body2">{formError}</Typography>}

          <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'flex-end', mt: 1 }}>
            <Button type="button" variant="outlined" color="inherit" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" color="primary" disabled={saving}>
              {saving ? 'Creating…' : 'Create batch'}
            </Button>
          </Box>
        </Box>
      </Modal>
    </AppShell>
  );
}
