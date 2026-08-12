import { useEffect, useState } from 'react';
import { IconButton, Badge, Popover, Box, Typography, ButtonBase } from '@mui/material';
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';
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

  async function handleOpenItem(n) {
    if (!n.read) {
      try {
        await markRead(n.id);
        setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      } catch {
        // non-critical
      }
    }
  }

  return (
    <>
      <IconButton color="inherit" onClick={handleClick}>
        <Badge badgeContent={unreadCount > 9 ? '9+' : unreadCount} color="error">
          <NotificationsNoneOutlinedIcon />
        </Badge>
      </IconButton>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 320, maxHeight: 420, overflowY: 'auto', mt: 1, p: 1 } }}
      >
        <Typography variant="overline" sx={{ px: 1.5, py: 1, display: 'block', fontWeight: 700, color: 'text.secondary' }}>
          Notifications
        </Typography>
        {loading ? (
          <Typography variant="body2" sx={{ p: 1.5, color: 'text.secondary' }}>Loading…</Typography>
        ) : error ? (
          <Typography variant="body2" color="error" sx={{ p: 1.5 }}>{error}</Typography>
        ) : items.length === 0 ? (
          <Typography variant="body2" sx={{ p: 1.5, color: 'text.secondary' }}>No notifications yet.</Typography>
        ) : (
          items.map((n) => (
            <ButtonBase
              key={n.id}
              onClick={() => handleOpenItem(n)}
              sx={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                bgcolor: n.read ? 'transparent' : 'action.hover',
                borderRadius: 1,
                p: 1.5,
                mb: 0.5,
              }}
            >
              <Typography variant="body2" sx={{ fontWeight: n.read ? 500 : 700, mb: 0.5 }}>
                {n.title}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                {n.message}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5, fontSize: 10 }}>
                {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
              </Typography>
            </ButtonBase>
          ))
        )}
      </Popover>
    </>
  );
}
