import { Box, Skeleton, Typography, Button } from '@mui/material';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';

export function LoadingState({ label = 'Loading…', rows = 3 }) {
  return (
    <Box role="status" aria-label={label} sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} variant="rounded" height={52} />
      ))}
    </Box>
  );
}

export function EmptyState({ title = 'Nothing here yet', description, action }) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8, textAlign: 'center' }}>
      <InboxOutlinedIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
      <Typography variant="h6" color="text.primary" sx={{ mb: 0.5, fontWeight: 600 }}>
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 400 }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 2 }}>{action}</Box>}
    </Box>
  );
}

export function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8, textAlign: 'center' }}>
      <ErrorOutlinedIcon sx={{ fontSize: 48, color: 'error.main', mb: 2 }} />
      <Typography variant="h6" color="error.main" sx={{ mb: 0.5, fontWeight: 600 }}>
        {message}
      </Typography>
      {onRetry && (
        <Button variant="outlined" color="inherit" onClick={onRetry} sx={{ mt: 2 }}>
          Try again
        </Button>
      )}
    </Box>
  );
}

export default function States({ state, message, onRetry }) {
  if (state === 'loading') return <LoadingState />;
  if (state === 'error') return <ErrorState message={message} onRetry={onRetry} />;
  return <EmptyState title={message} />;
}
