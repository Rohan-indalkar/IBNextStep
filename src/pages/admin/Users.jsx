import { useEffect, useState, useCallback } from 'react';
import { Box, TextField, MenuItem, Button, Typography, Stack, Grid, InputAdornment } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import LockResetOutlinedIcon from '@mui/icons-material/LockResetOutlined';
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
import {
  searchUsers,
  createUser,
  updateUser,
  activateUser,
  deactivateUser,
  resetUserPassword,
} from '../../api/user';
import { searchDepartments } from '../../api/department';

const ROLES = ['ADMIN', 'HR', 'TRAINER', 'STUDENT'];
const TRAINER_TYPES = ['TECHNICAL', 'SOFT_SKILL'];

const EMPTY_FORM = { firstName: '', lastName: '', email: '', role: 'STUDENT', trainerType: '', departmentId: '' };

export default function Users() {
  const confirm = useConfirm();
  const toast = useToast();

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query, 350);
  const [roleFilter, setRoleFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [departments, setDepartments] = useState([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchUsers({ query: debouncedQuery || undefined, role: roleFilter || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedQuery, roleFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [debouncedQuery, roleFilter]);

  useEffect(() => {
    searchDepartments({ page: 0, size: 100 })
      .then((res) => setDepartments(res.data.data.content))
      .catch(() => {});
  }, []);

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(u) {
    setEditingId(u.id);
    setForm({
      firstName: u.firstName,
      lastName: u.lastName,
      email: u.email,
      role: u.role,
      trainerType: u.trainerType || '',
      departmentId: u.departmentId || '',
    });
    setFormError('');
    setModalOpen(true);
  }

  async function handleSave(e) {
    e.preventDefault();
    setFormError('');
    if (form.role === 'TRAINER' && !form.trainerType) {
      setFormError('Trainer type is required for trainers.');
      return;
    }
    setSaving(true);
    try {
      if (editingId) {
        await updateUser(editingId, {
          firstName: form.firstName,
          lastName: form.lastName,
          trainerType: form.trainerType || null,
          departmentId: form.departmentId || null,
        });
        toast.success('User updated.');
      } else {
        await createUser({
          firstName: form.firstName,
          lastName: form.lastName,
          email: form.email,
          role: form.role,
          trainerType: form.role === 'TRAINER' ? form.trainerType : null,
          departmentId: form.departmentId || null,
        });
        toast.success('User created — credentials emailed.');
      }
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleStatus(u) {
    const activating = u.status !== 'ACTIVE';
    const ok = await confirm(
      activating ? `Reactivate ${u.firstName} ${u.lastName}? They'll be able to log in again.` : `Deactivate ${u.firstName} ${u.lastName}? They won't be able to log in.`,
      { title: activating ? 'Activate user?' : 'Deactivate user?', tone: activating ? 'default' : 'danger' }
    );
    if (!ok) return;
    setBusyId(u.id);
    try {
      if (activating) await activateUser(u.id);
      else await deactivateUser(u.id);
      toast.success(activating ? 'User activated.' : 'User deactivated.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleResetPassword(u) {
    const ok = await confirm(`Send a new temporary password to ${u.email}?`, { title: 'Reset password?' });
    if (!ok) return;
    setBusyId(u.id);
    try {
      await resetUserPassword(u.id);
      toast.success(`Temporary password emailed to ${u.email}.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const columns = [
    { key: 'name', header: 'Name', render: (r) => `${r.firstName} ${r.lastName}` },
    { key: 'email', header: 'Email' },
    { key: 'role', header: 'Role' },
    { key: 'trainerType', header: 'Trainer type', render: (r) => r.trainerType || '—' },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Edit user', icon: <EditOutlinedIcon fontSize="small" />, onClick: () => openEdit(r) },
            {
              label: r.status === 'ACTIVE' ? 'Deactivate' : 'Activate',
              icon: r.status === 'ACTIVE' ? <BlockOutlinedIcon fontSize="small" /> : <CheckCircleOutlinedIcon fontSize="small" />,
              color: r.status === 'ACTIVE' ? 'error' : 'success',
              disabled: busyId === r.id,
              onClick: () => handleToggleStatus(r),
            },
            { label: 'Reset password', icon: <LockResetOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleResetPassword(r) },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader
        title="Users"
        subtitle="Everyone with access to IBNextStep, across all roles."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + New user
          </Button>
        }
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 2.5, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="Search name or email…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          sx={{ flex: 1, minWidth: 220, bgcolor: 'background.paper' }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchOutlined fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
        <TextField
          select
          size="small"
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          sx={{ minWidth: 160, bgcolor: 'background.paper' }}
        >
          <MenuItem value="">All roles</MenuItem>
          {ROLES.map((r) => (
            <MenuItem key={r} value={r}>
              {r}
            </MenuItem>
          ))}
        </TextField>
      </Box>

      {error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <DataTable
          columns={columns}
          rows={rows}
          loading={loading}
          emptyLabel="No users found."
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}

      <Modal open={modalOpen} title={editingId ? 'Edit user' : 'New user'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="First name"
                required
                fullWidth
                value={form.firstName}
                onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Last name"
                required
                fullWidth
                value={form.lastName}
                onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                size="small"
              />
            </Grid>
          </Grid>

          <TextField
            label="Email"
            type="email"
            required
            fullWidth
            disabled={Boolean(editingId)}
            value={form.email}
            onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
            placeholder="name@infobeans.com"
            size="small"
          />

          {!editingId && (
            <TextField
              select
              label="Role"
              fullWidth
              value={form.role}
              onChange={(e) => setForm((f) => ({ ...f, role: e.target.value, trainerType: '' }))}
              size="small"
            >
              {ROLES.map((r) => (
                <MenuItem key={r} value={r}>
                  {r}
                </MenuItem>
              ))}
            </TextField>
          )}

          {form.role === 'TRAINER' && (
            <TextField
              select
              label="Trainer type"
              required
              fullWidth
              value={form.trainerType}
              onChange={(e) => setForm((f) => ({ ...f, trainerType: e.target.value }))}
              size="small"
            >
              <MenuItem value="">Select…</MenuItem>
              {TRAINER_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t.replace('_', ' ')}
                </MenuItem>
              ))}
            </TextField>
          )}

          <TextField
            select
            label="Department"
            fullWidth
            value={form.departmentId}
            onChange={(e) => setForm((f) => ({ ...f, departmentId: e.target.value }))}
            size="small"
          >
            <MenuItem value="">None</MenuItem>
            {departments.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.name}
              </MenuItem>
            ))}
          </TextField>

          {formError && <Typography color="error" variant="body2">{formError}</Typography>}
          <Box sx={{ mt: 1 }}>
            <Button variant="contained" color="primary" type="submit" disabled={saving} fullWidth>
              {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create user'}
            </Button>
          </Box>
        </form>
      </Modal>
    </AppShell>
  );
}
