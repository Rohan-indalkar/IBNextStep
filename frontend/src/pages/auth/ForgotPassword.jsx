import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import { forgotPassword } from '../../api/auth';
import { TextField, Button, Box, Typography } from '@mui/material';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await forgotPassword({ email });
      navigate('/auth/reset-password', { state: { email } });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Reset your password" subtitle="We'll email you a one-time code to reset it.">
      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          required
          fullWidth
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@infobeans.com"
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
          {loading ? 'Sending…' : 'Send reset code'}
        </Button>

        <Button
          type="button"
          onClick={() => navigate('/auth/login')}
          sx={{ textTransform: 'none', fontWeight: 600, mt: 1, alignSelf: 'center' }}
        >
          Back to login
        </Button>
      </Box>
    </AuthLayout>
  );
}
