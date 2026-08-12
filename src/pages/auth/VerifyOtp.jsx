import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import { verifyLoginOtp, login } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';
import { TextField, Button, Box, Typography } from '@mui/material';

const ROLE_HOME = {
  ADMIN: '/app/admin',
  HR: '/app/hr',
  TRAINER: '/app/trainer',
  STUDENT: '/app/student',
};

export default function VerifyOtp() {
  const navigate = useNavigate();
  const location = useLocation();
  const { applySession } = useAuth();
  const email = location.state?.email || '';
  const password = location.state?.password || '';
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!email) {
      setError('Session expired — please log in again.');
      return;
    }
    if (!/^\d{6}$/.test(otp)) {
      setError('Enter the full 6-digit code.');
      return;
    }
    setLoading(true);
    try {
      const res = await verifyLoginOtp({ email, otp });
      const auth = res.data.data; // ApiResponse<AuthResponse>
      applySession(auth);
      if (auth.mustChangePassword) {
        navigate('/auth/change-password', { replace: true });
        return;
      }
      navigate(ROLE_HOME[auth.role] || '/app', { replace: true });
    } catch (err) {
      setError(err.message || 'That code is incorrect. Please try again.');
      setOtp('');
    } finally {
      setLoading(false);
    }
  }

  function handleOtpChange(e) {
    const digitsOnly = e.target.value.replace(/\D/g, '').slice(0, 6);
    setOtp(digitsOnly);
    if (error) setError('');
  }

  async function handleResend() {
    setError('');
    if (!password) {
      setError('Please go back and log in again to resend a code.');
      return;
    }
    setResending(true);
    try {
      // AuthController has no dedicated resend endpoint — re-calling
      // /login (with the credentials already validated once) is what
      // re-triggers the OTP email.
      await login({ email, password });
    } catch (err) {
      setError('Could not resend — please go back and log in again.');
    } finally {
      setResending(false);
    }
  }

  if (!email) {
    return (
      <AuthLayout title="Session expired" subtitle="We couldn't find an email to verify.">
        <Button variant="contained" color="primary" onClick={() => navigate('/auth/login')}>
          Back to login
        </Button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Check your email" subtitle={`Enter the OTP sent to ${email}.`}>
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
          autoComplete="one-time-code"
          required
          fullWidth
          value={otp}
          onChange={handleOtpChange}
          placeholder="6-digit code"
          error={Boolean(error)}
          sx={{
            '& input': { letterSpacing: '0.4em', textAlign: 'center', fontSize: '20px', fontWeight: 700 }
          }}
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
          {loading ? 'Verifying…' : 'Verify & continue'}
        </Button>

        <Button
          type="button"
          onClick={handleResend}
          disabled={resending}
          sx={{ textTransform: 'none', fontWeight: 600, mt: 1, alignSelf: 'center' }}
        >
          {resending ? 'Resending…' : "Didn't get a code? Resend"}
        </Button>
      </Box>
    </AuthLayout>
  );
}
