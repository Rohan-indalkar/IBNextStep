import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { getStudentPlacementDashboard } from '../../api/studentDashboard';
import { LoadingState, ErrorState } from '../../components/States';
import { Box, Typography, Card, CardContent, CardActionArea, useTheme, Chip, Button, Avatar } from '@mui/material';
import { PieChart, Pie, Cell, Tooltip as RechartsTooltip, ResponsiveContainer, Legend } from 'recharts';
import {
  AssignmentOutlined,
  CheckCircleOutlined,
  CancelOutlined,
  BusinessCenterOutlined,
  SearchOutlined,
  DescriptionOutlined,
  QuizOutlined,
  CodeOutlined,
  EventNoteOutlined,
  ArrowForwardOutlined,
} from '@mui/icons-material';

const PIE_COLORS = ['#1E8E3E', '#E81838', '#B2790A'];

export default function StudentDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStudentPlacementDashboard()
      .then((res) => setData(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const pieData = data
    ? [
        { name: 'Selected', value: data.selectedCount },
        { name: 'Rejected', value: data.rejectedCount },
        { name: 'Pending', value: Math.max(0, data.appliedCount - data.selectedCount - data.rejectedCount) },
      ].filter((d) => d.value > 0)
    : [];

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Student Dashboard"
        subtitle="Track your applications, upcoming interviews, assessment results, and placement opportunities."
        actions={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <Button component={Link} to="/app/student/placements" variant="contained" color="primary" startIcon={<BusinessCenterOutlined />}>
              Browse drives
            </Button>
            <Button component={Link} to="/app/student/resume" variant="outlined" color="inherit" startIcon={<DescriptionOutlined />}>
              My resume
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
          {/* Top Metric Cards */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', lg: 'repeat(5, 1fr)' }, gap: 2 }}>
            <StatCard label="Total Applications" value={data.appliedCount} icon={<AssignmentOutlined />} />
            <StatCard label="Selections" value={data.selectedCount} accent icon={<CheckCircleOutlined />} />
            <StatCard label="Rejections" value={data.rejectedCount} icon={<CancelOutlined />} />
            <StatCard label="Campus Drives" value={data.campusOpenCount} accent icon={<BusinessCenterOutlined />} />
            <StatCard label="Off-Campus Drives" value={data.offCampusOpenCount} icon={<SearchOutlined />} />
          </Box>

          {/* Charts & Upcoming Interviews Grid */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 2fr' }, gap: 3 }}>
            {/* Pie Chart: Application Status */}
            <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)', height: '100%', minHeight: 320 }}>
              <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 1 }}>
                Application Pipeline
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Status distribution of your submitted applications
              </Typography>
              {pieData.length > 0 ? (
                <Box sx={{ height: 220 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={pieData} innerRadius={55} outerRadius={80} paddingAngle={4} dataKey="value" isAnimationActive={true}>
                        {pieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <RechartsTooltip />
                      <Legend verticalAlign="bottom" height={36} />
                    </PieChart>
                  </ResponsiveContainer>
                </Box>
              ) : (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography variant="body2" color="text.secondary">No application status recorded yet.</Typography>
                </Box>
              )}
            </Card>

            {/* Upcoming Interviews & History Stack */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              {/* Upcoming Interviews Card */}
              <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700 }}>
                    Upcoming Interviews
                  </Typography>
                  <Button component={Link} to="/app/student/placements" size="small" variant="text" color="primary">
                    View drives
                  </Button>
                </Box>

                {(!data.upcomingInterviews || data.upcomingInterviews.length === 0) ? (
                  <Box sx={{ py: 3, textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">No upcoming interview rounds scheduled right now.</Typography>
                  </Box>
                ) : (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {data.upcomingInterviews.map((i, idx) => (
                      <Box key={idx} sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: 'var(--color-surface-alt)', borderRadius: 2 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar sx={{ bgcolor: 'primary.main', fontWeight: 700, width: 38, height: 38, fontSize: '0.875rem' }}>
                            {i.companyName?.charAt(0) || 'C'}
                          </Avatar>
                          <Box>
                            <Typography variant="body2" fontWeight={700}>{i.companyName}</Typography>
                            <Typography variant="caption" color="text.secondary" fontWeight={500}>{i.roundType} (Round {i.roundNumber})</Typography>
                          </Box>
                        </Box>
                        <Chip label={i.scheduledAt ? new Date(i.scheduledAt).toLocaleString() : 'Scheduled'} size="small" color="primary" variant="outlined" sx={{ fontWeight: 600 }} />
                      </Box>
                    ))}
                  </Box>
                )}
              </Card>

              {/* Interview History Card */}
              {data.interviewHistory?.length > 0 && (
                <Card sx={{ p: 3, borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
                  <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 2 }}>
                    Interview History
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {data.interviewHistory.map((i, idx) => (
                      <Box key={idx} sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: 'var(--color-surface-alt)', borderRadius: 2 }}>
                        <Box>
                          <Typography variant="body2" fontWeight={700}>{i.companyName}</Typography>
                          <Typography variant="caption" color="text.secondary" fontWeight={500}>{i.roundType}</Typography>
                        </Box>
                        <Chip
                          label={i.result}
                          color={i.result === 'QUALIFIED' || i.result === 'SELECTED' ? 'success' : i.result === 'REJECTED' ? 'error' : 'default'}
                          size="small"
                          sx={{ fontWeight: 700 }}
                        />
                      </Box>
                    ))}
                  </Box>
                </Card>
              )}
            </Box>
          </Box>

          {/* Quick Actions Grid */}
          <Box sx={{ mt: 1 }}>
            <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 2 }}>
              Quick Navigation
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)', md: 'repeat(4, 1fr)' }, gap: 2 }}>
              {[
                { title: 'Placement Drives', desc: 'Apply to campus & off-campus jobs', path: '/app/student/placements', icon: BusinessCenterOutlined },
                { title: 'Quizzes & Tests', desc: 'Assigned MCQ & skill tests', path: '/app/student/quizzes', icon: QuizOutlined },
                { title: 'Coding Tests', desc: 'Live programming assessments', path: '/app/student/coding-assessments', icon: CodeOutlined },
                { title: 'My Resume', desc: 'Upload & track resume score', path: '/app/student/resume', icon: DescriptionOutlined },
              ].map((action, idx) => (
                <Card key={idx} sx={{ borderRadius: 3, border: '1px solid var(--color-border)', transition: 'transform 0.2s, box-shadow 0.2s', '&:hover': { transform: 'translateY(-3px)', boxShadow: '0 8px 24px rgba(0,0,0,0.08)' } }}>
                  <CardActionArea component={Link} to={action.path} sx={{ p: 2.5, height: '100%' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                      <Box sx={{ width: 44, height: 44, borderRadius: 2, bgcolor: 'rgba(198,40,40,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'primary.main' }}>
                        <action.icon />
                      </Box>
                      <ArrowForwardOutlined fontSize="small" sx={{ color: 'text.secondary' }} />
                    </Box>
                    <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 0.5 }}>{action.title}</Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 500 }}>{action.desc}</Typography>
                  </CardActionArea>
                </Card>
              ))}
            </Box>
          </Box>
        </Box>
      ) : null}
    </AppShell>
  );
}
