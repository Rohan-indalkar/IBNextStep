import { Card, Box, Typography } from '@mui/material';

export default function StatCard({ label, value, accent = false, icon }) {
  return (
    <Card
      sx={{
        position: 'relative',
        p: 2.5,
        minWidth: 150,
        flex: 1,
        overflow: 'hidden',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        '&:hover': {
          boxShadow: '0 8px 24px rgba(0,0,0,0.08)',
          transform: 'translateY(-2px)',
        },
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 3,
          background: accent
            ? 'linear-gradient(90deg, #E81838, #F8D0D8)'
            : 'linear-gradient(90deg, #282838, #6F6F76)',
        }}
      />
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        {icon && (
          <Box
            sx={{
              width: 38,
              height: 38,
              borderRadius: '10px',
              bgcolor: accent ? '#F8D0D8' : 'rgba(40, 40, 56, 0.05)',
              color: accent ? '#E81838' : 'var(--color-text-muted)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mb: 1.5,
              '& svg': {
                fontSize: '1.25rem',
              },
            }}
          >
            {icon}
          </Box>
        )}
        <Typography
          variant="h4"
          sx={{
            fontFamily: "'Sora', sans-serif",
            fontWeight: 800,
            color: accent ? '#E81838' : 'text.primary',
            lineHeight: 1.1,
            fontSize: { xs: '1.75rem', sm: '2rem' },
          }}
        >
          {value}
        </Typography>
        <Typography
          variant="caption"
          sx={{
            color: 'text.secondary',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            mt: 0.75,
            fontSize: '0.68rem',
            lineHeight: 1.3,
          }}
        >
          {label}
        </Typography>
      </Box>
    </Card>
  );
}
