import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Menu, MenuItem, Divider, Typography, Box, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Chip } from '@mui/material';
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getProfile, updateProfile, changePassword } from '../api/profile';

const ROLE_DISPLAY_NAMES = {
  ADMIN: 'Administrator',
  HR: 'HR Recruiter',
  TRAINER: 'Trainer',
  STUDENT: 'Student',
};

export default function ProfileDropdown({ dark = false }) {
  const { email, role: authRole, signOut } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [anchorEl, setAnchorEl] = useState(null);
  const [profile, setProfile] = useState(null);
  const [editOpen, setEditOpen] = useState(false);
  const [pwOpen, setPwOpen] = useState(false);

  useEffect(() => {
    getProfile()
      .then((res) => setProfile(res.data.data))
      .catch(() => {});
  }, []);

  const open = Boolean(anchorEl);
  const handleClick = (e) => setAnchorEl(e.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const initials = profile ? `${profile.firstName?.[0] || ''}${profile.lastName?.[0] || ''}`.toUpperCase() : (email?.[0] || '?').toUpperCase();
  const displayName = profile ? `${profile.firstName} ${profile.lastName}` : email;
  const userRole = profile?.role || authRole || 'USER';
  const roleBadge = ROLE_DISPLAY_NAMES[userRole] || userRole;

  async function handleLogout() {
    handleClose();
    await signOut();
    navigate('/auth/login', { replace: true });
  }

  return (
    <>
      <IconButton onClick={handleClick} size="small" sx={{ ml: 1, p: 0.5, border: '1px solid', borderColor: dark ? 'rgba(255, 255, 255, 0.20)' : 'divider', borderRadius: 8 }}>
        <Avatar sx={{ width: 32, height: 32, bgcolor: '#E81838', color: '#FFFFFF', fontSize: 13, fontWeight: 700 }}>
          {initials}
        </Avatar>
        <Typography variant="body2" sx={{ ml: 1, mr: 1, fontWeight: 600, color: dark ? '#FFFFFF' : 'text.primary', display: { xs: 'none', sm: 'block' }, maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {displayName}
        </Typography>
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        PaperProps={{ sx: { width: 230, mt: 1, borderRadius: 3, p: 0.5, boxShadow: '0 8px 30px rgba(0,0,0,0.12)' } }}
      >
        <Box sx={{ px: 2, pt: 1.5, pb: 1.5, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.5 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, fontSize: '0.975rem', color: 'text.primary', lineHeight: 1.2 }}>
            {displayName}
          </Typography>
          <Typography variant="body2" color="text.secondary" noWrap sx={{ fontSize: '0.8125rem', width: '100%' }}>
            {email}
          </Typography>
          <Chip
            label={roleBadge}
            size="small"
            sx={{
              bgcolor: 'primary.main',
              color: '#FFFFFF',
              fontWeight: 700,
              borderRadius: 999,
              height: 24,
              fontSize: '0.75rem',
              mt: 1,
            }}
          />
        </Box>
        <Divider sx={{ my: 0.5 }} />
        <MenuItem onClick={() => { handleClose(); setEditOpen(true); }} sx={{ py: 1, px: 2, borderRadius: 1.5 }}>
          <PersonOutlinedIcon sx={{ mr: 1.5, fontSize: 20, color: 'text.secondary' }} />
          <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
            My profile
          </Typography>
        </MenuItem>
        <MenuItem onClick={() => { handleClose(); setPwOpen(true); }} sx={{ py: 1, px: 2, borderRadius: 1.5 }}>
          <LockOutlinedIcon sx={{ mr: 1.5, fontSize: 20, color: 'text.secondary' }} />
          <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
            Change password
          </Typography>
        </MenuItem>
        <Divider sx={{ my: 0.5 }} />
        <MenuItem onClick={handleLogout} sx={{ py: 1, px: 2, borderRadius: 1.5 }}>
          <LogoutIcon sx={{ mr: 1.5, fontSize: 20, color: 'error.main' }} />
          <Typography variant="body2" sx={{ fontWeight: 600, color: 'error.main' }}>
            Log out
          </Typography>
        </MenuItem>
      </Menu>

      <EditProfileModal open={editOpen} onClose={() => setEditOpen(false)} profile={profile} onSaved={(p) => { setProfile(p); toast.success('Profile updated.'); }} />
      <ChangePasswordModal open={pwOpen} onClose={() => setPwOpen(false)} onSaved={() => toast.success('Password changed.')} />
    </>
  );
}

function EditProfileModal({ open, onClose, profile, onSaved }) {
  const [firstName, setFirstName] = useState(profile?.firstName || '');
  const [lastName, setLastName] = useState(profile?.lastName || '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setFirstName(profile?.firstName || '');
      setLastName(profile?.lastName || '');
      setError('');
    }
  }, [open, profile]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const res = await updateProfile({ firstName, lastName });
      onSaved(res.data.data);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Edit profile</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="First name" required value={firstName} onChange={(e) => setFirstName(e.target.value)} fullWidth />
          <TextField label="Last name" required value={lastName} onChange={(e) => setLastName(e.target.value)} fullWidth />
          {error && <Typography color="error" variant="body2">{error}</Typography>}
        </DialogContent>
        <DialogActions sx={{ p: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Saving…' : 'Save changes'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

function ChangePasswordModal({ open, onClose, onSaved }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setCurrentPassword('');
      setNewPassword('');
      setConfirm('');
      setError('');
    }
  }, [open]);

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
    setSaving(true);
    try {
      await changePassword({ currentPassword, newPassword });
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Change password</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="Current password" type="password" required value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} fullWidth />
          <TextField label="New password" type="password" required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="At least 8 characters" fullWidth />
          <TextField label="Confirm new password" type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)} fullWidth />
          {error && <Typography color="error" variant="body2">{error}</Typography>}
        </DialogContent>
        <DialogActions sx={{ p: 2, pt: 0 }}>
          <Button onClick={onClose} color="inherit">Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Saving…' : 'Change password'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
