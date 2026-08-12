import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Checkbox,
  FormControlLabel,
  CircularProgress,
  Alert
} from '@mui/material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import ActionMenu from '../../components/ActionMenu';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import EditCalendarOutlinedIcon from '@mui/icons-material/EditCalendarOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import { getBatchRoster } from '../../api/trainerBatch';
import {
  createMockInterviews,
  rescheduleMockInterview,
  cancelMockInterview,
  markConducted,
  submitEvaluation,
  publishEvaluation,
  searchMockInterviews,
  getMockInterviewAnalytics,
} from '../../api/mockInterview';

const INTERVIEW_TYPES = ['TECHNICAL', 'HR', 'SOFT_SKILLS'];

const EMPTY_CREATE = { batchId: '', studentIds: [], interviewType: 'TECHNICAL', scheduledAt: '', durationMinutes: 30, meetingLink: '', notes: '' };
const EMPTY_EVAL = { scores: { 'Problem Solving': 5, Communication: 5 }, strengths: '', weaknesses: '', improvementSuggestions: '', additionalComments: '' };

export default function MockInterviews() {
  const { batches } = useMyBatches();
  const [batchStudents, setBatchStudents] = useState([]);
  const [rosterLoading, setRosterLoading] = useState(false);
  const [rosterError, setRosterError] = useState('');
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_CREATE);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [evalModal, setEvalModal] = useState(null);
  const [evalForm, setEvalForm] = useState(EMPTY_EVAL);
  const [evalSaving, setEvalSaving] = useState(false);

  const [analyticsOpen, setAnalyticsOpen] = useState(false);
  const [analytics, setAnalytics] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchMockInterviews({ page, size: 10 })
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
    if (!form.batchId) {
      setBatchStudents([]);
      setRosterError('');
      return;
    }
    let cancelled = false;
    setRosterLoading(true);
    setRosterError('');
    getBatchRoster(form.batchId)
      .then((res) => {
        if (cancelled) return;
        const raw = res.data?.data;
        const payload = Array.isArray(raw)
          ? raw
          : Array.isArray(raw?.content)
          ? raw.content
          : Array.isArray(raw?.students)
          ? raw.students
          : Array.isArray(raw?.data)
          ? raw.data
          : Array.isArray(raw?.data?.content)
          ? raw.data.content
          : Array.isArray(raw?.data?.students)
          ? raw.data.students
          : [];
        const students = payload
          .map((item) => item?.student || item)
          .filter(Boolean);
        setBatchStudents(students);
      })
      .catch((err) => {
        if (!cancelled) {
          setBatchStudents([]);
          setRosterError(err.message || 'Unable to load batch students.');
        }
      })
      .finally(() => {
        if (!cancelled) setRosterLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [form.batchId]);

  const batchNameById = Object.fromEntries(batches.map((b) => [b.id, b.name]));

  function toggleStudent(id) {
    setForm((f) => ({ ...f, studentIds: f.studentIds.includes(id) ? f.studentIds.filter((s) => s !== id) : [...f.studentIds, id] }));
  }

  function openCreate() {
    setForm(EMPTY_CREATE);
    setFormError('');
    setCreateModalOpen(true);
  }

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    if (form.studentIds.length === 0) {
      setFormError('Select at least one student.');
      return;
    }
    setSaving(true);
    try {
      await createMockInterviews({
        batchId: form.batchId,
        studentIds: form.studentIds,
        interviewType: form.interviewType,
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        durationMinutes: form.durationMinutes ? Number(form.durationMinutes) : null,
        meetingLink: form.meetingLink || null,
        notes: form.notes || null,
      });
      setCreateModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleCancel(interview) {
    const reason = window.prompt('Cancellation reason:');
    if (!reason) return;
    setBusyId(interview.id);
    try {
      await cancelMockInterview(interview.id, reason);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleReschedule(interview) {
    const input = window.prompt('New date/time (YYYY-MM-DDTHH:mm):');
    if (!input) return;
    setBusyId(interview.id);
    try {
      await rescheduleMockInterview(interview.id, { scheduledAt: new Date(input).toISOString() });
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleMarkConducted(interview) {
    setBusyId(interview.id);
    try {
      await markConducted(interview.id);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handlePublish(interview) {
    setBusyId(interview.id);
    try {
      await publishEvaluation(interview.id);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function openEval(interview) {
    setEvalModal(interview);
    setEvalForm(EMPTY_EVAL);
  }

  function updateScore(key, value) {
    setEvalForm((f) => ({ ...f, scores: { ...f.scores, [key]: Number(value) } }));
  }

  async function handleSubmitEval(e) {
    e.preventDefault();
    setEvalSaving(true);
    setError('');
    try {
      await submitEvaluation(evalModal.id, {
        scores: evalForm.scores,
        strengths: evalForm.strengths ? evalForm.strengths.split(',').map((s) => s.trim()).filter(Boolean) : [],
        weaknesses: evalForm.weaknesses ? evalForm.weaknesses.split(',').map((s) => s.trim()).filter(Boolean) : [],
        improvementSuggestions: evalForm.improvementSuggestions ? evalForm.improvementSuggestions.split(',').map((s) => s.trim()).filter(Boolean) : [],
        additionalComments: evalForm.additionalComments || null,
      });
      setEvalModal(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setEvalSaving(false);
    }
  }

  function openAnalytics() {
    setAnalyticsOpen(true);
    setAnalytics(null);
    getMockInterviewAnalytics({})
      .then((res) => setAnalytics(res.data.data))
      .catch((err) => setError(err.message));
  }

  const columns = [
    { key: 'student', header: 'Student', render: (r) => r.studentName || r.studentId },
    { key: 'batch', header: 'Batch', render: (r) => batchNameById[r.batchId] || r.batchId },
    { key: 'type', header: 'Type', render: (r) => r.interviewType },
    { key: 'scheduledAt', header: 'When', render: (r) => (r.scheduledAt ? new Date(r.scheduledAt).toLocaleString() : '—') },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            r.status === 'SCHEDULED' && { label: 'Mark conducted', icon: <EventAvailableOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleMarkConducted(r) },
            r.status === 'SCHEDULED' && { label: 'Reschedule', icon: <EditCalendarOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleReschedule(r) },
            r.status === 'SCHEDULED' && { label: 'Cancel interview', icon: <CancelOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleCancel(r) },
            r.status === 'CONDUCTED' && { label: 'Evaluate student', icon: <RateReviewOutlinedIcon fontSize="small" />, color: 'primary', onClick: () => openEval(r) },
            r.status === 'EVALUATED' && { label: 'Publish report', icon: <PublishOutlinedIcon fontSize="small" />, color: 'success', disabled: busyId === r.id, onClick: () => handlePublish(r) },
          ].filter(Boolean)}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Mock interviews"
        subtitle="Schedule, conduct, and grade mock 1-on-1 interview sessions."
        actions={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <Button variant="outlined" color="inherit" onClick={openAnalytics}>
              Analytics
            </Button>
            <Button variant="contained" color="primary" onClick={openCreate}>
              + Schedule interview
            </Button>
          </Box>
        }
      />

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No mock interviews yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={createModalOpen} title="Schedule mock interview" onClose={() => setCreateModalOpen(false)}>
        <Box component="form" onSubmit={handleCreate} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField select label="Batch" required fullWidth value={form.batchId} onChange={(e) => setForm((f) => ({ ...f, batchId: e.target.value, studentIds: [] }))} slotProps={{ inputLabel: { shrink: true } }}>
            <MenuItem value="" disabled>Select a batch…</MenuItem>
            {batches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </TextField>

          {form.batchId && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Students</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, maxHeight: 140, overflowY: 'auto' }}>
                {rosterError ? (
                  <Typography variant="body2" color="error">{rosterError}</Typography>
                ) : batchStudents.length > 0 ? (
                  batchStudents.map((s) => (
                    <FormControlLabel
                      key={s.id || `${s.firstName}-${s.lastName}-${s.email}`}
                      control={<Checkbox checked={form.studentIds.includes(s.id)} onChange={() => toggleStudent(s.id)} />}
                      label={`${s.firstName} ${s.lastName}`}
                    />
                  ))
                ) : rosterLoading ? (
                  <Typography variant="body2" color="text.secondary">Loading students…</Typography>
                ) : (
                  <Typography variant="body2" color="text.secondary">No students in this batch.</Typography>
                )}
              </Box>
            </Box>
          )}

          <TextField select label="Interview type" fullWidth value={form.interviewType} onChange={(e) => setForm((f) => ({ ...f, interviewType: e.target.value }))}>
            {INTERVIEW_TYPES.map((t) => <MenuItem key={t} value={t}>{t.replace('_', ' ')}</MenuItem>)}
          </TextField>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Scheduled for" type="datetime-local" required fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.scheduledAt} onChange={(e) => setForm((f) => ({ ...f, scheduledAt: e.target.value }))} />
            <TextField label="Duration (min)" type="number" inputProps={{ min: 1 }} fullWidth value={form.durationMinutes} onChange={(e) => setForm((f) => ({ ...f, durationMinutes: e.target.value }))} />
          </Box>

          <TextField label="Meeting link (optional)" fullWidth value={form.meetingLink} onChange={(e) => setForm((f) => ({ ...f, meetingLink: e.target.value }))} placeholder="Leave blank to auto-generate" />
          <TextField label="Notes (included in invite)" fullWidth value={form.notes} onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))} />

          {formError && <Alert severity="error">{formError}</Alert>}
          <Button variant="contained" color="primary" type="submit" disabled={saving}>
            {saving ? 'Scheduling…' : 'Schedule'}
          </Button>
        </Box>
      </Modal>

      <Modal open={Boolean(evalModal)} title={`Evaluate — ${evalModal?.studentName || ''}`} onClose={() => setEvalModal(null)}>
        <Box component="form" onSubmit={handleSubmitEval} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Scores (out of 10)</Typography>
            {Object.entries(evalForm.scores).map(([key, val]) => (
              <Box key={key} sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                <Typography variant="body2" sx={{ flex: 1 }}>{key}</Typography>
                <TextField type="number" inputProps={{ min: 0, max: 10 }} size="small" sx={{ width: 80 }} value={val} onChange={(e) => updateScore(key, e.target.value)} />
              </Box>
            ))}
          </Box>
          <TextField label="Strengths (comma-separated)" fullWidth value={evalForm.strengths} onChange={(e) => setEvalForm((f) => ({ ...f, strengths: e.target.value }))} />
          <TextField label="Weaknesses (comma-separated)" fullWidth value={evalForm.weaknesses} onChange={(e) => setEvalForm((f) => ({ ...f, weaknesses: e.target.value }))} />
          <TextField label="Improvement suggestions (comma-separated)" fullWidth value={evalForm.improvementSuggestions} onChange={(e) => setEvalForm((f) => ({ ...f, improvementSuggestions: e.target.value }))} />
          <TextField label="Additional comments" multiline rows={3} fullWidth value={evalForm.additionalComments} onChange={(e) => setEvalForm((f) => ({ ...f, additionalComments: e.target.value }))} />

          {error && <Alert severity="error">{error}</Alert>}
          <Button variant="contained" color="primary" type="submit" disabled={evalSaving}>
            {evalSaving ? 'Saving…' : 'Save evaluation'}
          </Button>
        </Box>
      </Modal>

      <Modal open={analyticsOpen} title="Mock interview analytics" onClose={() => setAnalyticsOpen(false)}>
        {!analytics ? (
          <CircularProgress />
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2">
              Scheduled: {analytics.totalScheduled} · Conducted: {analytics.totalConducted} · Evaluated: {analytics.totalEvaluated} · Published: {analytics.totalPublished} · Cancelled: {analytics.totalCancelled}
            </Typography>
            {analytics.averageOverallRating != null && <Typography variant="body2">Average overall rating: {analytics.averageOverallRating}/10</Typography>}
            
            {analytics.averageRatingByInterviewType && Object.keys(analytics.averageRatingByInterviewType).length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>By interview type</Typography>
                {Object.entries(analytics.averageRatingByInterviewType).map(([k, v]) => (
                  <Typography key={k} variant="body2" color="text.secondary">{k}: {v}/10</Typography>
                ))}
              </Box>
            )}
            
            {analytics.averageScoreByParameter && Object.keys(analytics.averageScoreByParameter).length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>By parameter</Typography>
                {Object.entries(analytics.averageScoreByParameter).map(([k, v]) => (
                  <Typography key={k} variant="body2" color="text.secondary">{k}: {v}/10</Typography>
                ))}
              </Box>
            )}
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
