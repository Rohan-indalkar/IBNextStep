import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import PageHeader from '../../components/PageHeader';
import States from '../../components/States';
import { getMyMonthlyAttendance, downloadMonthlyReport } from '../../api/studentAttendance';
import { Box, Typography, Button, TextField, Stack, Card, CardContent, Divider, Grid } from '@mui/material';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';

export default function Attendance() {
  const [year, setYear] = useState(new Date().getFullYear());
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);

  function load() {
    setLoading(true);
    setError('');
    getMyMonthlyAttendance(year, month)
      .then((res) => setData(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, [year, month]);

  async function handleExport() {
    setExporting(true);
    try {
      await downloadMonthlyReport(year, month);
    } catch (err) {
      setError(err.message);
    } finally {
      setExporting(false);
    }
  }

  const statusColor = { PRESENT: 'success.main', ABSENT: 'error.main', LATE: 'warning.main' };

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Attendance"
        subtitle="Track your daily class attendance and monthly participation percentage."
        actions={
          <Button variant="outlined" color="inherit" onClick={handleExport} disabled={exporting} startIcon={<CloudDownloadIcon />}>
            {exporting ? 'Exporting…' : 'Download report'}
          </Button>
        }
      />

      <Stack direction="row" spacing={2} sx={{ mb: 3 }} alignItems="flex-end" flexWrap="wrap" useFlexGap>
        <TextField
          label="Year"
          type="number"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          size="small"
          sx={{ width: 120 }}
        />
        <TextField
          label="Month"
          type="number"
          slotProps={{ htmlInput: { min: 1, max: 12 } }}
          value={month}
          onChange={(e) => setMonth(Number(e.target.value))}
          size="small"
          sx={{ width: 100 }}
        />
      </Stack>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : data ? (
        <>
          <Grid container spacing={3} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6}>
              <Card>
                <CardContent>
                  <Typography variant="h3" fontWeight="bold" color="primary.main">{data.attendancePercentage}%</Typography>
                  <Typography variant="body2" color="text.secondary">Attendance this month</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Card>
                <CardContent>
                  <Typography variant="h3" fontWeight="bold" color="text.primary">{data.presentCount}/{data.totalDays}</Typography>
                  <Typography variant="body2" color="text.secondary">Present days ({data.absentCount} absent, {data.lateCount} late)</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Card>
            <CardContent sx={{ p: 0, '&:last-child': { pb: 0 } }}>
              {data.records?.length === 0 ? (
                <Box sx={{ p: 3 }}>
                  <Typography color="text.secondary">No records for this month.</Typography>
                </Box>
              ) : (
                <Stack divider={<Divider flexItem />}>
                  {data.records.map((r, i) => (
                    <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', p: 2, bgcolor: i % 2 === 0 ? 'background.default' : 'transparent' }}>
                      <Typography variant="body2">{r.date}</Typography>
                      <Typography variant="body2" fontWeight="bold" sx={{ color: statusColor[r.status] || 'text.secondary' }}>
                        {r.status}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </>
      ) : null}
    </AppShell>
  );
}
