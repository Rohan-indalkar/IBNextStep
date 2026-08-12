import { Box, Typography } from '@mui/material';

export default function Logo({ size = 40, withWordmark = true, dark = false }) {
  const textColor = dark ? '#FFFDF9' : '#282838';
  const accentColor = dark ? '#F8D0D8' : '#E81838';

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="40" height="40" rx="12" fill="#E81838" />
        <path d="M14 12L22 20L14 28" stroke="#ffffff" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M22 12L27 20L22 28" stroke="#FFFFFF" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.5" />
      </svg>
      {withWordmark && (
        <Typography
          variant="h6"
          component="span"
          sx={{
            fontFamily: 'Sora, sans-serif',
            fontWeight: 700,
            fontSize: size * 0.5,
            color: textColor,
            letterSpacing: '-0.02em',
          }}
        >
          IB<Box component="span" sx={{ color: accentColor }}>NextStep</Box>
        </Typography>
      )}
    </Box>
  );
}
