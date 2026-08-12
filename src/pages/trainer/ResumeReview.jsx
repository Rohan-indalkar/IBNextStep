import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Card,
  CircularProgress,
  Alert
} from '@mui/material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { searchUsers } from '../../api/user';
import {
  listResumes,
  getStudentResume,
  analyzeResume,
  autoReview,
  autoReviewAll,
  reviewResume,
  downloadResumeFile,
} from '../../api/resume';

const STATUSES = ['', 'PENDING_REVIEW', 'NEEDS_CHANGES', 'APPROVED'];

export default function ResumeReview() {
  const [students, setStudents] = useState([]);
  const [resumes, setResumes] = useState([]);
  const [statusFilter, setStatusFilter] = useState('PENDING_REVIEW');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bulkRunning, setBulkRunning] = useState(false);
  const [bulkResult, setBulkResult] = useState(null);

  const [detailModal, setDetailModal] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [busy, setBusy] = useState(false);

  const [suggestions, setSuggestions] = useState('');
  const [score, setScore] = useState(70);
  const [reviewStatus, setReviewStatus] = useState('APPROVED');

  function load() {
    setLoading(true);
    setError('');
    listResumes(statusFilter || undefined)
      .then((res) => setResumes(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, [statusFilter]);

  useEffect(() => {
    searchUsers({ role: 'STUDENT', page: 0, size: 200 })
      .then((res) => setStudents(res.data.data.content))
      .catch(() => {});
  }, []);

  const studentNameById = Object.fromEntries(students.map((s) => [s.id, `${s.firstName} ${s.lastName}`]));

  function openDetail(resume) {
    setDetailModal(resume);
    setAnalysis(null);
    const latest = resume.versions[resume.versions.length - 1];
    setSuggestions(latest?.suggestions || '');
    setScore(latest?.score ?? 70);
    setReviewStatus('APPROVED');
  }

  async function handleAnalyze(refresh = false) {
    setAnalyzing(true);
    setError('');
    try {
      const res = await analyzeResume(detailModal.studentId, refresh);
      setAnalysis(res.data.data);
      if (res.data.data.suggestedReviewText) setSuggestions(res.data.data.suggestedReviewText);
      if (res.data.data.overallScore != null) setScore(res.data.data.overallScore);
    } catch (err) {
      setError(err.message);
    } finally {
      setAnalyzing(false);
    }
  }

  async function handleAutoReview() {
    setBusy(true);
    setError('');
    try {
      await autoReview(detailModal.studentId, false);
      setDetailModal(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleManualReview(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await reviewResume(detailModal.studentId, { suggestions, score: Number(score), status: reviewStatus });
      setDetailModal(null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleDownload() {
    try {
      const latest = detailModal.versions[detailModal.versions.length - 1];
      await downloadResumeFile(detailModal.studentId, latest.fileName);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAutoReviewAll() {
    if (!window.confirm('Auto-review every pending resume in your batches? Each student will be notified.')) return;
    setBulkRunning(true);
    setBulkResult(null);
    setError('');
    try {
      const res = await autoReviewAll(false);
      setBulkResult(res.data.data);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBulkRunning(false);
    }
  }

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader 
        title="Resume review" 
        action={
          <Button variant="contained" color="primary" onClick={handleAutoReviewAll} disabled={bulkRunning}>
            {bulkRunning ? 'Running…' : 'Auto-review all pending'}
          </Button>
        }
      />

      {bulkResult && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Processed {bulkResult.length} resumes — {bulkResult.filter((r) => r.success).length} succeeded.
        </Typography>
      )}

      <Box sx={{ mb: 3 }}>
        <TextField select label="Status filter" size="small" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} sx={{ minWidth: 200 }} slotProps={{ inputLabel: { shrink: true } }}>
          {STATUSES.map((s) => (
            <MenuItem key={s} value={s}>{s || 'All statuses'}</MenuItem>
          ))}
        </TextField>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <CircularProgress />
      ) : resumes.length === 0 ? (
        <Typography color="text.secondary">No resumes found for this filter.</Typography>
      ) : (
        <Card sx={{ overflow: 'hidden' }}>
          {resumes.map((r, i) => {
            const latest = r.versions[r.versions.length - 1];
            return (
              <Box 
                key={r.id} 
                sx={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center', 
                  p: 2,
                  borderBottom: i < resumes.length - 1 ? '1px solid' : 'none',
                  borderColor: 'divider'
                }}
              >
                <Box>
                  <Typography variant="subtitle2">{studentNameById[r.studentId] || r.studentId}</Typography>
                  <Typography variant="body2" color="text.secondary">Version {latest?.versionNumber} · {latest?.fileName}</Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <StatusBadge status={r.currentStatus} />
                  <Button variant="outlined" size="small" onClick={() => openDetail(r)}>
                    Review
                  </Button>
                </Box>
              </Box>
            );
          })}
        </Card>
      )}

      <Modal open={Boolean(detailModal)} title={`Resume — ${studentNameById[detailModal?.studentId] || ''}`} onClose={() => setDetailModal(null)}>
        {detailModal && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Button variant="outlined" size="small" sx={{ alignSelf: 'flex-start' }} onClick={handleDownload}>
              Download resume file
            </Button>

            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <Button variant="outlined" size="small" onClick={() => handleAnalyze(false)} disabled={analyzing}>
                {analyzing ? 'Analyzing…' : 'Analyze with AI'}
              </Button>
              {analysis && (
                <Button variant="outlined" size="small" onClick={() => handleAnalyze(true)} disabled={analyzing}>
                  Refresh analysis
                </Button>
              )}
              <Button variant="outlined" size="small" onClick={handleAutoReview} disabled={busy}>
                Auto-review with AI
              </Button>
            </Box>

            {analysis && (
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>AI score: {analysis.overallScore ?? 'N/A'}/100</Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>{analysis.summary}</Typography>
                {analysis.strengths?.length > 0 && <Typography variant="body2"><strong>Strengths:</strong> {analysis.strengths.join(', ')}</Typography>}
                {analysis.weaknesses?.length > 0 && <Typography variant="body2"><strong>Weaknesses:</strong> {analysis.weaknesses.join(', ')}</Typography>}
                {analysis.missingSections?.length > 0 && <Typography variant="body2"><strong>Missing:</strong> {analysis.missingSections.join(', ')}</Typography>}
                {analysis.atsIssues?.length > 0 && <Typography variant="body2"><strong>ATS issues:</strong> {analysis.atsIssues.join(', ')}</Typography>}
              </Box>
            )}

            <Box component="form" onSubmit={handleManualReview} sx={{ display: 'flex', flexDirection: 'column', gap: 2, borderTop: '1px solid', borderColor: 'divider', pt: 2 }}>
              <Typography variant="subtitle2">Manual review</Typography>
              
              <TextField label="Suggestions" multiline rows={4} required fullWidth value={suggestions} onChange={(e) => setSuggestions(e.target.value)} />
              
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField label="Score (0-100)" type="number" inputProps={{ min: 0, max: 100 }} required fullWidth value={score} onChange={(e) => setScore(e.target.value)} />
                <TextField select label="Status" fullWidth value={reviewStatus} onChange={(e) => setReviewStatus(e.target.value)}>
                  <MenuItem value="APPROVED">Approved</MenuItem>
                  <MenuItem value="NEEDS_CHANGES">Needs changes</MenuItem>
                </TextField>
              </Box>
              
              <Button variant="contained" color="primary" type="submit" disabled={busy}>
                {busy ? 'Submitting…' : 'Submit review'}
              </Button>
            </Box>
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
