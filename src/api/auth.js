import client from './client';

// Maps 1:1 to com.infobeans.ibnextstep.auth.AuthController.
// Login is a two-step OTP flow: POST /login sends an OTP to the user's
// registered email, then POST /verify-login-otp exchanges (email, otp)
// for the AuthResponse { token, role, email, mustChangePassword }.

export function login({ email, password }) {
  return client.post('/auth/login', { email, password });
}

export function verifyLoginOtp({ email, otp }) {
  return client.post('/auth/verify-login-otp', { email, otp });
}

export function forgotPassword({ email }) {
  return client.post('/auth/forgot-password', { email });
}

export function resetPassword({ email, otp, newPassword }) {
  return client.post('/auth/reset-password', { email, otp, newPassword });
}

export function logout() {
  return client.post('/auth/logout');
}
