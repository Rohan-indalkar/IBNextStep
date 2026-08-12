import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Checkbox,
  FormControlLabel,
  Alert
} from '@mui/material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import ActionMenu from '../../components/ActionMenu';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import UnpublishedOutlinedIcon from '@mui/icons-material/UnpublishedOutlined';
import CalendarTodayOutlinedIcon from '@mui/icons-material/CalendarTodayOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import { searchTrainerCourses } from '../../api/course';
import {
  searchMaterials,
  uploadMaterial,
  deleteMaterial,
  publishMaterialNow,
  scheduleMaterial,
  unpublishMaterial,
} from '../../api/studyMaterial';

const SKILL_TYPES = ['TECHNICAL', 'SOFT_SKILL'];
const DIFFICULTIES = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const CONTENT_TYPES = ['PDF', 'PPT', 'DOCX', 'ZIP', 'RECORDED_SESSION', 'VIDEO_LINK', 'EXTERNAL_RESOURCE_LINK'];
const FILE_BASED = new Set(['PDF', 'PPT', 'DOCX', 'ZIP', 'RECORDED_SESSION']);
const STATUSES = ['DRAFT', 'SCHEDULED', 'PUBLISHED'];

const EMPTY_FORM = {
  title: '',
  description: '',
  courseId: '',
  module: '',
  topic: '',
  skillType: 'TECHNICAL',
  difficultyLevel: 'BEGINNER',
  contentType: 'PDF',
  externalUrl: '',
  batchIds: [],
  visibleFrom: '',
  expiryDate: '',
  publishOption: 'SAVE_AS_DRAFT',
  scheduledAt: '',
};

export default function StudyMaterials() {
  const { batches } = useMyBatches();
  const [courses, setCourses] = useState([]);
  const courseOptions = Array.isArray(courses) ? courses : [];
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [files, setFiles] = useState([]);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchMaterials({ search: search || undefined, status: statusFilter || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [search, statusFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    searchTrainerCourses({ page: 0, size: 100 })
      .then((res) => setCourses(res.data?.data || []))
      .catch(() => setCourses([]));
  }, []);

  function openCreate() {
    setForm(EMPTY_FORM);
    setFiles([]);
    setFormError('');
    setModalOpen(true);
  }

  function toggleBatch(id) {
    setForm((f) => ({
      ...f,
      batchIds: f.batchIds.includes(id) ? f.batchIds.filter((b) => b !== id) : [...f.batchIds, id],
    }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setFormError('');
    if (form.publishOption === 'SCHEDULE_PUBLISH' && !form.scheduledAt) {
      setFormError('Pick a schedule date/time.');
      return;
    }
    if (!FILE_BASED.has(form.contentType) && !form.externalUrl) {
      setFormError('A URL is required for this content type.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        title: form.title,
        description: form.description || null,
        courseId: form.courseId,
        module: form.module || null,
        topic: form.topic || null,
        skillType: form.skillType,
        difficultyLevel: form.difficultyLevel,
        contentType: form.contentType,
        externalUrl: FILE_BASED.has(form.contentType) ? null : form.externalUrl,
        batchIds: form.batchIds,
        visibleFrom: form.visibleFrom || null,
        expiryDate: form.expiryDate || null,
        publishOption: form.publishOption,
        scheduledAt: form.publishOption === 'SCHEDULE_PUBLISH' ? new Date(form.scheduledAt).toISOString() : null,
      };
      await uploadMaterial(payload, FILE_BASED.has(form.contentType) ? files : []);
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAction(material, action) {
    setBusyId(material.id);
    setError('');
    try {
      if (action === 'publish') await publishMaterialNow(material.id);
      else if (action === 'unpublish') await unpublishMaterial(material.id);
      else if (action === 'delete') {
        if (!window.confirm(`Delete "${material.title}"?`)) return;
        await deleteMaterial(material.id);
      }
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleSchedule(material) {
    const input = window.prompt('Schedule publish for (YYYY-MM-DDTHH:mm), e.g. 2026-08-15T09:00');
    if (!input) return;
    setBusyId(material.id);
    try {
      await scheduleMaterial(material.id, new Date(input).toISOString());
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const courseNameById = Object.fromEntries((courses || []).map((c) => [c.id, c.name]));

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'course', header: 'Course', render: (r) => courseNameById[r.courseId] || r.courseName || '—' },
    { key: 'contentType', header: 'Type' },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <Box
          component="span"
          sx={{
            fontSize: '0.75rem',
            fontWeight: 700,
            px: 1.5,
            py: 0.5,
            borderRadius: 999,
            color: r.status === 'PUBLISHED' ? 'success.main' : 'text.secondary',
            bgcolor: r.status === 'PUBLISHED' ? 'success.light' : 'action.selected',
          }}
        >
          {r.effectiveStatus || r.status}
        </Box>
      ),
    },
    { key: 'downloads', header: 'Downloads', render: (r) => r.downloadCount ?? 0 },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            r.status !== 'PUBLISHED' && { label: 'Publish material', icon: <PublishOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleAction(r, 'publish') },
            r.status === 'DRAFT' && { label: 'Schedule', icon: <CalendarTodayOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleSchedule(r) },
            r.status === 'PUBLISHED' && { label: 'Unpublish material', icon: <UnpublishedOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleAction(r, 'unpublish') },
            { label: 'Delete material', icon: <DeleteOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleAction(r, 'delete') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Study materials" 
        subtitle="Upload and manage learning resources for your batches."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate}>
            + Upload material
          </Button>
        }
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <TextField
          size="small"
          placeholder="Search title, topic…"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          sx={{ flex: 1, minWidth: 220 }}
        />
        <TextField
          select
          size="small"
          value={statusFilter}
          onChange={(e) => {
            setPage(0);
            setStatusFilter(e.target.value);
          }}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">All statuses</MenuItem>
          {STATUSES.map((s) => (
            <MenuItem key={s} value={s}>{s}</MenuItem>
          ))}
        </TextField>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <DataTable
        columns={columns}
        rows={rows}
        loading={loading}
        emptyLabel="No study materials yet — upload one to get started."
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
      />

      <Modal open={modalOpen} title="Upload study material" onClose={() => setModalOpen(false)}>
        <Box component="form" onSubmit={handleSave} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField label="Title" required fullWidth value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} />
          <TextField label="Description" fullWidth value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
          
          <TextField select label="Course" required fullWidth value={form.courseId} onChange={(e) => setForm((f) => ({ ...f, courseId: e.target.value }))} slotProps={{ inputLabel: { shrink: true } }}>
            <MenuItem value="" disabled>Select a course…</MenuItem>
            {courseOptions.map((c) => (
              <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
            ))}
          </TextField>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Module" fullWidth value={form.module} onChange={(e) => setForm((f) => ({ ...f, module: e.target.value }))} />
            <TextField label="Topic" fullWidth value={form.topic} onChange={(e) => setForm((f) => ({ ...f, topic: e.target.value }))} />
          </Box>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField select label="Skill type" fullWidth value={form.skillType} onChange={(e) => setForm((f) => ({ ...f, skillType: e.target.value }))}>
              {SKILL_TYPES.map((s) => <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>)}
            </TextField>
            <TextField select label="Difficulty" fullWidth value={form.difficultyLevel} onChange={(e) => setForm((f) => ({ ...f, difficultyLevel: e.target.value }))}>
              {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
            </TextField>
          </Box>

          <TextField select label="Content type" fullWidth value={form.contentType} onChange={(e) => setForm((f) => ({ ...f, contentType: e.target.value }))}>
            {CONTENT_TYPES.map((c) => <MenuItem key={c} value={c}>{c.replace('_', ' ')}</MenuItem>)}
          </TextField>

          {FILE_BASED.has(form.contentType) ? (
            <Box>
              <Typography variant="body2" sx={{ mb: 1 }}>File(s)</Typography>
              <input type="file" multiple onChange={(e) => setFiles(Array.from(e.target.files || []))} />
            </Box>
          ) : (
            <TextField label="URL" required fullWidth value={form.externalUrl} onChange={(e) => setForm((f) => ({ ...f, externalUrl: e.target.value }))} placeholder="https://…" />
          )}

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Batches (leave empty to save as unassigned draft)</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              {batches.map((b) => (
                <FormControlLabel
                  key={b.id}
                  control={<Checkbox checked={form.batchIds.includes(b.id)} onChange={() => toggleBatch(b.id)} />}
                  label={b.name}
                />
              ))}
              {batches.length === 0 && <Typography variant="body2" color="text.secondary">No batches assigned to you yet.</Typography>}
            </Box>
          </Box>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Visible from" type="date" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.visibleFrom} onChange={(e) => setForm((f) => ({ ...f, visibleFrom: e.target.value }))} />
            <TextField label="Expiry date" type="date" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.expiryDate} onChange={(e) => setForm((f) => ({ ...f, expiryDate: e.target.value }))} />
          </Box>

          <TextField select label="Publish option" fullWidth value={form.publishOption} onChange={(e) => setForm((f) => ({ ...f, publishOption: e.target.value }))}>
            <MenuItem value="SAVE_AS_DRAFT">Save as draft</MenuItem>
            <MenuItem value="PUBLISH_NOW">Publish now</MenuItem>
            <MenuItem value="SCHEDULE_PUBLISH">Schedule publish</MenuItem>
          </TextField>

          {form.publishOption === 'SCHEDULE_PUBLISH' && (
            <TextField label="Schedule for" type="datetime-local" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.scheduledAt} onChange={(e) => setForm((f) => ({ ...f, scheduledAt: e.target.value }))} />
          )}

          {formError && <Alert severity="error">{formError}</Alert>}
          <Button variant="contained" color="primary" type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save material'}
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
