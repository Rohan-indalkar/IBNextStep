import { useEffect, useState, useCallback } from 'react';
import { Box, TextField, InputAdornment, Button, Typography, Stack } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import StatusBadge from '../../components/StatusBadge';
import { ErrorState } from '../../components/States';
import { useConfirm } from '../../components/ConfirmDialog';
import { useToast } from '../../context/ToastContext';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import ActionMenu from '../../components/ActionMenu';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {
  searchDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
} from '../../api/department';

const EMPTY_FORM = { name: '', description: '' };

export default function Departments() {
  const confirm = useConfirm();
  const toast = useToast();

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [nameFilter, setNameFilter] = useState('');
  const debouncedFilter = useDebouncedValue(nameFilter, 350);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchDepartments({ name: debouncedFilter || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [debouncedFilter]);

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(dept) {
    setEditingId(dept.id);
    setForm({ name: dept.name, description: dept.description || '' });
    setFormError('');
    setModalOpen(true);
  }

  async function handleSave(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      if (editingId) {
        await updateDepartment(editingId, form);
        toast.success('Department updated.');
      } else {
        await createDepartment(form);
        toast.success('Department created.');
      }
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(dept) {
    const ok = await confirm(`Delete "${dept.name}"? This can't be undone.`, { title: 'Delete department?', tone: 'danger' });
    if (!ok) return;
    setDeletingId(dept.id);
    try {
      await deleteDepartment(dept.id);
      toast.success('Department deleted.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  }

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'description', header: 'Description', render: (r) => r.description || '—' },
    {
      key: 'active',
      header: 'Status',
      render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            {
              label: 'Edit',
              icon: <EditOutlinedIcon fontSize="small" />,
              onClick: () => openEdit(r),
            },
            {
              label: 'Delete',
              icon: <DeleteOutlinedIcon fontSize="small" />,
              color: 'error.main',
              onClick: () => handleDelete(r),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader
        title="Departments"
        subtitle="The org units batches, users and courses are organised under."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + New department
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
          emptyLabel="No departments yet — create one to get started."
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      <Modal open={modalOpen} title={editingId ? 'Edit department' : 'New department'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField
            label="Name"
            required
            fullWidth
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="e.g. Java Full Stack"
            size="small"
          />
          <TextField
            label="Description"
            fullWidth
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            placeholder="Optional"
            size="small"
          />
          {formError && <Typography color="error" variant="body2">{formError}</Typography>}
          
          <Box sx={{ mt: 1 }}>
            <Button variant="contained" color="primary" type="submit" disabled={saving} fullWidth>
              {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create department'}
            </Button>
          </Box>
        </form>
      </Modal>
    </AppShell>
  );
}
