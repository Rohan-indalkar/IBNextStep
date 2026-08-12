import { Box, Card, Typography } from '@mui/material';
import { motion } from 'framer-motion';
import Logo from './Logo';

const HIGHLIGHTS = [
  { title: 'Structured Learning', body: 'Batches, courses and study material organised like a real training program.' },
  { title: 'Practice That Counts', body: 'Coding assessments, quizzes and mock interviews before it matters.' },
  { title: 'A Real Next Step', body: 'Evaluations and resume readiness roll into one placement pipeline.' },
];

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', p: { xs: 3, md: 6 }, bgcolor: 'background.default' }}>
        <Box sx={{ mb: 4 }}>
          <Logo size={36} />
        </Box>
        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
          style={{ width: '100%', maxWidth: 420 }}
        >
          <Card sx={{ p: 4, width: '100%', borderRadius: 2, boxShadow: (theme) => theme.palette.mode === 'light' ? '0 4px 24px rgba(0,0,0,0.05)' : 'none' }}>
            <Typography variant="h5" component="h1" sx={{ fontFamily: 'Sora, sans-serif', fontWeight: 700, mb: 1 }}>
              {title}
            </Typography>
            {subtitle && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
                {subtitle}
              </Typography>
            )}
            {children}
          </Card>
        </motion.div>
      </Box>

      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flex: 1,
          flexDirection: 'column',
          justifyContent: 'center',
          bgcolor: '#282838',
          color: '#FFFFFF',
          position: 'relative',
          overflow: 'hidden',
          p: 8,
        }}
      >
        <Box sx={{ position: 'absolute', top: -60, right: -60, width: 240, height: 240, borderRadius: '50%', background: 'rgba(255,255,255,0.03)' }} />
        <Box sx={{ position: 'absolute', bottom: -80, left: -40, width: 200, height: 200, borderRadius: '50%', background: 'rgba(255,255,255,0.02)' }} />
        <Box sx={{ position: 'relative', maxWidth: 440, transform: 'translateY(-22px)' }}>
          <Logo size={36} dark />
          <Typography variant="body1" sx={{ opacity: 0.8, mt: 4, mb: 6 }}>
            IBNextStep runs the whole InfoBeans Foundation training journey — coursework, practice,
            evaluation and placement — in one connected system.
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {HIGHLIGHTS.map((h) => (
              <Box key={h.title} sx={{ display: 'flex', gap: 2 }}>
                <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#E81838', mt: 1, flexShrink: 0 }} />
                <Box>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>{h.title}</Typography>
                  <Typography variant="body2" sx={{ opacity: 0.75 }}>{h.body}</Typography>
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
