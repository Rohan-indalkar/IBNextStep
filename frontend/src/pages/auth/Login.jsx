import { useState } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import AuthLayout from '../../components/AuthLayout';
import PasswordInput from '../../components/PasswordInput';
import { login } from '../../api/auth';
import { TextField, Button, Typography, Box, Chip } from '@mui/material';

const ROLE_LABELS = {
  student: 'Student',
  trainer: 'Trainer',
  hr: 'HR Recruiter',
  admin: 'Administrator',
};

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const activeRole = location.state?.role || searchParams.get('role');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login({ email, password });
      navigate('/auth/verify-otp', { state: { email, password } });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle={activeRole ? `Sign in to access your ${ROLE_LABELS[activeRole] || activeRole} portal.` : "Sign in to continue to IBNextStep."}
    >
      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {activeRole && (
          <Box sx={{ mb: 1 }}>
            <Chip
              label={`Role: ${ROLE_LABELS[activeRole] || activeRole}`}
              color="primary"
              variant="outlined"
              size="small"
              sx={{ fontWeight: 700 }}
            />
          </Box>
        )}
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
          margin="none"
        />
        
        <PasswordInput
          id="password"
          label="Password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
        />

        {error && <Typography color="error" variant="body2">{error}</Typography>}

        <Button 
          type="submit" 
          variant="contained" 
          color="primary" 
          fullWidth 
          disabled={loading} 
          sx={{ mt: 1 }}
        >
          {loading ? 'Sending OTP…' : 'Continue'}
        </Button>

        <Button
          type="button"
          onClick={() => navigate('/auth/forgot-password')}
          sx={{ textTransform: 'none', fontWeight: 600, mt: 1, alignSelf: 'center' }}
        >
          Forgot your password?
        </Button>
      </Box>
    </AuthLayout>
  );
}
