import { useState } from 'react';
import { Card, CardContent, TextField, MenuItem, Button, Typography, Box } from '@mui/material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import { useToast } from '../../context/ToastContext';
import { composeNotification } from '../../api/notification';

const AUDIENCES = [
  { value: '', label: 'Everyone' },
  { value: 'ADMIN', label: 'Admins' },
  { value: 'HR', label: 'HR / Recruiters' },
  { value: 'TRAINER', label: 'Trainers' },
  { value: 'STUDENT', label: 'Students' },
];

export default function Notifications() {
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [audience, setAudience] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSending(true);
    try {
      await composeNotification({ title, message, audience: audience || null });
      setTitle('');
      setMessage('');
      setAudience('');
      toast.success('Notification sent.');
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader title="Broadcast notification" subtitle="Send a message to everyone, or a specific role." />

      <Card sx={{ maxWidth: 560 }}>
        <CardContent sx={{ p: 3 }}>
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <TextField
              label="Title"
              required
              fullWidth
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Platform maintenance tonight"
              size="small"
            />
            <TextField
              select
              label="Audience"
              fullWidth
              value={audience}
              onChange={(e) => setAudience(e.target.value)}
              size="small"
            >
              {AUDIENCES.map((a) => (
                <MenuItem key={a.value} value={a.value}>
                  {a.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Message"
              required
              fullWidth
              multiline
              rows={5}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="What do they need to know?"
              size="small"
            />

            {error && <Typography color="error" variant="body2">{error}</Typography>}

            <Box sx={{ mt: 1 }}>
              <Button variant="contained" color="primary" type="submit" disabled={sending} fullWidth>
                {sending ? 'Sending…' : 'Send notification'}
              </Button>
            </Box>
          </form>
        </CardContent>
      </Card>
    </AppShell>
  );
}
