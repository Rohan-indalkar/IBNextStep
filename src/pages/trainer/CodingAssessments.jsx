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
  Alert
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import ActionMenu from '../../components/ActionMenu';
import CodeOutlinedIcon from '@mui/icons-material/CodeOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import {
  listAssessments,
  createAssessment,
  deleteAssessment,
  publishAssessment,
  archiveAssessment,
  duplicateAssessment,
  listQuestions,
  addQuestion,
  deleteQuestion,
  generateQuestionsPreview,
  saveAiQuestions,
  getAnalytics,
} from '../../api/codingAssessment';

const LANGUAGES = ['JAVA', 'PYTHON3', 'CPP', 'C', 'JAVASCRIPT'];
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

const EMPTY_ASSESSMENT = {
  title: '', description: '', batchId: '', durationMinutes: 60,
  startTime: '', endTime: '', passingMarks: 50, maxAttempts: 1, allowedLanguages: ['JAVA'],
};

const EMPTY_QUESTION = {
  title: '', problemStatement: '', inputFormat: '', outputFormat: '', constraints: '',
  difficulty: 'MEDIUM', marks: 10, timeLimitSeconds: 2, memoryLimitMb: 256,
  allowedLanguages: ['JAVA'], publicTestCases: [{ input: '', expectedOutput: '' }], hiddenTestCases: [],
};

export default function CodingAssessments() {
  const { batches } = useMyBatches();
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_ASSESSMENT);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [questionsModal, setQuestionsModal] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [qLoading, setQLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [qForm, setQForm] = useState(EMPTY_QUESTION);
  const [qSaving, setQSaving] = useState(false);

  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [aiForm, setAiForm] = useState({ topic: '', language: 'JAVA', difficulty: '', questionCount: 3 });
  const [aiPreview, setAiPreview] = useState(null);
  const [aiSaving, setAiSaving] = useState(false);

  const [analyticsModal, setAnalyticsModal] = useState(null);
  const [analytics, setAnalytics] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    listAssessments({ page, size: 10 })
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

  function toggleLang(formObj, setFormObj, lang) {
    setFormObj((f) => ({ ...f, allowedLanguages: f.allowedLanguages.includes(lang) ? f.allowedLanguages.filter((l) => l !== lang) : [...f.allowedLanguages, lang] }));
  }

  function openCreate() {
    setForm(EMPTY_ASSESSMENT);
    setFormError('');
    setCreateModalOpen(true);
  }

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await createAssessment({
        title: form.title,
        description: form.description || null,
        batchId: form.batchId,
        durationMinutes: Number(form.durationMinutes),
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        passingMarks: Number(form.passingMarks),
        maxAttempts: Number(form.maxAttempts),
        allowedLanguages: form.allowedLanguages,
      });
      setCreateModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAction(assessment, action) {
    setBusyId(assessment.id);
    setError('');
    try {
      if (action === 'publish') await publishAssessment(assessment.id);
      else if (action === 'archive') await archiveAssessment(assessment.id);
      else if (action === 'duplicate') await duplicateAssessment(assessment.id);
      else if (action === 'delete') {
        if (!window.confirm(`Delete "${assessment.title}"?`)) return;
        await deleteAssessment(assessment.id);
      }
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function openQuestions(assessment) {
    setQuestionsModal(assessment);
    setQLoading(true);
    listQuestions(assessment.id)
      .then((res) => setQuestions(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setQLoading(false));
  }

  function refreshQuestions() {
    listQuestions(questionsModal.id).then((res) => setQuestions(res.data.data));
  }

  function openAddQuestion() {
    setQForm(EMPTY_QUESTION);
    setAddModalOpen(true);
  }

  function updateTestCase(list, setter, field, idx, key, value) {
    setter((f) => ({ ...f, [field]: f[field].map((tc, i) => (i === idx ? { ...tc, [key]: value } : tc)) }));
  }

  async function handleAddQuestion(e) {
    e.preventDefault();
    setQSaving(true);
    setError('');
    try {
      await addQuestion(questionsModal.id, {
        title: qForm.title,
        problemStatement: qForm.problemStatement,
        inputFormat: qForm.inputFormat || null,
        outputFormat: qForm.outputFormat || null,
        constraints: qForm.constraints || null,
        examples: [],
        difficulty: qForm.difficulty,
        marks: Number(qForm.marks),
        timeLimitSeconds: Number(qForm.timeLimitSeconds),
        memoryLimitMb: Number(qForm.memoryLimitMb),
        allowedLanguages: qForm.allowedLanguages,
        publicTestCases: qForm.publicTestCases.filter((tc) => tc.input && tc.expectedOutput),
        hiddenTestCases: qForm.hiddenTestCases.filter((tc) => tc.input && tc.expectedOutput),
      });
      setAddModalOpen(false);
      refreshQuestions();
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setQSaving(false);
    }
  }

  async function handleDeleteQuestion(questionId) {
    if (!window.confirm('Delete this question?')) return;
    try {
      await deleteQuestion(questionsModal.id, questionId);
      refreshQuestions();
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  function openAiModal() {
    setAiForm({ topic: '', language: 'JAVA', difficulty: '', questionCount: 3 });
    setAiPreview(null);
    setAiModalOpen(true);
  }

  async function handleAiPreview(e) {
    e.preventDefault();
    setError('');
    setAiSaving(true);
    try {
      const res = await generateQuestionsPreview({
        topic: aiForm.topic,
        language: aiForm.language,
        difficulty: aiForm.difficulty || null,
        questionCount: Number(aiForm.questionCount),
      });
      setAiPreview(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setAiSaving(false);
    }
  }

  async function handleAiSave() {
    setAiSaving(true);
    setError('');
    try {
      await saveAiQuestions(questionsModal.id, aiPreview);
      setAiModalOpen(false);
      setAiPreview(null);
      refreshQuestions();
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setAiSaving(false);
    }
  }

  function openAnalytics(assessment) {
    setAnalyticsModal(assessment);
    setAnalytics(null);
    getAnalytics(assessment.id)
      .then((res) => setAnalytics(res.data.data))
      .catch((err) => setError(err.message));
  }

  const batchNameById = Object.fromEntries(batches.map((b) => [b.id, b.name]));

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'batch', header: 'Batch', render: (r) => batchNameById[r.batchId] || r.batchId },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'questions', header: 'Questions', render: (r) => r.questionIds?.length ?? 0 },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'View problems', icon: <CodeOutlinedIcon fontSize="small" />, onClick: () => openQuestions(r) },
            { label: 'View analytics', icon: <BarChartOutlinedIcon fontSize="small" />, onClick: () => openAnalytics(r) },
            r.status === 'DRAFT' && { label: 'Publish assessment', icon: <PublishOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleAction(r, 'publish') },
            { label: 'Duplicate assessment', icon: <ContentCopyOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleAction(r, 'duplicate') },
            r.status === 'PUBLISHED' && { label: 'Archive assessment', icon: <ArchiveOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleAction(r, 'archive') },
            { label: 'Delete assessment', icon: <DeleteOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleAction(r, 'delete') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Coding assessments"
        subtitle="Create automated code assessments and review submission results."
        actions={
          <Button variant="contained" color="primary" onClick={openCreate} startIcon={<AddIcon />}>
            New assessment
          </Button>
        }
      />

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No assessments yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={createModalOpen} title="New coding assessment" onClose={() => setCreateModalOpen(false)}>
        <Box component="form" onSubmit={handleCreate} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField label="Title" required fullWidth value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} />
          <TextField label="Description" fullWidth value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
          
          <TextField select label="Batch" required fullWidth value={form.batchId} onChange={(e) => setForm((f) => ({ ...f, batchId: e.target.value }))} slotProps={{ inputLabel: { shrink: true } }}>
            <MenuItem value="" disabled>Select a batch…</MenuItem>
            {batches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </TextField>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Start" type="datetime-local" required fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.startTime} onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))} />
            <TextField label="End" type="datetime-local" required fullWidth slotProps={{ inputLabel: { shrink: true } }} value={form.endTime} onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))} />
          </Box>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Duration (min)" type="number" inputProps={{ min: 1 }} fullWidth value={form.durationMinutes} onChange={(e) => setForm((f) => ({ ...f, durationMinutes: e.target.value }))} />
            <TextField label="Passing marks" type="number" inputProps={{ min: 0 }} fullWidth value={form.passingMarks} onChange={(e) => setForm((f) => ({ ...f, passingMarks: e.target.value }))} />
            <TextField label="Max attempts" type="number" inputProps={{ min: 1 }} fullWidth value={form.maxAttempts} onChange={(e) => setForm((f) => ({ ...f, maxAttempts: e.target.value }))} />
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Allowed languages</Typography>
            <FormGroup row>
              {LANGUAGES.map((l) => (
                <FormControlLabel
                  key={l}
                  control={<Checkbox checked={form.allowedLanguages.includes(l)} onChange={() => toggleLang(form, setForm, l)} />}
                  label={l}
                />
              ))}
            </FormGroup>
          </Box>

          {formError && <Alert severity="error">{formError}</Alert>}
          <Button variant="contained" color="primary" type="submit" disabled={saving}>
            {saving ? 'Creating…' : 'Create assessment (draft)'}
          </Button>
        </Box>
      </Modal>

      <Modal open={Boolean(questionsModal)} title={`Questions — ${questionsModal?.title || ''}`} onClose={() => setQuestionsModal(null)}>
        <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
          <Button variant="outlined" size="small" onClick={openAddQuestion} startIcon={<AddIcon />}>
            Add question
          </Button>
          <Button variant="outlined" size="small" onClick={openAiModal}>
            Generate with AI
          </Button>
        </Box>
        {qLoading ? (
          <CircularProgress />
        ) : questions.length === 0 ? (
          <Typography color="text.secondary">No questions yet.</Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {questions.map((q) => (
              <Box key={q.id} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle2">{q.title}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  {q.difficulty} · {q.marks} marks {q.aiGenerated && '· AI generated'}
                </Typography>
                <Button variant="outlined" size="small" color="error" onClick={() => handleDeleteQuestion(q.id)}>
                  Delete
                </Button>
              </Box>
            ))}
          </Box>
        )}
      </Modal>

      <Modal open={addModalOpen} title="Add question" onClose={() => setAddModalOpen(false)}>
        <Box component="form" onSubmit={handleAddQuestion} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField label="Title" required fullWidth value={qForm.title} onChange={(e) => setQForm((f) => ({ ...f, title: e.target.value }))} />
          <TextField label="Problem statement" multiline rows={4} required fullWidth value={qForm.problemStatement} onChange={(e) => setQForm((f) => ({ ...f, problemStatement: e.target.value }))} />
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField select label="Difficulty" fullWidth value={qForm.difficulty} onChange={(e) => setQForm((f) => ({ ...f, difficulty: e.target.value }))}>
              {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
            </TextField>
            <TextField label="Marks" type="number" inputProps={{ min: 1 }} value={qForm.marks} onChange={(e) => setQForm((f) => ({ ...f, marks: e.target.value }))} />
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Allowed languages</Typography>
            <FormGroup row>
              {LANGUAGES.map((l) => (
                <FormControlLabel
                  key={l}
                  control={<Checkbox checked={qForm.allowedLanguages.includes(l)} onChange={() => toggleLang(qForm, setQForm, l)} />}
                  label={l}
                />
              ))}
            </FormGroup>
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Public test case</Typography>
            <TextField size="small" label="Input" fullWidth sx={{ mb: 1 }} value={qForm.publicTestCases[0].input} onChange={(e) => updateTestCase(qForm.publicTestCases, setQForm, 'publicTestCases', 0, 'input', e.target.value)} />
            <TextField size="small" label="Expected output" fullWidth value={qForm.publicTestCases[0].expectedOutput} onChange={(e) => updateTestCase(qForm.publicTestCases, setQForm, 'publicTestCases', 0, 'expectedOutput', e.target.value)} />
          </Box>

          <Button variant="contained" color="primary" type="submit" disabled={qSaving}>
            {qSaving ? 'Saving…' : 'Add question'}
          </Button>
        </Box>
      </Modal>

      <Modal open={aiModalOpen} title="Generate questions with AI" onClose={() => setAiModalOpen(false)}>
        {!aiPreview ? (
          <Box component="form" onSubmit={handleAiPreview} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField label="Topic" required fullWidth value={aiForm.topic} onChange={(e) => setAiForm((f) => ({ ...f, topic: e.target.value }))} />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField select label="Language" fullWidth value={aiForm.language} onChange={(e) => setAiForm((f) => ({ ...f, language: e.target.value }))}>
                {LANGUAGES.map((l) => <MenuItem key={l} value={l}>{l}</MenuItem>)}
              </TextField>
              <TextField select label="Difficulty (blank = mixed)" fullWidth value={aiForm.difficulty} onChange={(e) => setAiForm((f) => ({ ...f, difficulty: e.target.value }))}>
                <MenuItem value="">Mixed</MenuItem>
                {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </TextField>
              <TextField label="Count" type="number" inputProps={{ min: 1, max: 20 }} value={aiForm.questionCount} onChange={(e) => setAiForm((f) => ({ ...f, questionCount: e.target.value }))} />
            </Box>
            <Button variant="contained" color="primary" type="submit" disabled={aiSaving}>
              {aiSaving ? 'Generating…' : 'Preview questions'}
            </Button>
          </Box>
        ) : (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Review before saving — nothing is stored yet.</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 3, maxHeight: 300, overflowY: 'auto' }}>
              {aiPreview.map((q, i) => (
                <Box key={i} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                  <Typography variant="subtitle2" component="span">{q.title}</Typography>
                  <Typography variant="body2" component="span"> — {q.difficulty}, {q.marks} marks</Typography>
                </Box>
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button variant="contained" color="primary" onClick={handleAiSave} disabled={aiSaving}>
                {aiSaving ? 'Saving…' : `Save all ${aiPreview.length} questions`}
              </Button>
              <Button variant="outlined" onClick={() => setAiPreview(null)}>
                Discard & retry
              </Button>
            </Box>
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(analyticsModal)} title={`Analytics — ${analyticsModal?.title || ''}`} onClose={() => setAnalyticsModal(null)}>
        {!analytics ? (
          <CircularProgress />
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2">Students: {analytics.totalStudents} · Submitted: {analytics.submittedCount} · Pending: {analytics.pendingCount}</Typography>
            <Typography variant="body2">Highest: {analytics.highestScore} · Lowest: {analytics.lowestScore} · Average: {analytics.averageScore}</Typography>
            <Typography variant="body2">Pass rate: {analytics.passPercentage}% · Fail rate: {analytics.failPercentage}%</Typography>
            {analytics.leaderboard?.length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Leaderboard</Typography>
                {analytics.leaderboard.map((l, i) => (
                  <Typography key={i} variant="body2" color="text.secondary">
                    #{i + 1} {l.studentId} — {l.totalMarks} marks
                  </Typography>
                ))}
              </Box>
            )}
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
