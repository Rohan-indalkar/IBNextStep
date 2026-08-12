import { useEffect, useState, useCallback } from 'react';
import AppShell from '../../components/AppShell';
import HrSidebar from '../../components/HrSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import ActionMenu from '../../components/ActionMenu';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { searchCompanies } from '../../api/company';
import { searchPlacements, createPlacement, updatePlacement, uploadPlacementPdf, publishPlacement, closePlacement } from '../../api/placement';
import { Box, Typography, Button, TextField, Select, MenuItem, Checkbox, FormControlLabel, IconButton, Chip, Grid, FormControl, InputLabel } from '@mui/material';
import { DeleteOutlined } from '@mui/icons-material';

const TYPES = ['CAMPUS', 'OFF_CAMPUS'];

const EMPTY_ELIGIBILITY = { minAttendancePercentage: '', minQuizPercentage: '', minCodingPercentage: '', minMockInterviewRating: '', minStudentEvaluationScore: '', requireResumeApproved: false };
const EMPTY_FORM = {
  companyId: '', title: '', description: '', type: 'CAMPUS',
  eligibility: EMPTY_ELIGIBILITY, applicationDeadline: '', externalApplyLink: '',
  interviewRoundTemplates: [{ roundNumber: 1, name: '' }], packageLpa: '',
};

function StatusBadge({ status }) {
  const colors = { DRAFT: 'default', PUBLISHED: 'success', CLOSED: 'error' };
  return <Chip label={status} color={colors[status] || 'default'} size="small" sx={{ fontWeight: 600 }} />;
}

export default function Placements() {
  const [companies, setCompanies] = useState([]);
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [pdfFile, setPdfFile] = useState(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchPlacements({ page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    searchCompanies({ active: true, page: 0, size: 100 })
      .then((res) => setCompanies(res.data.data.content))
      .catch(() => {});
  }, []);

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setPdfFile(null);
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(p) {
    setEditingId(p.id);
    setForm({
      companyId: p.companyId,
      title: p.title,
      description: p.description || '',
      type: p.type,
      eligibility: p.eligibility
        ? Object.fromEntries(Object.entries(EMPTY_ELIGIBILITY).map(([k]) => [k, p.eligibility[k] ?? (k === 'requireResumeApproved' ? false : '')]))
        : EMPTY_ELIGIBILITY,
      applicationDeadline: p.applicationDeadline ? p.applicationDeadline.slice(0, 16) : '',
      externalApplyLink: p.externalApplyLink || '',
      interviewRoundTemplates: p.interviewRoundTemplates?.length ? p.interviewRoundTemplates : [{ roundNumber: 1, name: '' }],
      packageLpa: p.packageLpa ?? '',
    });
    setPdfFile(null);
    setFormError('');
    setModalOpen(true);
  }

  function updateRound(i, key, value) {
    setForm((f) => ({ ...f, interviewRoundTemplates: f.interviewRoundTemplates.map((r, idx) => (idx === i ? { ...r, [key]: value } : r)) }));
  }
  function addRound() {
    setForm((f) => ({ ...f, interviewRoundTemplates: [...f.interviewRoundTemplates, { roundNumber: f.interviewRoundTemplates.length + 1, name: '' }] }));
  }
  function removeRound(i) {
    setForm((f) => ({ ...f, interviewRoundTemplates: f.interviewRoundTemplates.filter((_, idx) => idx !== i) }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      const eligibility = form.type === 'CAMPUS'
        ? {
            minAttendancePercentage: form.eligibility.minAttendancePercentage === '' ? null : Number(form.eligibility.minAttendancePercentage),
            minQuizPercentage: form.eligibility.minQuizPercentage === '' ? null : Number(form.eligibility.minQuizPercentage),
            minCodingPercentage: form.eligibility.minCodingPercentage === '' ? null : Number(form.eligibility.minCodingPercentage),
            minMockInterviewRating: form.eligibility.minMockInterviewRating === '' ? null : Number(form.eligibility.minMockInterviewRating),
            minStudentEvaluationScore: form.eligibility.minStudentEvaluationScore === '' ? null : Number(form.eligibility.minStudentEvaluationScore),
            requireResumeApproved: form.eligibility.requireResumeApproved,
          }
        : null;
      const rounds = form.interviewRoundTemplates.filter((r) => r.name.trim()).map((r, i) => ({ roundNumber: i + 1, name: r.name }));

      let id = editingId;
      if (editingId) {
        await updatePlacement(editingId, {
          title: form.title,
          description: form.description || null,
          eligibility,
          applicationDeadline: form.applicationDeadline ? new Date(form.applicationDeadline).toISOString() : null,
          externalApplyLink: form.externalApplyLink || null,
          interviewRoundTemplates: rounds,
          packageLpa: form.packageLpa === '' ? null : Number(form.packageLpa),
        });
      } else {
        const res = await createPlacement({
          companyId: form.companyId,
          title: form.title,
          description: form.description || null,
          type: form.type,
          eligibility,
          applicationDeadline: form.applicationDeadline ? new Date(form.applicationDeadline).toISOString() : null,
          externalApplyLink: form.externalApplyLink || null,
          interviewRoundTemplates: rounds,
          packageLpa: form.packageLpa === '' ? null : Number(form.packageLpa),
        });
        id = res.data.data.id;
      }
      if (pdfFile) await uploadPlacementPdf(id, pdfFile);
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAction(p, action) {
    setBusyId(p.id);
    setError('');
    try {
      if (action === 'publish') await publishPlacement(p.id);
      else if (action === 'close') {
        if (!window.confirm(`Close "${p.title}"? No new applications after this.`)) return;
        await closePlacement(p.id);
      }
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const companyNameById = Object.fromEntries(companies.map((c) => [c.id, c.name]));

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'company', header: 'Company', render: (r) => r.companyName || companyNameById[r.companyId] || '—' },
    { key: 'type', header: 'Type', render: (r) => r.type.replace('_', ' ') },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'applications', header: 'Applications', render: (r) => `${r.totalApplications ?? 0} (${r.selected ?? 0} selected)` },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Edit drive', icon: <EditOutlinedIcon fontSize="small" />, onClick: () => openEdit(r) },
            r.status === 'DRAFT' && { label: 'Publish drive', icon: <PublishOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleAction(r, 'publish') },
            r.status === 'PUBLISHED' && { label: 'Close drive', icon: <LockOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleAction(r, 'close') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="HR / Recruiter" sidebar={<HrSidebar />}>
      <PageHeader
        title="Placement drives"
        subtitle="Campus and off-campus recruitment opportunities for students."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + New drive
          </Button>
        }
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No placement drives yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={modalOpen} title={editingId ? 'Edit placement drive' : 'New placement drive'} onClose={() => setModalOpen(false)}>
        <Box component="form" onSubmit={handleSave} sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {!editingId && (
            <FormControl fullWidth>
              <InputLabel id="pcompany-label">Company *</InputLabel>
              <Select
                labelId="pcompany-label"
                id="pcompany"
                required
                value={form.companyId}
                label="Company *"
                onChange={(e) => setForm((f) => ({ ...f, companyId: e.target.value }))}
              >
                <MenuItem value=""><em>Select…</em></MenuItem>
                {companies.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
              </Select>
            </FormControl>
          )}
          
          <TextField label="Title" required fullWidth value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} />
          <TextField label="Description" fullWidth value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
          
          {!editingId && (
            <FormControl fullWidth>
              <InputLabel id="ptype-label">Type</InputLabel>
              <Select
                labelId="ptype-label"
                id="ptype"
                value={form.type}
                label="Type"
                onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
              >
                {TYPES.map((t) => <MenuItem key={t} value={t}>{t.replace('_', ' ')}</MenuItem>)}
              </Select>
            </FormControl>
          )}

          {form.type === 'CAMPUS' && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Eligibility criteria (leave blank to skip a rule)</Typography>
              <Grid container spacing={1.5}>
                <Grid item xs={6}>
                  <TextField size="small" label="Min attendance %" type="number" fullWidth value={form.eligibility.minAttendancePercentage} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, minAttendancePercentage: e.target.value } }))} />
                </Grid>
                <Grid item xs={6}>
                  <TextField size="small" label="Min quiz %" type="number" fullWidth value={form.eligibility.minQuizPercentage} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, minQuizPercentage: e.target.value } }))} />
                </Grid>
                <Grid item xs={6}>
                  <TextField size="small" label="Min coding %" type="number" fullWidth value={form.eligibility.minCodingPercentage} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, minCodingPercentage: e.target.value } }))} />
                </Grid>
                <Grid item xs={6}>
                  <TextField size="small" label="Min mock interview (0-10)" type="number" fullWidth value={form.eligibility.minMockInterviewRating} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, minMockInterviewRating: e.target.value } }))} />
                </Grid>
                <Grid item xs={12}>
                  <TextField size="small" label="Min evaluation score (0-10)" type="number" fullWidth value={form.eligibility.minStudentEvaluationScore} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, minStudentEvaluationScore: e.target.value } }))} />
                </Grid>
              </Grid>
              <FormControlLabel
                control={<Checkbox checked={form.eligibility.requireResumeApproved} onChange={(e) => setForm((f) => ({ ...f, eligibility: { ...f.eligibility, requireResumeApproved: e.target.checked } }))} />}
                label="Require approved resume"
                sx={{ mt: 1 }}
              />
            </Box>
          )}

          <TextField label="Application deadline" type="datetime-local" slotProps={{ inputLabel: { shrink: true } }} fullWidth value={form.applicationDeadline} onChange={(e) => setForm((f) => ({ ...f, applicationDeadline: e.target.value }))} />
          
          <TextField 
            label={`External apply link ${form.type === 'OFF_CAMPUS' ? '*' : '(optional)'}`} 
            required={form.type === 'OFF_CAMPUS'} 
            fullWidth 
            value={form.externalApplyLink} 
            onChange={(e) => setForm((f) => ({ ...f, externalApplyLink: e.target.value }))} 
            placeholder="https://…" 
          />
          
          <TextField label="Package (LPA)" type="number" inputProps={{ step: "0.1" }} fullWidth value={form.packageLpa} onChange={(e) => setForm((f) => ({ ...f, packageLpa: e.target.value }))} />

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Interview round templates</Typography>
            {form.interviewRoundTemplates.map((r, i) => (
              <Box key={i} sx={{ display: 'flex', gap: 1, mb: 1 }}>
                <TextField size="small" value={r.name} onChange={(e) => updateRound(i, 'name', e.target.value)} placeholder={`Round ${i + 1} name`} fullWidth />
                {form.interviewRoundTemplates.length > 1 && (
                  <IconButton onClick={() => removeRound(i)} color="error" size="small"><DeleteOutlined /></IconButton>
                )}
              </Box>
            ))}
            <Button variant="text" size="small" onClick={addRound} sx={{ mt: 1 }}>+ Add round</Button>
          </Box>

          <Box>
            <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>JD / brochure PDF (optional)</Typography>
            <input type="file" accept="application/pdf" onChange={(e) => setPdfFile(e.target.files?.[0] || null)} />
          </Box>

          {formError && <Typography color="error" variant="body2">{formError}</Typography>}
          <Button variant="contained" color="primary" type="submit" disabled={saving} sx={{ mt: 1 }}>
            {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create drive (draft)'}
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
