import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import HrSidebar from '../../components/HrSidebar';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { getHrDashboard } from '../../api/hrDashboard';
import { LoadingState, ErrorState } from '../../components/States';
import { Typography, Box, Card, CardContent, Button, Chip, Grid, Avatar } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, Tooltip as RechartsTooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell } from 'recharts';
import {
  BusinessOutlined,
  WorkOutlined,
  DescriptionOutlined,
  CheckCircleOutlined,
  EventAvailableOutlined,
  FilterListOutlined,
  ScheduleOutlined,
  CancelOutlined,
  AddOutlined,
  TrendingUpOutlined,
} from '@mui/icons-material';

const PIPELINE_COLORS = ['#282838', '#6F6F76', '#1E8E3E', '#E81838'];

export default function HrDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getHrDashboard()
      .then((res) => setData(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const getCompanyChartData = () => {
    if (!data?.companyWiseHiring) return [];
    return Object.entries(data.companyWiseHiring).map(([name, count]) => ({ name, count }));
  };

  const getPipelineData = () => {
    if (!data) return [];
    return [
      { name: 'Shortlisted', value: data.shortlisted || 0 },
      { name: 'Interviewed', value: data.interviewScheduled || 0 },
      { name: 'Selected', value: data.selected || 0 },
      { name: 'Rejected', value: data.rejected || 0 },
    ].filter((d) => d.value > 0);
  };

  return (
    <AppShell roleLabel="HR / Recruiter" sidebar={<HrSidebar />}>
      <PageHeader
        title="Recruitment Overview"
        subtitle="Monitor hiring pipelines, company partnerships, placement drives, and candidate status."
        actions={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <Button component={Link} to="/app/hr/placements" variant="contained" color="primary" startIcon={<AddOutlined />}>
              New placement drive
            </Button>
            <Button component={Link} to="/app/hr/companies" variant="outlined" color="inherit">
              Manage companies
            </Button>
          </Box>
        }
      />

      {loading ? (
        <LoadingState rows={3} />
      ) : error ? (
        <ErrorState message={error} />
      ) : data ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {/* Top Stat Cards */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: 2 }}>
            <StatCard label="Total Partner Companies" value={data.totalCompanies} icon={<BusinessOutlined />} />
            <StatCard label="Active Companies" value={data.activeCompanies} icon={<CheckCircleOutlined />} />
            <StatCard label="Active Drives" value={data.activePlacementDrives} accent icon={<WorkOutlined />} />
            <StatCard label="Campus / Off-Campus" value={`${data.campusDrives} / ${data.offCampusDrives}`} icon={<EventAvailableOutlined />} />
          </Box>

          {/* Application Pipeline Stats */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', lg: 'repeat(5, 1fr)' }, gap: 2 }}>
            <StatCard label="Total Applications" value={data.applicationsReceived} icon={<DescriptionOutlined />} />
            <StatCard label="Shortlisted" value={data.shortlisted} icon={<FilterListOutlined />} />
            <StatCard label="Interviews Scheduled" value={data.interviewScheduled} icon={<ScheduleOutlined />} />
            <StatCard label="Selected Candidates" value={data.selected} accent icon={<CheckCircleOutlined />} />
            <StatCard label="Rejected" value={data.rejected} icon={<CancelOutlined />} />
          </Box>

          {/* Charts Row */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 2fr' }, gap: 3 }}>
            {/* Donut Chart: Pipeline Breakdown */}
            <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
              <Typography variant="h6" sx={{ mb: 1, fontFamily: "'Sora', sans-serif", fontWeight: 700 }}>
                Pipeline Breakdown
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {data.applicationsReceived || 0} applications in process
              </Typography>
              {getPipelineData().length === 0 ? (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography variant="body2" color="text.secondary">No applications recorded yet.</Typography>
                </Box>
              ) : (
                <Box sx={{ height: 240, position: 'relative' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={getPipelineData()}
                        cx="50%"
                        cy="45%"
                        innerRadius={50}
                        outerRadius={75}
                        paddingAngle={4}
                        dataKey="value"
                      >
                        {getPipelineData().map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={PIPELINE_COLORS[index % PIPELINE_COLORS.length]} />
                        ))}
                      </Pie>
                      <RechartsTooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </Box>
              )}
            </Card>

            {/* Bar Chart: Company-wise Hiring */}
            <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
              <Typography variant="h6" sx={{ mb: 1, fontFamily: "'Sora', sans-serif", fontWeight: 700 }}>
                Company-Wise Selections
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Hiring volume per partner company
              </Typography>
              {getCompanyChartData().length === 0 ? (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography variant="body2" color="text.secondary">No placement selections recorded yet.</Typography>
                </Box>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={getCompanyChartData()} margin={{ top: 10, right: 20, left: -20, bottom: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" />
                    <XAxis dataKey="name" stroke="var(--color-text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                    <YAxis allowDecimals={false} stroke="var(--color-text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                    <RechartsTooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                    <Bar dataKey="count" fill="#E81838" radius={[4, 4, 0, 0]} maxBarSize={45} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card>
          </Box>

          {/* Upcoming Interviews & Quick Stats Banner */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '2fr 1fr' }, gap: 3 }}>
            <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700 }}>
                  Next Upcoming Interviews
                </Typography>
                <Button component={Link} to="/app/hr/applications" size="small" variant="text" color="primary">
                  View all
                </Button>
              </Box>

              {(!data.nextUpcomingInterviews || data.nextUpcomingInterviews.length === 0) ? (
                <Box sx={{ py: 4, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">No upcoming interviews scheduled today.</Typography>
                </Box>
              ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  {data.nextUpcomingInterviews.map((i, idx) => (
                    <Box key={idx} sx={{ display: 'flex', justifyContent: 'space-between', p: 2, bgcolor: 'var(--color-surface-alt)', borderRadius: 2, alignItems: 'center' }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar sx={{ bgcolor: 'primary.main', fontWeight: 700, width: 38, height: 38, fontSize: '0.875rem' }}>
                          {i.studentName?.charAt(0) || 'S'}
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight={700}>{i.studentName} — {i.companyName}</Typography>
                          <Typography variant="caption" color="text.secondary" fontWeight={500}>{i.roundType || 'Interview'}</Typography>
                        </Box>
                      </Box>
                      <Chip label={i.scheduledAt ? new Date(i.scheduledAt).toLocaleString() : 'Scheduled'} size="small" variant="outlined" color="primary" sx={{ fontWeight: 600 }} />
                    </Box>
                  ))}
                </Box>
              )}
            </Card>

            {/* Today's Highlight Box */}
            <Card sx={{ p: 3, borderRadius: 3, bgcolor: '#282838', color: '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box>
                <Chip label="Today's Schedule" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.12)', color: '#fff', fontWeight: 700, mb: 2 }} />
                <Typography variant="h3" sx={{ fontWeight: 800, color: 'var(--color-accent)', mb: 0.5 }}>
                  {data.todaysInterviews || 0}
                </Typography>
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', fontWeight: 600 }}>
                  Interviews taking place today
                </Typography>
              </Box>
              <Box sx={{ pt: 3, borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.9)', fontWeight: 700, mb: 1 }}>
                  {data.upcomingInterviews || 0} total upcoming
                </Typography>
                <Button component={Link} to="/app/hr/applications" variant="contained" color="primary" fullWidth sx={{ textTransform: 'none', fontWeight: 700 }}>
                  Manage Candidate Pipeline
                </Button>
              </Box>
            </Card>
          </Box>
        </Box>
      ) : null}
    </AppShell>
  );
}
