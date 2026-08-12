import { useEffect, useState } from 'react';
import {
  IconButton,
  Badge,
  Popover,
  Box,
  Typography,
  Chip,
  Tooltip,
  Divider,
  Button,
} from '@mui/material';
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';
import NotificationsActiveOutlinedIcon from '@mui/icons-material/NotificationsActiveOutlined';
import CloseIcon from '@mui/icons-material/Close';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import client from '../api/client';

function myNotifications(page = 0, size = 30) {
  return client.get('/notifications/me', { params: { page, size } });
}
function markRead(id) {
  return client.patch(`/notifications/${id}/read`);
}

export default function NotificationButton() {
  const [anchorEl, setAnchorEl] = useState(null);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function load() {
    setLoading(true);
    setError('');
    myNotifications()
      .then((res) => setItems(res.data.data.content))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  const handleClick = (event) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);
  const open = Boolean(anchorEl);

  const unreadCount = items.filter((n) => !n.read).length;

  async function handleMarkItemRead(n) {
    if (!n.read) {
      try {
        await markRead(n.id);
        setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      } catch {
        // non-critical
      }
    }
  }

  // Dismiss / Clear an individual notification item
  async function handleDismissItem(e, n) {
    e.stopPropagation();
    if (!n.read) {
      markRead(n.id).catch(() => {});
    }
    setItems((list) => list.filter((x) => x.id !== n.id));
  }

  // Clear all notifications from current view
  async function handleClearAll() {
    items.forEach((n) => {
      if (!n.read) markRead(n.id).catch(() => {});
    });
    setItems([]);
  }

  return (
    <>
      <Tooltip title="Notifications">
        <IconButton color="inherit" onClick={handleClick} sx={{ borderRadius: 2 }}>
          <Badge badgeContent={unreadCount > 9 ? '9+' : unreadCount} color="error">
            <NotificationsNoneOutlinedIcon />
          </Badge>
        </IconButton>
      </Tooltip>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{
          paper: {
            sx: {
              width: { xs: 320, sm: 360 },
              maxHeight: 480,
              borderRadius: 3,
              boxShadow: '0 16px 40px rgba(0,0,0,0.18)',
              border: '1px solid',
              borderColor: 'divider',
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
              mt: 1,
            },
          },
        }}
      >
        {/* Header */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            px: 2,
            py: 1.5,
            bgcolor: 'background.paper',
            borderBottom: '1px solid',
            borderColor: 'divider',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography
              variant="h6"
              sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, fontSize: '0.95rem' }}
            >
              Notifications
            </Typography>
            {unreadCount > 0 && (
              <Chip
                label={`${unreadCount} unread`}
                size="small"
                sx={{
                  bgcolor: '#F8D0D8',
                  color: '#E81838',
                  fontWeight: 700,
                  fontSize: '0.7rem',
                  height: 20,
                }}
              />
            )}
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {items.length > 0 && (
              <Button
                size="small"
                variant="text"
                color="inherit"
                onClick={handleClearAll}
                sx={{ fontSize: '0.75rem', fontWeight: 600, px: 1, color: 'text.secondary' }}
              >
                Clear all
              </Button>
            )}
            <IconButton size="small" onClick={handleClose} sx={{ color: 'text.secondary' }}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>

        {/* Content list */}
        <Box sx={{ overflowY: 'auto', flex: 1, p: 1 }}>
          {loading ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                Loading notifications…
              </Typography>
            </Box>
          ) : error ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="error">
                {error}
              </Typography>
            </Box>
          ) : items.length === 0 ? (
            <Box sx={{ py: 5, px: 3, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
              <CheckCircleOutlinedIcon sx={{ fontSize: 40, color: 'text.disabled', opacity: 0.7 }} />
              <Typography variant="subtitle2" fontWeight={700}>
                All caught up!
              </Typography>
              <Typography variant="caption" color="text.secondary">
                No notifications to display right now.
              </Typography>
            </Box>
          ) : (
            items.map((n) => (
              <Box
                key={n.id}
                onClick={() => handleMarkItemRead(n)}
                sx={{
                  position: 'relative',
                  p: 1.5,
                  pr: 4,
                  mb: 0.75,
                  borderRadius: 2,
                  bgcolor: n.read ? 'transparent' : 'rgba(232, 24, 56, 0.04)',
                  borderLeft: '3px solid',
                  borderColor: n.read ? 'transparent' : '#E81838',
                  cursor: 'pointer',
                  transition: 'background-color 150ms ease',
                  '&:hover': {
                    bgcolor: n.read ? 'rgba(40, 40, 56, 0.04)' : 'rgba(232, 24, 56, 0.08)',
                    '& .dismiss-btn': { opacity: 1 },
                  },
                }}
              >
                <Box sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                  <Box
                    sx={{
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      bgcolor: n.read ? 'rgba(40, 40, 56, 0.06)' : '#F8D0D8',
                      color: n.read ? 'text.secondary' : '#E81838',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      mt: 0.25,
                    }}
                  >
                    {n.read ? (
                      <NotificationsNoneOutlinedIcon sx={{ fontSize: '1rem' }} />
                    ) : (
                      <NotificationsActiveOutlinedIcon sx={{ fontSize: '1rem' }} />
                    )}
                  </Box>

                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: n.read ? 600 : 700,
                        fontSize: '0.85rem',
                        color: 'text.primary',
                        lineHeight: 1.35,
                        mb: 0.4,
                      }}
                    >
                      {n.title}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ display: 'block', fontSize: '0.8rem', lineHeight: 1.45 }}
                    >
                      {n.message}
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{ display: 'block', mt: 0.75, fontSize: '0.7rem', color: 'text.disabled', fontWeight: 500 }}
                    >
                      {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
                    </Typography>
                  </Box>
                </Box>

                {/* Cross / Dismiss button for individual notification */}
                <Tooltip title="Clear notification">
                  <IconButton
                    className="dismiss-btn"
                    size="small"
                    onClick={(e) => handleDismissItem(e, n)}
                    sx={{
                      position: 'absolute',
                      top: 8,
                      right: 8,
                      p: 0.5,
                      color: 'text.disabled',
                      opacity: { xs: 1, sm: 0.7 },
                      transition: 'opacity 150ms ease, color 150ms ease',
                      '&:hover': { color: 'error.main', bgcolor: 'rgba(232,24,56,0.08)' },
                    }}
                  >
                    <CloseIcon sx={{ fontSize: '0.9rem' }} />
                  </IconButton>
                </Tooltip>
              </Box>
            ))
          )}
        </Box>
      </Popover>
    </>
  );
}
