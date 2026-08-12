import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import States from '../../components/States';
import { listAvailableAssignments, getAssignmentDetail, submitAssignment, downloadReferenceFile } from '../../api/studentAssignment';
import { Box, Typography, Button, TextField, Stack, Card, CardContent, Divider, IconButton } from '@mui/material';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';

export default function Assignments() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [detail, setDetail] = useState(null);
  const [answers, setAnswers] = useState({});
  const [files, setFiles] = useState([]);
  const [saving, setSaving] = useState(false);

  function load() {
    setLoading(true);
    listAvailableAssignments()
      .then((res) => setItems(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function openDetail(item) {
    setError('');
    getAssignmentDetail(item.assignmentId)
      .then((res) => {
        setDetail(res.data.data);
        const initial = {};
        (res.data.data.questions || []).forEach((q) => {
          initial[q.id] = res.data.data.mySubmission?.answers?.find((a) => a.questionId === q.id)?.textAnswer || '';
        });
        setAnswers(initial);
        setFiles([]);
      })
      .catch((err) => setError(err.message));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = {
        answers: detail.questions.map((q) => ({ questionId: q.id, textAnswer: answers[q.id] || '' })),
      };
      await submitAssignment(detail.assignmentId, payload, files);
      setDetail(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDownloadRef(file) {
    try {
      await downloadReferenceFile(detail.assignmentId, file.fileId, file.fileName);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Assignments"
        subtitle="Complete and submit course assignments for your batch."
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : items.length === 0 ? (
        <States state="empty" message="No assignments yet." />
      ) : (
        <Card>
          <Stack divider={<Divider flexItem />}>
            {items.map((a) => (
              <Box key={a.assignmentId} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2 }}>
                <Box>
                  <Typography variant="subtitle1" fontWeight="bold">{a.title}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {a.courseName} · {a.questionCount} questions {a.dueDate && `· due ${new Date(a.dueDate).toLocaleDateString()}`}
                    {a.rating != null && ` · rated ${a.rating}/5`}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <StatusBadge status={a.submissionStatus || 'Not submitted'} />
                  <Button variant={a.submissionStatus ? 'outlined' : 'contained'} size="small" onClick={() => openDetail(a)}>
                    {a.submissionStatus ? 'View' : 'Open'}
                  </Button>
                </Box>
              </Box>
            ))}
          </Stack>
        </Card>
      )}

      <Modal open={Boolean(detail)} title={detail?.title || ''} onClose={() => setDetail(null)}>
        {detail && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {detail.description && <Typography variant="body2" color="text.secondary">{detail.description}</Typography>}

            {detail.referenceFiles?.length > 0 && (
              <Box>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Reference files</Typography>
                <Stack spacing={1}>
                  {detail.referenceFiles.map((f) => (
                    <Button key={f.fileId} variant="text" color="inherit" startIcon={<CloudDownloadIcon />} sx={{ justifyContent: 'flex-start', textTransform: 'none' }} onClick={() => handleDownloadRef(f)}>
                      {f.fileName}
                    </Button>
                  ))}
                </Stack>
              </Box>
            )}

            {detail.mySubmission ? (
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Your submission — {detail.mySubmission.status}</Typography>
                {detail.mySubmission.answers.map((a, i) => (
                  <Typography key={i} variant="body2" sx={{ mb: 1 }}>{a.textAnswer}</Typography>
                ))}
                {detail.mySubmission.rating != null && <Typography variant="body2" color="success.main" fontWeight="bold" sx={{ mt: 1 }}>Rated {detail.mySubmission.rating}/5</Typography>}
                {detail.mySubmission.feedback && <Typography variant="body2" sx={{ mt: 0.5 }}><strong>Feedback:</strong> {detail.mySubmission.feedback}</Typography>}
              </Box>
            ) : (
              <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {detail.questions.map((q) => (
                  <TextField
                    key={q.id}
                    label={q.questionText}
                    multiline
                    minRows={3}
                    fullWidth
                    value={answers[q.id] || ''}
                    onChange={(e) => setAnswers((a) => ({ ...a, [q.id]: e.target.value }))}
                  />
                ))}
                <Box>
                  <Typography variant="body2" sx={{ mb: 1 }}>Attach file(s) (optional)</Typography>
                  <input type="file" multiple onChange={(e) => setFiles(Array.from(e.target.files || []))} />
                </Box>
                <Button type="submit" variant="contained" disabled={saving} fullWidth>
                  {saving ? 'Submitting…' : 'Submit assignment'}
                </Button>
              </form>
            )}
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
