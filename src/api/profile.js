import client from './client';

// Maps to com.infobeans.ibnextstep.profile.ProfileController (authenticated).
export function getProfile() {
  return client.get('/profile');
}

export function updateProfile(payload) {
  return client.put('/profile', payload);
}

export function changePassword({ currentPassword, newPassword }) {
  return client.post('/profile/change-password', { currentPassword, newPassword });
}
