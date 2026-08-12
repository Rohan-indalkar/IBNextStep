import { Box, Card, CardContent, Typography, useTheme } from '@mui/material';
import AppShell from '../../components/AppShell';
import ExploreIcon from '@mui/icons-material/Explore';

const LABELS = {
  ADMIN: 'Administrator',
  HR: 'HR / Recruiter',
  TRAINER: 'Trainer',
  STUDENT: 'Student',
};

export default function RoleDashboard({ role }) {
  const theme = useTheme();

  return (
    <AppShell roleLabel={LABELS[role] || role}>
      <Box sx={{ maxWidth: 640, mx: 'auto', mt: 8 }}>
        <Card sx={{ p: 4, textAlign: 'center', borderRadius: 4, boxShadow: '0 8px 40px rgba(0,0,0,0.1)' }}>
          <CardContent>
            <Box sx={{ width: 80, height: 80, borderRadius: '50%', bgcolor: '#F8D0D8', display: 'flex', alignItems: 'center', justifyContent: 'center', mx: 'auto', mb: 3, color: '#E81838' }}>
              <ExploreIcon sx={{ fontSize: 40 }} />
            </Box>
            <Typography variant="h4" sx={{ fontWeight: 800, mb: 2 }}>
              You're in — {LABELS[role]} dashboard
            </Typography>
            <Typography color="text.secondary" sx={{ fontSize: '1.1rem', lineHeight: 1.6 }}>
              Authentication is fully wired to the real backend. This is where the {LABELS[role]?.toLowerCase()}
              {' '}modules (courses, batches, attendance, placements, and the rest) get built next.
            </Typography>
          </CardContent>
        </Card>
      </Box>
    </AppShell>
  );
}
