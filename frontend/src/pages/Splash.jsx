import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import Logo from '../components/Logo';
import { Box, Typography } from '@mui/material';

const MIN_DISPLAY_MS = 3000;

export default function Splash() {
  const navigate = useNavigate();
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const start = Date.now();
    const interval = setInterval(() => {
      const elapsed = Date.now() - start;
      setProgress(Math.min(100, Math.round((elapsed / MIN_DISPLAY_MS) * 100)));
    }, 40);

    const timeout = setTimeout(() => {
      navigate('/home', { replace: true });
    }, MIN_DISPLAY_MS);

    return () => {
      clearInterval(interval);
      clearTimeout(timeout);
    };
  }, [navigate]);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(160deg, #282838 0%, #1C1C28 60%, #E81838 100%)',
        gap: 3.5,
      }}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.85 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      >
        <Logo size={64} dark />
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3, duration: 0.5 }}
      >
        <Typography sx={{ color: 'rgba(255,255,255,0.75)', fontSize: 14, fontWeight: 500, letterSpacing: '0.02em' }}>
          Prepare. Practice. Get Placed.
        </Typography>
      </motion.div>

      <Box
        sx={{
          width: 180,
          height: 4,
          borderRadius: 999,
          background: 'rgba(255,255,255,0.15)',
          overflow: 'hidden',
        }}
      >
        <motion.div
          style={{
            height: '100%',
            background: 'var(--color-accent)',
            borderRadius: 999,
            width: `${progress}%`,
          }}
        />
      </Box>
    </Box>
  );
}
