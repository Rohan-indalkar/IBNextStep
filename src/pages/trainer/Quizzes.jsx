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
import QuizOutlinedIcon from '@mui/icons-material/QuizOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import AutorenewOutlinedIcon from '@mui/icons-material/AutorenewOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import {
  listQuizzes,
  generateQuiz,
  deleteQuiz,
  publishQuiz,
  regenerateEntireQuiz,
  addQuestion,
  editQuestion,
  deleteQuestion,
  regenerateQuestion,
  getQuizAnalytics,
} from '../../api/quiz';

const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];
const QUESTION_TYPES = ['MCQ', 'TRUE_FALSE', 'MULTIPLE_SELECT', 'FILL_BLANK', 'SHORT_ANSWER'];

const EMPTY_GEN = {
  title: '', prompt: '', topic: '', subTopics: '', difficulty: 'MEDIUM',
  questionCount: 10, durationMinutes: 20, passingPercentage: 60, batchId: '',
  language: 'English', questionTypes: ['MCQ'],
};

export default function Quizzes() {
  const { batches } = useMyBatches();
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [genModalOpen, setGenModalOpen] = useState(false);
  const [genForm, setGenForm] = useState(EMPTY_GEN);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [questionsModal, setQuestionsModal] = useState(null);
  const [questionModalIdx, setQuestionModalIdx] = useState(null);
  const [questionForm, setQuestionForm] = useState(null);
  const [qSaving, setQSaving] = useState(false);

  const [analyticsModal, setAnalyticsModal] = useState(null);
  const [analytics, setAnalytics] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    listQuizzes({ page, size: 10 })
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

  function toggleType(t) {
    setGenForm((f) => ({
      ...f,
      questionTypes: f.questionTypes.includes(t) ? f.questionTypes.filter((x) => x !== t) : [...f.questionTypes, t],
    }));
  }

  async function handleGenerate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await generateQuiz({
        title: genForm.title || null,
        prompt: genForm.prompt,
        topic: genForm.topic,
        subTopics: genForm.subTopics ? genForm.subTopics.split(',').map((s) => s.trim()).filter(Boolean) : [],
        difficulty: genForm.difficulty,
        questionCount: Number(genForm.questionCount),
        durationMinutes: Number(genForm.durationMinutes),
        passingPercentage: Number(genForm.passingPercentage),
        batchId: genForm.batchId,
        language: genForm.language,
        questionTypes: genForm.questionTypes,
      });
      setGenModalOpen(false);
      setGenForm(EMPTY_GEN);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAction(quiz, action) {
    setBusyId(quiz.id);
    setError('');
    try {
      if (action === 'publish') await publishQuiz(quiz.id);
      else if (action === 'regenerate') {
        if (!window.confirm('Regenerate the entire quiz? This replaces all questions.')) return;
        await regenerateEntireQuiz(quiz.id);
      } else if (action === 'delete') {
        if (!window.confirm(`Delete "${quiz.title}"?`)) return;
        await deleteQuiz(quiz.id);
      }
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function openQuestions(quiz) {
    setQuestionsModal(quiz);
  }

  function openEditQuestion(idx) {
    const q = questionsModal.questions[idx].question;
    setQuestionModalIdx(idx);
    setQuestionForm({ ...q, options: (q.options || []).join(', '), correctAnswers: (q.correctAnswers || []).join(', ') });
  }

  function openAddQuestion() {
    setQuestionModalIdx('new');
    setQuestionForm({ questionText: '', options: '', correctAnswer: '', correctAnswers: '', explanation: '', type: 'MCQ', difficulty: 'MEDIUM', marks: 1 });
  }

  async function handleSaveQuestion(e) {
    e.preventDefault();
    setQSaving(true);
    setError('');
    try {
      const payload = {
        questionText: questionForm.questionText,
        options: questionForm.options ? questionForm.options.split(',').map((s) => s.trim()).filter(Boolean) : [],
        correctAnswer: questionForm.correctAnswer || null,
        correctAnswers: questionForm.correctAnswers ? questionForm.correctAnswers.split(',').map((s) => s.trim()).filter(Boolean) : [],
        explanation: questionForm.explanation || null,
        type: questionForm.type,
        difficulty: questionForm.difficulty,
        marks: Number(questionForm.marks),
      };
      let res;
      if (questionModalIdx === 'new') {
        res = await addQuestion(questionsModal.id, payload);
      } else {
        const order = questionsModal.questions[questionModalIdx].order;
        res = await editQuestion(questionsModal.id, order, payload);
      }
      setQuestionsModal(res.data.data);
      setQuestionModalIdx(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setQSaving(false);
    }
  }

  async function handleDeleteQuestion(idx) {
    if (!window.confirm('Delete this question?')) return;
    const order = questionsModal.questions[idx].order;
    try {
      const res = await deleteQuestion(questionsModal.id, order);
      setQuestionsModal(res.data.data);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRegenerateQuestion(idx) {
    const order = questionsModal.questions[idx].order;
    const instruction = window.prompt('Additional instruction (optional):') || '';
    try {
      const res = await regenerateQuestion(questionsModal.id, order, instruction ? { additionalInstruction: instruction } : null);
      setQuestionsModal(res.data.data);
    } catch (err) {
      setError(err.message);
    }
  }

  function openAnalytics(quiz) {
    setAnalyticsModal(quiz);
    setAnalytics(null);
    getQuizAnalytics(quiz.id)
      .then((res) => setAnalytics(res.data.data))
      .catch((err) => setError(err.message));
  }

  const batchNameById = Object.fromEntries(batches.map((b) => [b.id, b.name]));

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'topic', header: 'Topic' },
    { key: 'batch', header: 'Batch', render: (r) => batchNameById[r.batchId] || r.batchId },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'questionCount', header: 'Questions' },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'View questions', icon: <QuizOutlinedIcon fontSize="small" />, onClick: () => openQuestions(r) },
            { label: 'View analytics', icon: <BarChartOutlinedIcon fontSize="small" />, onClick: () => openAnalytics(r) },
            r.status === 'DRAFT' && { label: 'Publish quiz', icon: <PublishOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handleAction(r, 'publish') },
            r.status === 'DRAFT' && { label: 'Regenerate all questions', icon: <AutorenewOutlinedIcon fontSize="small" />, disabled: busyId === r.id, onClick: () => handleAction(r, 'regenerate') },
            { label: 'Delete quiz', icon: <DeleteOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handleAction(r, 'delete') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Quizzes"
        subtitle="Create, manage, and AI-generate quizzes for your batches."
        actions={
          <Button variant="contained" color="primary" onClick={() => setGenModalOpen(true)}>
            + Generate quiz with AI
          </Button>
        }
      />

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No quizzes yet." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={genModalOpen} title="Generate quiz with AI" onClose={() => setGenModalOpen(false)}>
        <Box component="form" onSubmit={handleGenerate} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField label="Title (optional)" fullWidth value={genForm.title} onChange={(e) => setGenForm((f) => ({ ...f, title: e.target.value }))} />
          <TextField label="Prompt" required fullWidth value={genForm.prompt} onChange={(e) => setGenForm((f) => ({ ...f, prompt: e.target.value }))} placeholder="Instructions for the AI" />
          <TextField label="Topic" required fullWidth value={genForm.topic} onChange={(e) => setGenForm((f) => ({ ...f, topic: e.target.value }))} />
          <TextField label="Sub-topics (comma-separated)" fullWidth value={genForm.subTopics} onChange={(e) => setGenForm((f) => ({ ...f, subTopics: e.target.value }))} />
          
          <TextField select label="Batch" required fullWidth value={genForm.batchId} onChange={(e) => setGenForm((f) => ({ ...f, batchId: e.target.value }))} slotProps={{ inputLabel: { shrink: true } }}>
            <MenuItem value="" disabled>Select a batch…</MenuItem>
            {batches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </TextField>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField select label="Difficulty" fullWidth value={genForm.difficulty} onChange={(e) => setGenForm((f) => ({ ...f, difficulty: e.target.value }))}>
              {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
            </TextField>
            <TextField label="Questions" type="number" inputProps={{ min: 1, max: 100 }} value={genForm.questionCount} onChange={(e) => setGenForm((f) => ({ ...f, questionCount: e.target.value }))} />
          </Box>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Duration (min)" type="number" inputProps={{ min: 1 }} fullWidth value={genForm.durationMinutes} onChange={(e) => setGenForm((f) => ({ ...f, durationMinutes: e.target.value }))} />
            <TextField label="Passing %" type="number" inputProps={{ min: 0, max: 100 }} fullWidth value={genForm.passingPercentage} onChange={(e) => setGenForm((f) => ({ ...f, passingPercentage: e.target.value }))} />
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Question types</Typography>
            <FormGroup row>
              {QUESTION_TYPES.map((t) => (
                <FormControlLabel
                  key={t}
                  control={<Checkbox checked={genForm.questionTypes.includes(t)} onChange={() => toggleType(t)} />}
                  label={t.replace('_', ' ')}
                />
              ))}
            </FormGroup>
          </Box>

          {formError && <Alert severity="error">{formError}</Alert>}
          <Button variant="contained" color="primary" type="submit" disabled={saving}>
            {saving ? 'Generating…' : 'Generate quiz'}
          </Button>
        </Box>
      </Modal>

      <Modal open={Boolean(questionsModal)} title={`Questions — ${questionsModal?.title || ''}`} onClose={() => setQuestionsModal(null)}>
        <Button variant="outlined" size="small" onClick={openAddQuestion} sx={{ mb: 2 }} startIcon={<AddIcon />}>
          Add question
        </Button>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {(questionsModal?.questions || []).map((entry, idx) => (
            <Box key={idx} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                {entry.order}. {entry.question.questionText}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                {entry.question.type} · {entry.question.marks} marks
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button variant="outlined" size="small" onClick={() => openEditQuestion(idx)}>Edit</Button>
                <Button variant="outlined" size="small" onClick={() => handleRegenerateQuestion(idx)}>Regenerate</Button>
                <Button variant="outlined" size="small" color="error" onClick={() => handleDeleteQuestion(idx)}>Delete</Button>
              </Box>
            </Box>
          ))}
        </Box>
      </Modal>

      <Modal open={questionModalIdx !== null} title={questionModalIdx === 'new' ? 'Add question' : 'Edit question'} onClose={() => setQuestionModalIdx(null)}>
        {questionForm && (
          <Box component="form" onSubmit={handleSaveQuestion} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField label="Question text" required fullWidth value={questionForm.questionText} onChange={(e) => setQuestionForm((f) => ({ ...f, questionText: e.target.value }))} />
            
            <TextField select label="Type" fullWidth value={questionForm.type} onChange={(e) => setQuestionForm((f) => ({ ...f, type: e.target.value }))}>
              {QUESTION_TYPES.map((t) => <MenuItem key={t} value={t}>{t.replace('_', ' ')}</MenuItem>)}
            </TextField>
            
            <TextField label="Options (comma-separated)" fullWidth value={questionForm.options} onChange={(e) => setQuestionForm((f) => ({ ...f, options: e.target.value }))} />
            <TextField label="Correct answer" fullWidth value={questionForm.correctAnswer} onChange={(e) => setQuestionForm((f) => ({ ...f, correctAnswer: e.target.value }))} />
            <TextField label="Correct answers (MULTIPLE_SELECT only, comma-separated)" fullWidth value={questionForm.correctAnswers} onChange={(e) => setQuestionForm((f) => ({ ...f, correctAnswers: e.target.value }))} />
            <TextField label="Explanation" fullWidth value={questionForm.explanation} onChange={(e) => setQuestionForm((f) => ({ ...f, explanation: e.target.value }))} />
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField select label="Difficulty" fullWidth value={questionForm.difficulty} onChange={(e) => setQuestionForm((f) => ({ ...f, difficulty: e.target.value }))}>
                {DIFFICULTIES.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </TextField>
              <TextField label="Marks" type="number" inputProps={{ min: 1 }} value={questionForm.marks} onChange={(e) => setQuestionForm((f) => ({ ...f, marks: e.target.value }))} />
            </Box>

            {error && <Alert severity="error">{error}</Alert>}
            <Button variant="contained" color="primary" type="submit" disabled={qSaving}>
              {qSaving ? 'Saving…' : 'Save question'}
            </Button>
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(analyticsModal)} title={`Analytics — ${analyticsModal?.title || ''}`} onClose={() => setAnalyticsModal(null)}>
        {!analytics ? (
          <CircularProgress />
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2">Assigned: {analytics.totalAssigned} · Attempted: {analytics.attemptedCount} · Pending: {analytics.pendingCount}</Typography>
            <Typography variant="body2">Highest: {analytics.highestScore} · Lowest: {analytics.lowestScore} · Average: {analytics.averageScore}</Typography>
            <Typography variant="body2">Pass rate: {analytics.passPercentage}% · Fail rate: {analytics.failPercentage}%</Typography>
            {analytics.leaderboard?.length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Leaderboard</Typography>
                {analytics.leaderboard.map((l, i) => (
                  <Typography key={i} variant="body2" color="text.secondary">
                    #{i + 1} {l.studentId} — {l.percentage}%
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
