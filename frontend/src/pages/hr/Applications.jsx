import { useEffect, useState, useCallback } from 'react';
import AppShell from '../../components/AppShell';
import HrSidebar from '../../components/HrSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import ActionMenu from '../../components/ActionMenu';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import StarOutlinedIcon from '@mui/icons-material/StarOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import { searchPlacements } from '../../api/placement';
import {
  searchApplications,
  getApplication,
  shortlistApplication,
  rejectApplication,
  selectApplication,
  notSelectApplication,
  scheduleRound,
  rescheduleRound,
  cancelRound,
  updateRoundResult,
} from '../../api/application';
import { Box, Typography, Button, TextField, Select, MenuItem, Chip, FormControl, InputLabel } from '@mui/material';

const STATUSES = ['', 'APPLIED', 'SHORTLISTED', 'REJECTED', 'INTERVIEW_SCHEDULED', 'SELECTED', 'NOT_SELECTED'];
const RESULTS = ['PENDING', 'QUALIFIED', 'REJECTED'];

function StatusBadge({ status }) {
  const colors = { APPLIED: 'default', SHORTLISTED: 'warning', REJECTED: 'error', INTERVIEW_SCHEDULED: 'info', SELECTED: 'success', NOT_SELECTED: 'error' };
  return <Chip label={status.replace('_', ' ')} color={colors[status] || 'default'} size="small" sx={{ fontWeight: 600 }} />;
}

export default function Applications() {
  const [placements, setPlacements] = useState([]);
  const [placementFilter, setPlacementFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [detailModal, setDetailModal] = useState(null);
  const [scheduleForm, setScheduleForm] = useState(null);
  const [scheduleSaving, setScheduleSaving] = useState(false);
  const [resultRound, setResultRound] = useState(null);
  const [resultForm, setResultForm] = useState({ result: 'QUALIFIED', resultRemarks: '' });

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    searchApplications({ placementId: placementFilter || undefined, status: statusFilter || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [placementFilter, statusFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    searchPlacements({ page: 0, size: 100 })
      .then((res) => setPlacements(res.data.data.content))
      .catch(() => {});
  }, []);

  async function reloadDetail(id) {
    const res = await getApplication(id);
    setDetailModal(res.data.data);
  }

  async function handlePipelineAction(app, action) {
    setBusyId(app.id);
    setError('');
    try {
      if (action === 'shortlist') await shortlistApplication(app.id);
      else if (action === 'select') await selectApplication(app.id);
      else if (action === 'reject') {
        const reason = window.prompt('Rejection reason:');
        if (!reason) return;
        await rejectApplication(app.id, reason);
      } else if (action === 'not-select') {
        const reason = window.prompt('Reason for not selecting:');
        if (!reason) return;
        await notSelectApplication(app.id, reason);
      }
      load();
      if (detailModal?.id === app.id) reloadDetail(app.id);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function openSchedule(app) {
    setScheduleForm({ roundType: '', scheduledAt: '', durationMinutes: 30, venue: '', meetingLink: '', remarks: '' });
  }

  async function handleScheduleSubmit(e) {
    e.preventDefault();
    setScheduleSaving(true);
    setError('');
    try {
      await scheduleRound(detailModal.id, {
        roundType: scheduleForm.roundType,
        scheduledAt: new Date(scheduleForm.scheduledAt).toISOString(),
        durationMinutes: scheduleForm.durationMinutes ? Number(scheduleForm.durationMinutes) : null,
        venue: scheduleForm.venue || null,
        meetingLink: scheduleForm.meetingLink || null,
        remarks: scheduleForm.remarks || null,
      });
      setScheduleForm(null);
      reloadDetail(detailModal.id);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setScheduleSaving(false);
    }
  }

  async function handleCancelRound(roundNumber) {
    const reason = window.prompt('Cancellation reason:');
    if (!reason) return;
    try {
      await cancelRound(detailModal.id, roundNumber, reason);
      reloadDetail(detailModal.id);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  function openResult(round) {
    setResultRound(round);
    setResultForm({ result: 'QUALIFIED', resultRemarks: '' });
  }

  async function handleResultSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await updateRoundResult(detailModal.id, resultRound.roundNumber, resultForm);
      setResultRound(null);
      reloadDetail(detailModal.id);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const columns = [
    { key: 'student', header: 'Student', render: (r) => r.studentName },
    { key: 'placement', header: 'Drive', render: (r) => r.placementTitle },
    { key: 'company', header: 'Company', render: (r) => r.companyName },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Application details', icon: <InfoOutlinedIcon fontSize="small" />, onClick: () => setDetailModal(r) },
            r.status === 'APPLIED' && { label: 'Shortlist candidate', icon: <StarOutlinedIcon fontSize="small" />, color: 'primary', disabled: busyId === r.id, onClick: () => handlePipelineAction(r, 'shortlist') },
            r.status === 'APPLIED' && { label: 'Reject candidate', icon: <CancelOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handlePipelineAction(r, 'reject') },
            (r.status === 'SHORTLISTED' || r.status === 'INTERVIEW_SCHEDULED') && { label: 'Select candidate', icon: <CheckCircleOutlinedIcon fontSize="small" />, color: 'success', disabled: busyId === r.id, onClick: () => handlePipelineAction(r, 'select') },
            (r.status === 'SHORTLISTED' || r.status === 'INTERVIEW_SCHEDULED') && { label: 'Not select', icon: <CancelOutlinedIcon fontSize="small" />, color: 'error', disabled: busyId === r.id, onClick: () => handlePipelineAction(r, 'not-select') },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="HR / Recruiter" sidebar={<HrSidebar />}>
      <PageHeader
        title="Applications"
        subtitle="Track student job applications and advance candidates through recruitment stages."
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="placement-filter-label">Filter by Drive</InputLabel>
          <Select
            labelId="placement-filter-label"
            value={placementFilter}
            label="Filter by Drive"
            onChange={(e) => { setPage(0); setPlacementFilter(e.target.value); }}
          >
            <MenuItem value="">All drives</MenuItem>
            {placements.map((p) => <MenuItem key={p.id} value={p.id}>{p.title}</MenuItem>)}
          </Select>
        </FormControl>
        
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel id="status-filter-label">Status</InputLabel>
          <Select
            labelId="status-filter-label"
            value={statusFilter}
            label="Status"
            onChange={(e) => { setPage(0); setStatusFilter(e.target.value); }}
          >
            {STATUSES.map((s) => <MenuItem key={s} value={s}>{s ? s.replace('_', ' ') : 'All statuses'}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No applications found." page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal open={Boolean(detailModal)} title={`${detailModal?.studentName || ''} — ${detailModal?.placementTitle || ''}`} onClose={() => setDetailModal(null)}>
        {detailModal && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, pt: 1 }}>
            <Box sx={{ color: 'text.secondary' }}>
              <Typography variant="body2">{detailModal.studentEmail} · {detailModal.departmentName}</Typography>
              <Typography variant="body2">Applied {detailModal.appliedAt ? new Date(detailModal.appliedAt).toLocaleDateString() : '—'}</Typography>
              {detailModal.rejectionReason && <Typography variant="body2" color="error">Reason: {detailModal.rejectionReason}</Typography>}
            </Box>

            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>Interview rounds</Typography>
                <Button variant="outlined" size="small" onClick={() => openSchedule(detailModal)}>
                  + Schedule round
                </Button>
              </Box>
              {(detailModal.rounds || []).length === 0 ? (
                <Typography variant="body2" color="text.secondary">No rounds scheduled yet.</Typography>
              ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  {detailModal.rounds.map((r) => (
                    <Box key={r.roundNumber} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>Round {r.roundNumber}: {r.roundType}</Typography>
                        <Typography variant="body2">{r.status}</Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary">{r.scheduledAt ? new Date(r.scheduledAt).toLocaleString() : '—'} {r.venue && `· ${r.venue}`}</Typography>
                      {r.result && r.result !== 'PENDING' && <Typography variant="body2" sx={{ mt: 1 }}>Result: {r.result} {r.resultRemarks && `— ${r.resultRemarks}`}</Typography>}
                      <Box sx={{ display: 'flex', gap: 1, mt: 1.5 }}>
                        <Button variant="text" size="small" onClick={() => openResult(r)}>Set result</Button>
                        <Button variant="text" size="small" color="error" onClick={() => handleCancelRound(r.roundNumber)}>Cancel</Button>
                      </Box>
                    </Box>
                  ))}
                </Box>
              )}
            </Box>
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(scheduleForm)} title="Schedule interview round" onClose={() => setScheduleForm(null)}>
        {scheduleForm && (
          <Box component="form" onSubmit={handleScheduleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField label="Round type" required fullWidth value={scheduleForm.roundType} onChange={(e) => setScheduleForm((f) => ({ ...f, roundType: e.target.value }))} placeholder="e.g. Technical Interview" />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField label="Scheduled for" type="datetime-local" slotProps={{ inputLabel: { shrink: true } }} required fullWidth value={scheduleForm.scheduledAt} onChange={(e) => setScheduleForm((f) => ({ ...f, scheduledAt: e.target.value }))} />
              <TextField label="Duration (min)" type="number" sx={{ width: 150 }} value={scheduleForm.durationMinutes} onChange={(e) => setScheduleForm((f) => ({ ...f, durationMinutes: e.target.value }))} />
            </Box>
            <TextField label="Venue" fullWidth value={scheduleForm.venue} onChange={(e) => setScheduleForm((f) => ({ ...f, venue: e.target.value }))} />
            <TextField label="Meeting link" fullWidth value={scheduleForm.meetingLink} onChange={(e) => setScheduleForm((f) => ({ ...f, meetingLink: e.target.value }))} />
            <TextField label="Remarks" fullWidth value={scheduleForm.remarks} onChange={(e) => setScheduleForm((f) => ({ ...f, remarks: e.target.value }))} />
            <Button variant="contained" color="primary" type="submit" disabled={scheduleSaving} sx={{ mt: 1 }}>
              {scheduleSaving ? 'Scheduling…' : 'Schedule'}
            </Button>
          </Box>
        )}
      </Modal>

      <Modal open={Boolean(resultRound)} title={`Result — Round ${resultRound?.roundNumber || ''}`} onClose={() => setResultRound(null)}>
        <Box component="form" onSubmit={handleResultSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          <FormControl fullWidth>
            <InputLabel id="rresult-label">Result</InputLabel>
            <Select
              labelId="rresult-label"
              id="rresult"
              value={resultForm.result}
              label="Result"
              onChange={(e) => setResultForm((f) => ({ ...f, result: e.target.value }))}
            >
              {RESULTS.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Remarks" fullWidth value={resultForm.resultRemarks} onChange={(e) => setResultForm((f) => ({ ...f, resultRemarks: e.target.value }))} />
          <Button variant="contained" color="primary" type="submit" sx={{ mt: 1 }}>Save result</Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
