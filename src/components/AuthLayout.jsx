import { Box, Card, Typography } from '@mui/material';
import { motion } from 'framer-motion';
import Logo from './Logo';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';

const HIGHLIGHTS = [
  {
    title: 'Structured Learning',
    body: 'Batches, courses and study material organised like a real training program.',
  },
  {
    title: 'Practice That Counts',
    body: 'Coding assessments, quizzes and mock interviews before it matters.',
  },
  {
    title: 'A Real Next Step',
    body: 'Evaluations and resume readiness roll into one placement pipeline.',
  },
];

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Left: Form panel (untouched) */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 3, md: 6 },
          bgcolor: 'background.default',
        }}
      >
        <Box sx={{ mb: 4 }}>
          <Logo size={36} />
        </Box>
        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
          style={{ width: '100%', maxWidth: 420 }}
        >
          <Card
            sx={{
              p: 4,
              width: '100%',
              borderRadius: 2,
              boxShadow: (theme) =>
                theme.palette.mode === 'light' ? '0 4px 24px rgba(0,0,0,0.05)' : 'none',
            }}
          >
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

      {/* Right: Brand panel matching reference screenshot */}
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
        {/* Decorative background circles matching screenshot */}
        <Box
          sx={{
            position: 'absolute',
            top: -80,
            right: -80,
            width: 320,
            height: 320,
            borderRadius: '50%',
            background: 'rgba(255, 255, 255, 0.025)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            bottom: -100,
            left: -60,
            width: 300,
            height: 300,
            borderRadius: '50%',
            background: 'rgba(232, 24, 56, 0.08)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            top: '48%',
            right: 60,
            width: 150,
            height: 150,
            borderRadius: '50%',
            background: 'rgba(255, 255, 255, 0.018)',
            pointerEvents: 'none',
          }}
        />

        <Box sx={{ position: 'relative', maxWidth: 440, transform: 'translateY(-20px)' }}>
          <Logo size={36} dark />
          <Typography
            variant="body1"
            sx={{
              opacity: 0.8,
              mt: 4,
              mb: 5,
              lineHeight: 1.65,
              fontSize: '0.9375rem',
            }}
          >
            IBNextStep runs the whole InfoBeans Foundation training journey — coursework, practice,
            evaluation and placement — in one connected system.
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {HIGHLIGHTS.map((h) => (
              <Box key={h.title} sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: '8px',
                    bgcolor: 'rgba(232, 24, 56, 0.15)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    mt: 0.25,
                  }}
                >
                  <CheckCircleOutlinedIcon sx={{ fontSize: '1.05rem', color: '#E81838' }} />
                </Box>
                <Box>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.4, fontSize: '0.9375rem', color: '#FFFFFF' }}>
                    {h.title}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.72, fontSize: '0.8125rem', lineHeight: 1.5 }}>
                    {h.body}
                  </Typography>
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
