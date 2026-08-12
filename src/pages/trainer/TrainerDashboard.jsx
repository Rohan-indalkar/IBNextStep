import { Link } from 'react-router-dom';
import { Box, Typography, Card, CardContent, Grid, CardActionArea, useTheme } from '@mui/material';
import { PeopleOutlined, GridViewOutlined, MenuBookOutlined, ChecklistOutlined, AssignmentOutlined, GroupOutlined } from '@mui/icons-material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer } from 'recharts';
import AppShell from '../../components/AppShell';
import TrainerSidebar from '../../components/TrainerSidebar';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { LoadingState, ErrorState } from '../../components/States';
import useMyBatches from '../../hooks/useMyBatches';

export default function TrainerDashboard() {
  const { batches, stats, loading, error } = useMyBatches();
  const theme = useTheme();

  return (
    <AppShell roleLabel="Trainer" sidebar={<TrainerSidebar />}>
      <PageHeader title="Overview" subtitle="Manage your assigned batches, student attendance, and course assessments." />

      {loading ? (
        <LoadingState rows={2} />
      ) : error ? (
        <ErrorState message={error} />
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(1, 1fr)', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
            <StatCard label="My Batches" value={stats.myBatches} icon={<GridViewOutlined />} />
            <StatCard label="Active Batches" value={stats.activeBatches} accent icon={<GridViewOutlined />} />
            <StatCard label="Total Students" value={stats.totalStudents} icon={<PeopleOutlined />} />
          </Box>

          <Typography variant="h6" fontWeight={700}>Quick Actions</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
            {[
              { title: 'Study materials', desc: 'Manage course resources', path: '/app/trainer/study-materials', icon: MenuBookOutlined },
              { title: 'Mark attendance', desc: 'Update daily records', path: '/app/trainer/attendance', icon: ChecklistOutlined },
              { title: 'Assignments', desc: 'Create & grade tasks', path: '/app/trainer/assignments', icon: AssignmentOutlined },
            ].map((action, i) => (
              <Card key={i} sx={{ cursor: 'pointer', transition: 'transform 0.2s', '&:hover': { transform: 'translateY(-3px)', boxShadow: 4 } }}>
                <CardActionArea component={Link} to={action.path}>
                  <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2.5 }}>
                    <Box sx={{ width: 44, height: 44, borderRadius: 2, bgcolor: '#F8D0D8', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#E81838' }}>
                      <action.icon />
                    </Box>
                    <Box>
                      <Typography fontWeight={700}>{action.title}</Typography>
                      <Typography variant="body2" color="text.secondary">{action.desc}</Typography>
                    </Box>
                  </CardContent>
                </CardActionArea>
              </Card>
            ))}
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
            <Card sx={{ p: 3, height: 340 }}>
              <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>My Batches</Typography>
              {batches.length === 0 ? (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography variant="body2" color="text.secondary">
                    You're not assigned to any batches yet.
                  </Typography>
                </Box>
              ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, overflowY: 'auto', maxHeight: 240 }}>
                  {batches.map((b) => (
                    <Box
                      key={b.id}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        p: 2,
                        bgcolor: 'var(--color-surface-alt)',
                        borderRadius: 2,
                      }}
                    >
                      <Box display="flex" alignItems="center" gap={1.5}>
                        <Box sx={{ p: 1, borderRadius: 1, bgcolor: 'rgba(198,40,40,0.1)', color: 'primary.main', display: 'flex' }}>
                          <GroupOutlined fontSize="small" />
                        </Box>
                        <Typography variant="body2" fontWeight={700}>{b.name}</Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary" fontWeight={600}>{b.studentCount || 0} students</Typography>
                    </Box>
                  ))}
                </Box>
              )}
            </Card>

            <Card sx={{ p: 3, height: 340 }}>
              <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>Students per Batch</Typography>
              {batches.length > 0 ? (
                <ResponsiveContainer width="100%" height="80%">
                  <BarChart data={batches} margin={{ top: 10, right: 10, left: -20, bottom: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
                    <XAxis dataKey="name" stroke="var(--color-text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                    <YAxis stroke="var(--color-text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                    <RechartsTooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                    <Bar dataKey="studentCount" name="Students" fill="#E81838" radius={[4, 4, 0, 0]} maxBarSize={45} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography variant="body2" color="text.secondary">No batch data to display.</Typography>
                </Box>
              )}
            </Card>
          </Box>
        </Box>
      )}
    </AppShell>
  );
}
