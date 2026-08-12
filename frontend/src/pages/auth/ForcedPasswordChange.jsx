import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import PasswordInput from '../../components/PasswordInput';
import { changePassword } from '../../api/profile';
import { useAuth } from '../../context/AuthContext';
import { Button, Box, Typography } from '@mui/material';

const ROLE_HOME = {
  ADMIN: '/app/admin',
  HR: '/app/hr',
  TRAINER: '/app/trainer',
  STUDENT: '/app/student',
};

export default function ForcedPasswordChange() {
  const navigate = useNavigate();
  const { role } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (newPassword !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setLoading(true);
    try {
      await changePassword({ currentPassword, newPassword });
      navigate(ROLE_HOME[role] || '/app', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Set a new password" subtitle="Your account requires a password change before continuing.">
      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <PasswordInput
          id="currentPassword"
          label="Current (temporary) password"
          autoComplete="current-password"
          required
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
        />
        
        <PasswordInput
          id="newPassword"
          label="New password"
          autoComplete="new-password"
          required
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="At least 8 characters"
        />
        
        <PasswordInput
          id="confirm"
          label="Confirm password"
          autoComplete="new-password"
          required
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />

        {error && (
          <Box role="alert" sx={{ background: 'var(--color-danger-bg)', p: 1.5, borderRadius: 1 }}>
            <Typography color="error" variant="body2">{error}</Typography>
          </Box>
        )}

        <Button 
          variant="contained" 
          color="primary" 
          type="submit" 
          fullWidth 
          disabled={loading}
          sx={{ mt: 1 }}
        >
          {loading ? 'Saving…' : 'Set new password'}
        </Button>
      </Box>
    </AuthLayout>
  );
}
