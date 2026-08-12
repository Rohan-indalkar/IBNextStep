import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ allow }) {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }
  if (allow && !allow.includes(role)) {
    return <Navigate to="/app" replace />;
  }
  return <Outlet />;
}
