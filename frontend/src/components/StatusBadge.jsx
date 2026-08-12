import { Chip } from '@mui/material';

const TONE_MAP = {
  // Generic
  ACTIVE: 'success', INACTIVE: 'neutral', DRAFT: 'neutral', PUBLISHED: 'success', SCHEDULED: 'warning',
  CLOSED: 'danger', ARCHIVED: 'neutral', COMPLETED: 'neutral', EXPIRED: 'danger',
  // Users
  APPROVED: 'success', PENDING_REVIEW: 'warning', NEEDS_CHANGES: 'danger',
  // Applications / placements
  APPLIED: 'neutral', SHORTLISTED: 'warning', REJECTED: 'danger', INTERVIEW_SCHEDULED: 'info',
  SELECTED: 'success', NOT_SELECTED: 'danger',
  // Attendance / assignments
  PRESENT: 'success', ABSENT: 'danger', LATE: 'warning', SUBMITTED: 'warning', GRADED: 'success',
  // Sessions
  CONDUCTED: 'info', EVALUATED: 'info', CANCELLED: 'danger', IN_PROGRESS: 'info',
};

const COLOR_MAP = {
  success: { bg: '#E8F5E9', color: '#1E8E3E', solidBg: '#1E8E3E' },
  warning: { bg: '#FFF8E1', color: '#B2790A', solidBg: '#B2790A' },
  danger: { bg: '#F8D0D8', color: '#E81838', solidBg: '#E81838' },
  info: { bg: '#E8E8F5', color: '#282838', solidBg: '#282838' },
  neutral: { bg: '#F8F8E8', color: '#6F6F76', solidBg: '#6F6F76' },
};

export default function StatusBadge({ status, tone, solid = false }) {
  if (!status) return null;
  const resolvedTone = tone || TONE_MAP[status] || 'neutral';
  const label = String(status).replace(/_/g, ' ');

  const colors = COLOR_MAP[resolvedTone] || COLOR_MAP.neutral;

  return (
    <Chip
      label={label}
      size="small"
      sx={{
        fontWeight: 600,
        fontSize: '0.75rem',
        letterSpacing: '0.02em',
        borderRadius: 1,
        ...(solid
          ? { bgcolor: colors.solidBg, color: '#fff', border: 'none' }
          : { bgcolor: colors.bg, color: colors.color, border: `1px solid ${colors.color}30` })
      }}
    />
  );
}
