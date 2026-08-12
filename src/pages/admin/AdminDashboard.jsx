import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, Button, Typography, Card, CardContent } from '@mui/material';
import PeopleOutlinedIcon from '@mui/icons-material/PeopleOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import WorkOutlinedIcon from '@mui/icons-material/WorkOutlined';
import GridViewOutlinedIcon from '@mui/icons-material/GridViewOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import BusinessCenterOutlinedIcon from '@mui/icons-material/BusinessCenterOutlined';
import { PieChart, Pie, Cell, Tooltip as RechartsTooltip, Legend, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';

import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { LoadingState, ErrorState } from '../../components/States';
import { getAdminDashboardStats } from '../../api/adminDashboard';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    setError('');
    getAdminDashboardStats()
      .then((res) => setStats(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    getAdminDashboardStats()
      .then((res) => {
        if (!cancelled) setStats(res.data.data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const roleData = [
    { name: 'Students', value: stats?.totalStudents ?? 0 },
    { name: 'Trainers', value: stats?.totalTrainers ?? 0 },
    { name: 'HR / Recruiters', value: stats?.totalHr ?? 0 },
  ];

  const footprintData = [
    { name: 'Active Batches', value: stats?.activeBatches ?? 0 },
    { name: 'Courses', value: stats?.totalCourses ?? 0 },
    { name: 'Placements', value: stats?.activePlacementDrives ?? 0 },
  ];

  const ROLE_COLORS = ['#E81838', '#282838', '#6F6F76'];
  const totalPeople = roleData.reduce((acc, curr) => acc + curr.value, 0);

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader title="Welcome back" subtitle="Here's a snapshot of the organisation, pulled live from the platform." />

      {loading ? (
        <LoadingState rows={2} />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', lg: 'repeat(6, 1fr)' }, gap: 2 }}>
            <StatCard label="Students" value={stats?.totalStudents ?? 0} icon={<SchoolOutlinedIcon />} />
            <StatCard label="Trainers" value={stats?.totalTrainers ?? 0} icon={<PeopleOutlinedIcon />} />
            <StatCard label="HR / Recruiters" value={stats?.totalHr ?? 0} icon={<WorkOutlinedIcon />} />
            <StatCard label="Active batches" value={stats?.activeBatches ?? 0} accent icon={<GridViewOutlinedIcon />} />
            <StatCard label="Courses" value={stats?.totalCourses ?? 0} icon={<MenuBookOutlinedIcon />} />
            <StatCard label="Active placement drives" value={stats?.activePlacementDrives ?? 0} accent icon={<BusinessCenterOutlinedIcon />} />
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
            <Card sx={{ p: 3, height: 340, position: 'relative' }}>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>People by role</Typography>
              <ResponsiveContainer width="100%" height="80%">
                <PieChart>
                  <Pie
                    data={roleData}
                    cx="50%"
                    cy="45%"
                    innerRadius={60}
                    outerRadius={95}
                    paddingAngle={3}
                    dataKey="value"
                    isAnimationActive={true}
                  >
                    {roleData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={ROLE_COLORS[index % ROLE_COLORS.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip />
                  <Legend verticalAlign="bottom" height={36} />
                </PieChart>
              </ResponsiveContainer>
              <Box sx={{ position: 'absolute', top: '47%', left: '50%', transform: 'translate(-50%, -50%)', textAlign: 'center', pointerEvents: 'none' }}>
                <Typography variant="h5" fontWeight="800" sx={{ color: 'var(--color-text)', lineHeight: 1 }}>{totalPeople}</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total</Typography>
              </Box>
            </Card>

            <Card sx={{ p: 3, height: 340 }}>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>Program Footprint</Typography>
              <ResponsiveContainer width="100%" height="80%">
                <BarChart data={footprintData} margin={{ top: 20, right: 30, left: 0, bottom: 15 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" />
                  <XAxis dataKey="name" tick={{ fontSize: 12, fill: 'var(--color-text-muted)' }} axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: 'var(--color-text-muted)' }} axisLine={false} tickLine={false} />
                  <RechartsTooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                  <Bar dataKey="value" fill="#E81838" radius={[6, 6, 0, 0]} maxBarSize={55} isAnimationActive={true} />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Box>

          <Card sx={{
            background: 'linear-gradient(135deg, #282838 0%, #1C1C28 100%)',
            color: 'white',
            p: 4,
            borderRadius: 3,
            position: 'relative',
            overflow: 'hidden',
            boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
          }}>
            <CardContent sx={{ p: 0, '&:last-child': { pb: 0 } }}>
              <Typography variant="h5" sx={{ fontSize: 22, mb: 1, fontWeight: 800 }}>Set up your organisation</Typography>
              <Typography sx={{ fontSize: 14, mb: 3, opacity: 0.8, maxWidth: '640px', lineHeight: 1.6 }}>
                Departments and users are the foundation everything else builds on — batches, courses and
                placements all reference them.
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Button component={Link} to="/app/admin/departments" variant="contained" sx={{ bgcolor: 'white', color: 'black', fontWeight: 700, '&:hover': { bgcolor: '#f0f0f0' } }}>
                  Manage departments
                </Button>
                <Button component={Link} to="/app/admin/users" variant="outlined" sx={{ color: 'white', borderColor: 'rgba(255,255,255,0.4)', fontWeight: 600, '&:hover': { borderColor: 'white', bgcolor: 'rgba(255,255,255,0.08)' } }}>
                  Manage users
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Box>
      )}
    </AppShell>
  );
}
