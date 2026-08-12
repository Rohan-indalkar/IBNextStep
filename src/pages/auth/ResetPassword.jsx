import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import PasswordInput from '../../components/PasswordInput';
import { resetPassword } from '../../api/auth';
import { TextField, Button, Box, Typography } from '@mui/material';

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || '';

  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function handleOtpChange(e) {
    setOtp(e.target.value.replace(/\D/g, '').slice(0, 6));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!/^\d{6}$/.test(otp)) {
      setError('Enter the full 6-digit code.');
      return;
    }
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
      await resetPassword({ email, otp, newPassword });
      navigate('/auth/login', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (!email) {
    return (
      <AuthLayout title="Session expired" subtitle="We couldn't find an email to reset.">
        <Button variant="contained" color="primary" onClick={() => navigate('/auth/forgot-password')}>
          Start over
        </Button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Enter your new password" subtitle={`Enter the code sent to ${email} and choose a new password.`}>
      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          id="otp"
          label="One-time code"
          type="text"
          inputProps={{
            inputMode: 'numeric',
            pattern: '\\d{6}',
            maxLength: 6,
          }}
          required
          fullWidth
          value={otp}
          onChange={handleOtpChange}
          placeholder="6-digit code"
          sx={{
            '& input': { letterSpacing: '0.3em', textAlign: 'center', fontSize: '18px' }
          }}
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
          placeholder="Re-enter password"
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
          disabled={loading || otp.length !== 6}
          sx={{ mt: 1 }}
        >
          {loading ? 'Saving…' : 'Set new password'}
        </Button>
      </Box>
    </AuthLayout>
  );
}
