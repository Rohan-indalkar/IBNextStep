import { useEffect, useState, useCallback } from 'react';
import AppShell from '../../components/AppShell';
import HrSidebar from '../../components/HrSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import ActionMenu from '../../components/ActionMenu';
import { searchCompanies, createCompany, updateCompany, uploadCompanyLogo, activateCompany, deactivateCompany } from '../../api/company';
import { Box, Typography, Button, TextField, Chip, InputAdornment } from '@mui/material';
import SearchOutlined from '@mui/icons-material/SearchOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';

const EMPTY_FORM = { name: '', description: '', industry: '', location: '', websiteUrl: '' };

export default function Companies() {
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [logoFile, setLogoFile] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchCompanies({ query: query || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [query, page]);

  useEffect(() => {
    load();
  }, [load]);

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setLogoFile(null);
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(c) {
    setEditingId(c.id);
    setForm({ name: c.name, description: c.description || '', industry: c.industry || '', location: c.location || '', websiteUrl: c.websiteUrl || '' });
    setLogoFile(null);
    setFormError('');
    setModalOpen(true);
  }

  async function handleSave(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      let id = editingId;
      if (editingId) {
        await updateCompany(editingId, form);
      } else {
        const res = await createCompany(form);
        id = res.data.data.id;
      }
      if (logoFile) await uploadCompanyLogo(id, logoFile);
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(c) {
    setBusyId(c.id);
    try {
      if (c.active) await deactivateCompany(c.id);
      else await activateCompany(c.id);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'industry', header: 'Industry', render: (r) => r.industry || '—' },
    { key: 'location', header: 'Location', render: (r) => r.location || '—' },
    {
      key: 'active',
      header: 'Status',
      render: (r) => (
        <Chip 
          label={r.active ? 'Active' : 'Inactive'} 
          color={r.active ? 'success' : 'default'} 
          size="small" 
          sx={{ fontWeight: 600 }}
        />
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Edit company', icon: <EditOutlinedIcon fontSize="small" />, onClick: () => openEdit(r) },
            {
              label: r.active ? 'Deactivate' : 'Activate',
              icon: r.active ? <BlockOutlinedIcon fontSize="small" /> : <CheckCircleOutlinedIcon fontSize="small" />,
              color: r.active ? 'error' : 'success',
              disabled: busyId === r.id,
              onClick: () => handleToggle(r),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="HR / Recruiter" sidebar={<HrSidebar />}>
      <PageHeader
        title="Companies"
        subtitle="Manage hiring partner organizations and recruiter profiles."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + New company
          </Button>
        }
      />

      <Box sx={{ mb: 3, maxWidth: 320 }}>
        <TextField
          variant="outlined"
          size="small"
          placeholder="Search companies…"
          fullWidth
          value={query}
          onChange={(e) => { setPage(0); setQuery(e.target.value); }}
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
      </Box>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No companies yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={modalOpen} title={editingId ? 'Edit company' : 'New company'} onClose={() => setModalOpen(false)}>
        <Box component="form" onSubmit={handleSave} sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          <TextField 
            label="Name" 
            required 
            fullWidth 
            value={form.name} 
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} 
          />
          <TextField 
            label="Description" 
            fullWidth 
            value={form.description} 
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} 
          />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField 
              label="Industry" 
              fullWidth 
              value={form.industry} 
              onChange={(e) => setForm((f) => ({ ...f, industry: e.target.value }))} 
            />
            <TextField 
              label="Location" 
              fullWidth 
              value={form.location} 
              onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))} 
            />
          </Box>
          <TextField 
            label="Website" 
            fullWidth 
            value={form.websiteUrl} 
            onChange={(e) => setForm((f) => ({ ...f, websiteUrl: e.target.value }))} 
            placeholder="https://…" 
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>Logo (optional)</Typography>
            <input type="file" accept="image/*" onChange={(e) => setLogoFile(e.target.files?.[0] || null)} />
          </Box>
          {formError && <Typography color="error" variant="body2">{formError}</Typography>}
          <Button variant="contained" color="primary" type="submit" disabled={saving} sx={{ mt: 1 }}>
            {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create company'}
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
