import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Card,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Grid
} from '@mui/material';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import PageHeader from '../../components/PageHeader';
import useMyBatches from '../../hooks/useMyBatches';
import {
  getStudentListForMarking,
  markAttendance,
  getDailySummary,
  getMonthlySummary,
} from '../../api/attendance';

const STATUSES = ['PRESENT', 'ABSENT', 'LATE'];
const STATUS_COLOR = { PRESENT: 'success.main', ABSENT: 'error.main', LATE: 'warning.main' };

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function Attendance() {
  const { batches } = useMyBatches();
  const [batchId, setBatchId] = useState('');
  const [date, setDate] = useState(todayIso());
  const [students, setStudents] = useState([]);
  const [marks, setMarks] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dailySummary, setDailySummary] = useState(null);

  const [monthYear, setMonthYear] = useState(new Date().getFullYear());
  const [monthNum, setMonthNum] = useState(new Date().getMonth() + 1);
  const [monthlySummary, setMonthlySummary] = useState(null);
  const [monthlyLoading, setMonthlyLoading] = useState(false);

  const loadRoster = useCallback(() => {
    if (!batchId || !date) return;
    setLoading(true);
    setError('');
    getStudentListForMarking(batchId, date)
      .then((res) => {
        const list = res.data.data;
        setStudents(list);
        const initial = {};
        list.forEach((s) => {
          initial[s.studentId] = { status: s.status || '', remarks: s.remarks || '' };
        });
        setMarks(initial);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));

    getDailySummary(batchId, date)
      .then((res) => setDailySummary(res.data.data))
      .catch(() => {});
  }, [batchId, date]);

  useEffect(() => {
    loadRoster();
  }, [loadRoster]);

  useEffect(() => {
    if (batches.length && !batchId) setBatchId(batches[0].id);
  }, [batches, batchId]);

  function setMark(studentId, field, value) {
    setMarks((m) => ({ ...m, [studentId]: { ...m[studentId], [field]: value } }));
  }

  async function handleSave() {
    setError('');
    setNotice('');
    const entries = Object.entries(marks)
      .filter(([, v]) => v.status)
      .map(([studentId, v]) => ({ studentId, status: v.status, remarks: v.remarks || null }));
    if (entries.length === 0) {
      setError('Mark at least one student before saving.');
      return;
    }
    setSaving(true);
    try {
      await markAttendance({ batchId, date, entries });
      setNotice('Attendance saved.');
      loadRoster();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function loadMonthly() {
    if (!batchId) return;
    setMonthlyLoading(true);
    getMonthlySummary(batchId, monthYear, monthNum)
      .then((res) => setMonthlySummary(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setMonthlyLoading(false));
  }

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader title="Attendance" />

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <TextField select size="small" label="Batch" value={batchId} onChange={(e) => setBatchId(e.target.value)} sx={{ minWidth: 200 }} slotProps={{ inputLabel: { shrink: true } }}>
          <MenuItem value="" disabled>Select a batch…</MenuItem>
          {batches.map((b) => (
            <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>
          ))}
        </TextField>
        <TextField type="date" size="small" value={date} onChange={(e) => setDate(e.target.value)} />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {notice && <Alert severity="success" sx={{ mb: 2 }}>{notice}</Alert>}

      {dailySummary && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="body2" color="text.secondary">
            {dailySummary.presentCount} present · {dailySummary.absentCount} absent · {dailySummary.lateCount} late · {dailySummary.notMarkedCount} not marked
          </Typography>
        </Box>
      )}

      {!batchId ? (
        <Typography color="text.secondary">Select a batch to mark attendance.</Typography>
      ) : loading ? (
        <CircularProgress />
      ) : students.length === 0 ? (
        <Typography color="text.secondary">No students in this batch.</Typography>
      ) : (
        <Card sx={{ mb: 4, overflow: 'hidden' }}>
          <TableContainer component={Paper} elevation={0} sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
            <Table size="small">
              <TableHead sx={{ bgcolor: 'background.default' }}>
                <TableRow>
                  <TableCell>Student</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Remarks</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {students.map((s) => (
                  <TableRow key={s.studentId}>
                    <TableCell>
                      {s.firstName} {s.lastName}
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        {STATUSES.map((st) => (
                          <Button
                            key={st}
                            size="small"
                            variant={marks[s.studentId]?.status === st ? 'contained' : 'outlined'}
                            onClick={() => setMark(s.studentId, 'status', st)}
                            sx={{
                              borderRadius: 4,
                              minWidth: 70,
                              p: '2px 8px',
                              ...(marks[s.studentId]?.status === st && {
                                bgcolor: STATUS_COLOR[st],
                                '&:hover': { bgcolor: STATUS_COLOR[st] }
                              })
                            }}
                          >
                            {st}
                          </Button>
                        ))}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        placeholder="Optional"
                        fullWidth
                        value={marks[s.studentId]?.remarks || ''}
                        onChange={(e) => setMark(s.studentId, 'remarks', e.target.value)}
                        variant="outlined"
                        InputProps={{ sx: { fontSize: 13, padding: '2px' } }}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Box sx={{ p: 2 }}>
            <Button variant="contained" color="primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save attendance'}
            </Button>
          </Box>
        </Card>
      )}

      <Card sx={{ p: 3 }}>
        <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>Monthly summary</Typography>
        <Grid container spacing={2} sx={{ mb: 3, alignItems: 'flex-end' }}>
          <Grid item>
            <TextField label="Year" type="number" size="small" value={monthYear} onChange={(e) => setMonthYear(Number(e.target.value))} sx={{ width: 100 }} />
          </Grid>
          <Grid item>
            <TextField label="Month" type="number" inputProps={{ min: 1, max: 12 }} size="small" value={monthNum} onChange={(e) => setMonthNum(Number(e.target.value))} sx={{ width: 80 }} />
          </Grid>
          <Grid item>
            <Button variant="outlined" onClick={loadMonthly} disabled={!batchId || monthlyLoading}>
              {monthlyLoading ? 'Loading…' : 'View summary'}
            </Button>
          </Grid>
        </Grid>

        {monthlySummary && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {monthlySummary.students.map((s) => (
              <Box
                key={s.studentId}
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  p: 1.5,
                  bgcolor: 'background.default',
                  borderRadius: 1,
                  fontSize: 14,
                }}
              >
                <Typography variant="body2">{s.firstName} {s.lastName}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {s.presentCount}/{s.totalDays} present — {s.attendancePercentage}%
                </Typography>
              </Box>
            ))}
          </Box>
        )}
      </Card>
    </AppShell>
  );
}
