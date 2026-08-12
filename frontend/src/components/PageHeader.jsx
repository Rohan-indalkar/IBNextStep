import { Box, Typography } from '@mui/material';

export default function PageHeader({ title, subtitle, actions, action }) {
  const headerActions = actions || action;
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 2, mb: 3.5 }}>
      <Box>
        <Typography variant="h4" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 800, mb: subtitle ? 0.5 : 0 }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body1" color="text.secondary" sx={{ fontWeight: 500 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {headerActions && (
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', alignItems: 'center' }}>
          {headerActions}
        </Box>
      )}
    </Box>
  );
}
