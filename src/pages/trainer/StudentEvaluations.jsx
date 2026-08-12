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
import ActionMenu from '../../components/ActionMenu';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined';
import useMyBatches from '../../hooks/useMyBatches';
import {
  getRubric,
  getBatchOverview,
  getMetrics,
  submitEvaluation,
  exportPdf,
  exportExcel,
} from '../../api/studentEvaluation';

export default function StudentEvaluations() {
  const { batches } = useMyBatches();
  const [batchId, setBatchId] = useState('');
  const [overview, setOverview] = useState(null);
  const [rubric, setRubric] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [evalModal, setEvalModal] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [scores, setScores] = useState({});
  const [remarks, setRemarks] = useState('');
  const [overrideFinal, setOverrideFinal] = useState('');
  const [overrideReason, setOverrideReason] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getRubric()
      .then((res) => setRubric(res.data.data))
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (batches.length && !batchId) setBatchId(batches[0].id);
  }, [batches, batchId]);

  useEffect(() => {
    if (!batchId) return;
    setLoading(true);
    setError('');
    getBatchOverview(batchId)
      .then((res) => setOverview(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [batchId]);

  function openEvaluate(student) {
    setEvalModal(student);
    setMetrics(null);
    setRemarks('');
    setOverrideFinal('');
    setOverrideReason('');
    const initial = {};
    (rubric?.skills || []).forEach((s) => (initial[s] = 5));
    setScores(initial);
    getMetrics(student.studentId)
      .then((res) => setMetrics(res.data.data))
      .catch((err) => setError(err.message));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (overrideFinal !== '' && Boolean(overrideFinal === 'true') !== metrics?.systemEligible && !overrideReason) {
      setError('An override reason is required when your decision differs from the system verdict.');
      return;
    }
    setSaving(true);
    try {
      await submitEvaluation(evalModal.studentId, {
        skillScores: scores,
        remarks: remarks || null,
        finalEligibleOverride: overrideFinal === '' ? null : overrideFinal === 'true',
        overrideReason: overrideReason || null,
      });
      setEvalModal(null);
      getBatchOverview(batchId).then((res) => setOverview(res.data.data));
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader title="Student evaluations" />

      <Box sx={{ mb: 3 }}>
        <TextField select label="Batch" size="small" value={batchId} onChange={(e) => setBatchId(e.target.value)} sx={{ minWidth: 200 }} slotProps={{ inputLabel: { shrink: true } }}>
          <MenuItem value="" disabled>Select a batch…</MenuItem>
          {batches.map((b) => (
            <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>
          ))}
        </TextField>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <CircularProgress />
      ) : overview ? (
        <Card sx={{ p: 3 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {overview.evaluatedByMe} of {overview.totalStudents} evaluated by you · {overview.pendingEvaluationByMe} pending
          </Typography>
          
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {overview.students.map((s) => (
              <Box 
                key={s.studentId} 
                sx={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center', 
                  p: 2, 
                  bgcolor: 'background.default', 
                  borderRadius: 1 
                }}
              >
                <Box>
                  <Typography variant="subtitle2">{s.studentName}</Typography>
                  <Typography variant="body2" color={s.systemEligible ? 'success.main' : 'text.secondary'}>
                    {s.systemEligible ? 'System eligible' : 'Not yet eligible'}
                    {s.evaluatedByMyRubric && ` · last scored ${s.lastOverallRubricScore ?? '—'}/10`}
                  </Typography>
                </Box>

                <ActionMenu
                  items={[
                    { label: s.evaluatedByMyRubric ? 'Re-evaluate student' : 'Evaluate student', icon: <RateReviewOutlinedIcon fontSize="small" />, color: 'primary', onClick: () => openEvaluate(s) },
                    s.lastEvaluationId && { label: 'Export PDF report', icon: <PictureAsPdfOutlinedIcon fontSize="small" />, onClick: () => exportPdf(s.lastEvaluationId).catch((err) => setError(err.message)) },
                    s.lastEvaluationId && { label: 'Export Excel report', icon: <TableChartOutlinedIcon fontSize="small" />, onClick: () => exportExcel(s.lastEvaluationId).catch((err) => setError(err.message)) },
                  ]}
                />
              </Box>
            ))}
          </Box>
        </Card>
      ) : (
        <Typography color="text.secondary">Select a batch to view its roster.</Typography>
      )}

      <Modal open={Boolean(evalModal)} title={`Evaluate — ${evalModal?.studentName || ''}`} onClose={() => setEvalModal(null)}>
        {!metrics ? (
          <CircularProgress />
        ) : (
          <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
              <Typography variant="body2">Attendance: {metrics.attendancePercentage ?? 'N/A'}% (min {metrics.minAttendancePercentage}%)</Typography>
              <Typography variant="body2">Avg quiz: {metrics.avgQuizPercentage ?? 'N/A'}% (min {metrics.minQuizPercentage}%)</Typography>
              <Typography variant="body2">Avg coding: {metrics.avgCodingPercentage ?? 'N/A'}% (min {metrics.minCodingPercentage}%)</Typography>
              <Typography variant="body2">Avg mock interview: {metrics.avgMockInterviewRating ?? 'N/A'} (min {metrics.minMockInterviewRating})</Typography>
              <Typography variant="subtitle2" color={metrics.systemEligible ? 'success.main' : 'error.main'} sx={{ mt: 1 }}>
                System verdict: {metrics.systemEligible ? 'Eligible' : 'Not eligible'}
              </Typography>
              {metrics.systemIneligibilityReasons?.length > 0 && (
                <Box component="ul" sx={{ mt: 1, pl: 2, m: 0 }}>
                  {metrics.systemIneligibilityReasons.map((r, i) => (
                    <Typography component="li" variant="body2" key={i}>{r}</Typography>
                  ))}
                </Box>
              )}
            </Box>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Rubric scores (0-10)</Typography>
              {(rubric?.skills || []).map((skill) => (
                <Box key={skill} sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                  <Typography variant="body2" sx={{ flex: 1 }}>{skill}</Typography>
                  <TextField type="number" inputProps={{ min: 0, max: 10 }} size="small" sx={{ width: 80 }} value={scores[skill] ?? 5} onChange={(e) => setScores((s) => ({ ...s, [skill]: Number(e.target.value) }))} />
                </Box>
              ))}
            </Box>

            <TextField label="Remarks" multiline rows={3} fullWidth value={remarks} onChange={(e) => setRemarks(e.target.value)} />

            <TextField select label="Final decision" fullWidth value={overrideFinal} onChange={(e) => setOverrideFinal(e.target.value)}>
              <MenuItem value="">Agree with system verdict</MenuItem>
              <MenuItem value="true">Override: Eligible</MenuItem>
              <MenuItem value="false">Override: Not eligible</MenuItem>
            </TextField>
            
            {overrideFinal !== '' && (
              <TextField label="Override reason" required fullWidth value={overrideReason} onChange={(e) => setOverrideReason(e.target.value)} />
            )}

            {error && <Alert severity="error">{error}</Alert>}
            <Button variant="contained" color="primary" type="submit" disabled={saving}>
              {saving ? 'Submitting…' : 'Submit evaluation'}
            </Button>
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
