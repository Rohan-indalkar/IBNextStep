import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Checkbox,
  FormControlLabel,
  FormGroup,
  CircularProgress,
  Alert,
  IconButton
} from '@mui/material';
import { Delete as DeleteIcon, Add as AddIcon } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import StatusBadge from '../../components/StatusBadge';
import PageHeader from '../../components/PageHeader';
import ActionMenu from '../../components/ActionMenu';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import EventOutlinedIcon from '@mui/icons-material/EventOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import { searchTrainerCourses } from '../../api/course';
import {
  dashboardAssignments,
  createAssignment,
  generateAssignmentWithAi,
  publishAssignmentNow,
  scheduleAssignment,
  closeAssignment,
  deleteAssignment,
  getSubmissions,
  gradeSubmission,
} from '../../api/assignment';

const SKILL_TYPES = ['TECHNICAL', 'SOFT_SKILL'];
const DIFFICULTIES = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

const EMPTY_MANUAL = {
  title: '', description: '', courseId: '', module: '', topic: '',
  skillType: 'TECHNICAL', difficultyLevel: 'BEGINNER', batchIds: [],
  questions: [''], dueDate: '', publishOption: 'SAVE_AS_DRAFT', scheduledAt: '',
};

const EMPTY_AI = {
  title: '', description: '', courseId: '', module: '', topic: '',
  skillType: 'TECHNICAL', difficultyLevel: 'BEGINNER', numberOfQuestions: 5,
  additionalInstructions: '', dueDate: '', batchIds: [],
};

export default function Assignments() {
  const { batches } = useMyBatches();
  const [courses, setCourses] = useState([]);
  const courseOptions = Array.isArray(courses) ? courses : [];
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [mode, setMode] = useState('manual');
  const [manualForm, setManualForm] = useState(EMPTY_MANUAL);
  const [aiForm, setAiForm] = useState(EMPTY_AI);
  const [files, setFiles] = useState([]);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [submissionsModal, setSubmissionsModal] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);
  const [gradingFor, setGradingFor] = useState(null);
  const [feedback, setFeedback] = useState('');
  const [rating, setRating] = useState(5);
  const [gradeSaving, setGradeSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    dashboardAssignments({ search: search || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [search, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    searchTrainerCourses({ page: 0, size: 100 })
      .then((res) => setCourses(res.data?.data || []))
      .catch(() => setCourses([]));
  }, []);

  function openCreate() {
    setMode('manual');
    setManualForm(EMPTY_MANUAL);
    setAiForm(EMPTY_AI);
    setFiles([]);
    setFormError('');
    setCreateModalOpen(true);
  }

  function toggleBatch(form, setForm, id) {
    setForm((f) => ({ ...f, batchIds: f.batchIds.includes(id) ? f.batchIds.filter((b) => b !== id) : [...f.batchIds, id] }));
  }

  function updateQuestion(i, value) {
    setManualForm((f) => ({ ...f, questions: f.questions.map((q, idx) => (idx === i ? value : q)) }));
  }
  function addQuestion() {
    setManualForm((f) => ({ ...f, questions: [...f.questions, ''] }));
  }
  function removeQuestion(i) {
    setManualForm((f) => ({ ...f, questions: f.questions.filter((_, idx) => idx !== i) }));
  }

  async function handleManualSave(e) {
    e.preventDefault();
    setFormError('');
    if (manualForm.publishOption === 'SCHEDULE_PUBLISH' && !manualForm.scheduledAt) {
      setFormError('Pick a schedule date/time.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        title: manualForm.title,
        description: manualForm.description || null,
        courseId: manualForm.courseId,
        module: manualForm.module || null,
        topic: manualForm.topic || null,
        skillType: manualForm.skillType,
        difficultyLevel: manualForm.difficultyLevel,
        batchIds: manualForm.batchIds,
        questions: manualForm.questions.filter((q) => q.trim()).map((q) => ({ questionText: q })),
        dueDate: manualForm.dueDate ? new Date(manualForm.dueDate).toISOString() : null,
        publishOption: manualForm.publishOption,
        scheduledAt: manualForm.publishOption === 'SCHEDULE_PUBLISH' ? new Date(manualForm.scheduledAt).toISOString() : null,
      };
      await createAssignment(payload, files);
      setCreateModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAiGenerate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await generateAssignmentWithAi({
        title: aiForm.title,
        description: aiForm.description || null,
        courseId: aiForm.courseId,
        module: aiForm.module || null,
        topic: aiForm.topic,
        skillType: aiForm.skillType,
        difficultyLevel: aiForm.difficultyLevel,
        numberOfQuestions: Number(aiForm.numberOfQuestions),
        additionalInstructions: aiForm.additionalInstructions || null,
        dueDate: aiForm.dueDate ? new Date(aiForm.dueDate).toISOString() : null,
        batchIds: aiForm.batchIds,
      });
      setCreateModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAction(assignment, action) {
    setBusyId(assignment.id);
    setError('');
    try {
      if (action === 'publish') await publishAssignmentNow(assignment.id);
      else if (action === 'close') await closeAssignment(assignment.id);
      else if (action === 'delete') {
        if (!window.confirm(`Delete "${assignment.title}"?`)) return;
        await deleteAssignment(assignment.id);
      }
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleSchedule(assignment) {
    const input = window.prompt('Schedule publish for (YYYY-MM-DDTHH:mm)');
    if (!input) return;
    setBusyId(assignment.id);
    try {
      await scheduleAssignment(assignment.id, new Date(input).toISOString());
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function openSubmissions(assignment) {
    setSubmissionsModal(assignment);
    setSubmissionsLoading(true);
    getSubmissions(assignment.id)
      .then((res) => setSubmissions(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setSubmissionsLoading(false));
  }

  function openGrade(submission) {
    setGradingFor(submission);
    setFeedback(submission.feedback || '');
    setRating(submission.rating || 5);
  }

  async function handleGrade(e) {
    e.preventDefault();
    setGradeSaving(true);
    try {
      await gradeSubmission(submissionsModal.id, gradingFor.studentId, { feedback, rating: Number(rating) });
      setGradingFor(null);
      openSubmissions(submissionsModal);
    } catch (err) {
      setError(err.message);
    } finally {
      setGradeSaving(false);
    }
  }

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'courseName', header: 'Course', render: (r) => r.courseName || '—' },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'questionCount', header: 'Questions' },
    { key: 'ai', header: 'AI', render: (r) => (r.generatedByAI ? 'Yes' : '—') },
    { key: 'submissions', header: 'Submissions', render: (r) => `${r.gradedSubmissions}/${r.totalSubmissions} graded` },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'View submissions', icon: <AssignmentTurnedInIcon fontSize="small" />, onClick: () => openSubmissions(r) },
            r.status === 'DRAFT' && { label: 'Publish assignment', icon: <PublishOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleAction(r, 'publish') },
            r.status === 'DRAFT' && { label: 'Schedule assignment', icon: <EventOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleSchedule(r) },
            r.status === 'PUBLISHED' && { label: 'Close assignment', icon: <LockOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleAction(r, 'close') },
            { label: 'Delete assignment', icon: <DeleteOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleAction(r, 'delete') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Assignments"
        subtitle="Manage batch assignments, grade student submissions, or generate new ones with AI."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate} startIcon={<AddIcon />}>
            New assignment
          </Button>
        }
      />

      <Box sx={{ mb: 2, maxWidth: 320 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Search title, topic…"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
        />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No assignments yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={createModalOpen} title="New assignment" onClose={() => setCreateModalOpen(false)}>
        <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
          <Button onClick={() => setMode('manual')} variant={mode === 'manual' ? 'contained' : 'outlined'}>
            Manual
          </Button>
          <Button onClick={() => setMode('ai')} variant={mode === 'ai' ? 'contained' : 'outlined'}>
            Generate with AI
          </Button>
        </Box>

        {mode === 'manual' ? (
          <Box component="form" onSubmit={handleManualSave} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField label="Title" required fullWidth value={manualForm.title} onChange={(e) => setManualForm((f) => ({ ...f, title: e.target.value }))} />
            
            <TextField select label="Course" required fullWidth value={manualForm.courseId} onChange={(e) => setManualForm((f) => ({ ...f, courseId: e.target.value }))} slotProps={{ inputLabel: { shrink: true } }}>
              <MenuItem value="" disabled>Select a course…</MenuItem>
              {courseOptions.map((c) => (
                <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
              ))}
            </TextField>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField select label="Skill type" fullWidth value={manualForm.skillType} onChange={(e) => setManualForm((f) => ({ ...f, skillType: e.target.value }))}>
                {SKILL_TYPES.map((s) => <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>)}
              </TextField>
              <TextField select label="Difficulty" fullWidth value={manualForm.difficultyLevel} onChange={(e) => setManualForm((f) => ({ ...f, difficultyLevel: e.target.value }))}>
                {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </TextField>
            </Box>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Questions</Typography>
              {manualForm.questions.map((q, i) => (
                <Box key={i} sx={{ display: 'flex', gap: 1, mb: 1 }}>
                  <TextField size="small" fullWidth value={q} onChange={(e) => updateQuestion(i, e.target.value)} placeholder={`Question ${i + 1}`} />
                  {manualForm.questions.length > 1 && (
                    <IconButton color="error" onClick={() => removeQuestion(i)}>
                      <DeleteIcon />
                    </IconButton>
                  )}
                </Box>
              ))}
              <Button size="small" startIcon={<AddIcon />} onClick={addQuestion} sx={{ mt: 1 }}>
                Add question
              </Button>
            </Box>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Reference file(s) (optional)</Typography>
              <input type="file" multiple onChange={(e) => setFiles(Array.from(e.target.files || []))} />
            </Box>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Batches</Typography>
              <FormGroup row>
                {batches.map((b) => (
                  <FormControlLabel
                    key={b.id}
                    control={<Checkbox checked={manualForm.batchIds.includes(b.id)} onChange={() => toggleBatch(manualForm, setManualForm, b.id)} />}
                    label={b.name}
                  />
                ))}
              </FormGroup>
            </Box>

            <TextField label="Due date" type="datetime-local" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={manualForm.dueDate} onChange={(e) => setManualForm((f) => ({ ...f, dueDate: e.target.value }))} />

            <TextField select label="Publish option" fullWidth value={manualForm.publishOption} onChange={(e) => setManualForm((f) => ({ ...f, publishOption: e.target.value }))}>
              <MenuItem value="SAVE_AS_DRAFT">Save as draft</MenuItem>
              <MenuItem value="PUBLISH_NOW">Publish now</MenuItem>
              <MenuItem value="SCHEDULE_PUBLISH">Schedule publish</MenuItem>
            </TextField>

            {manualForm.publishOption === 'SCHEDULE_PUBLISH' && (
              <TextField label="Schedule for" type="datetime-local" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={manualForm.scheduledAt} onChange={(e) => setManualForm((f) => ({ ...f, scheduledAt: e.target.value }))} />
            )}

            {formError && <Alert severity="error">{formError}</Alert>}
            <Button variant="contained" color="primary" type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save assignment'}
            </Button>
          </Box>
        ) : (
          <Box component="form" onSubmit={handleAiGenerate} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Generates a draft for you to review and publish — nothing goes live automatically.
            </Typography>
            
            <TextField label="Title" required fullWidth value={aiForm.title} onChange={(e) => setAiForm((f) => ({ ...f, title: e.target.value }))} />
            
            <TextField select label="Course" required fullWidth value={aiForm.courseId} onChange={(e) => setAiForm((f) => ({ ...f, courseId: e.target.value }))} slotProps={{ inputLabel: { shrink: true } }}>
              <MenuItem value="" disabled>Select a course…</MenuItem>
              {courseOptions.map((c) => (
                <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
              ))}
            </TextField>
            
            <TextField label="Topic (drives the AI prompt)" required fullWidth value={aiForm.topic} onChange={(e) => setAiForm((f) => ({ ...f, topic: e.target.value }))} placeholder="e.g. Java Streams API" />
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField select label="Skill type" fullWidth value={aiForm.skillType} onChange={(e) => setAiForm((f) => ({ ...f, skillType: e.target.value }))}>
                {SKILL_TYPES.map((s) => <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>)}
              </TextField>
              <TextField select label="Difficulty" fullWidth value={aiForm.difficultyLevel} onChange={(e) => setAiForm((f) => ({ ...f, difficultyLevel: e.target.value }))}>
                {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </TextField>
              <TextField label="Questions" type="number" inputProps={{ min: 1, max: 20 }} value={aiForm.numberOfQuestions} onChange={(e) => setAiForm((f) => ({ ...f, numberOfQuestions: e.target.value }))} />
            </Box>
            
            <TextField label="Additional instructions" fullWidth value={aiForm.additionalInstructions} onChange={(e) => setAiForm((f) => ({ ...f, additionalInstructions: e.target.value }))} placeholder="Optional guidance for the AI" />
            
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Batches</Typography>
              <FormGroup row>
                {batches.map((b) => (
                  <FormControlLabel
                    key={b.id}
                    control={<Checkbox checked={aiForm.batchIds.includes(b.id)} onChange={() => toggleBatch(aiForm, setAiForm, b.id)} />}
                    label={b.name}
                  />
                ))}
              </FormGroup>
            </Box>
            
            <TextField label="Due date" type="datetime-local" fullWidth slotProps={{ inputLabel: { shrink: true } }} value={aiForm.dueDate} onChange={(e) => setAiForm((f) => ({ ...f, dueDate: e.target.value }))} />

            {formError && <Alert severity="error">{formError}</Alert>}
            <Button variant="contained" color="primary" type="submit" disabled={saving}>
              {saving ? 'Generating…' : 'Generate draft'}
            </Button>
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(submissionsModal)} title={`Submissions — ${submissionsModal?.title || ''}`} onClose={() => setSubmissionsModal(null)}>
        {submissionsLoading ? (
          <CircularProgress />
        ) : submissions.length === 0 ? (
          <Typography color="text.secondary">No submissions yet.</Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {submissions.map((s) => (
              <Box key={s.studentId} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="subtitle2">{s.studentName}</Typography>
                  <Typography variant="caption" color="text.secondary">{s.status}</Typography>
                </Box>
                {s.rating ? (
                  <Typography variant="body2" color="success.main" sx={{ mb: 1 }}>Rated {s.rating}/5</Typography>
                ) : (
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Not graded yet</Typography>
                )}
                <Button variant="outlined" size="small" onClick={() => openGrade(s)}>
                  {s.rating ? 'Update feedback' : 'Give feedback'}
                </Button>
              </Box>
            ))}
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(gradingFor)} title={`Feedback for ${gradingFor?.studentName || ''}`} onClose={() => setGradingFor(null)}>
        <Box component="form" onSubmit={handleGrade} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField label="Rating (1-5)" type="number" inputProps={{ min: 1, max: 5 }} required fullWidth value={rating} onChange={(e) => setRating(e.target.value)} />
          <TextField label="Feedback" multiline rows={4} fullWidth value={feedback} onChange={(e) => setFeedback(e.target.value)} />
          <Button variant="contained" color="primary" type="submit" disabled={gradeSaving}>
            {gradeSaving ? 'Saving…' : 'Share feedback'}
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
