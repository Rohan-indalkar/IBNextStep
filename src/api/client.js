import axios from 'axios';

// Backend runs on Spring Boot default port 8080 with base path /api
// (see SecurityConfig — CORS is open, JWT is a Bearer token, PUBLIC_ENDPOINTS
// covers /api/auth/**, /api/health, swagger, and /ws/**).
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('ibns_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ibns_token');
      localStorage.removeItem('ibns_role');
      localStorage.removeItem('ibns_email');
      if (!window.location.pathname.startsWith('/auth')) {
        window.location.href = '/auth/login';
      }
    }
    // Every controller returns the ApiResponse<T> envelope — normalize the
    // error message so callers can just read err.message.
    const message =
      error.response?.data?.message || error.message || 'Something went wrong. Please try again.';
    return Promise.reject({ ...error, message });
  }
);

export default client;
