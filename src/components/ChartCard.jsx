import { Card, Typography, Box, Badge } from '@mui/material';

export default function ChartCard({ title, data, colorVar = '#E81838' }) {
  const max = Math.max(1, ...data.map((d) => d.value));

  return (
    <Card sx={{ p: 3, display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Typography variant="h6" sx={{ fontSize: 16, mb: 3, fontWeight: 600 }}>
        {title}
      </Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {data.map((d) => (
          <Box key={d.label}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2" color="text.secondary">
                {d.label}
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 700 }}>
                {d.value}
              </Typography>
            </Box>
            <Box
              sx={{
                height: 12,
                borderRadius: 6,
                bgcolor: 'action.hover',
                overflow: 'hidden'
              }}
            >
              <Box
                sx={{
                  height: '100%',
                  width: `${(d.value / max) * 100}%`,
                  bgcolor: colorVar,
                  borderRadius: 6,
                  transition: 'width 0.4s ease-out'
                }}
              />
            </Box>
          </Box>
        ))}
      </Box>
    </Card>
  );
}
