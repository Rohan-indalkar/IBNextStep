import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const ROLE_HOME = {
  ADMIN: '/app/admin',
  HR: '/app/hr',
  TRAINER: '/app/trainer',
  STUDENT: '/app/student',
};

export default function AppIndex() {
  const { role } = useAuth();
  return <Navigate to={ROLE_HOME[role] || '/auth/login'} replace />;
}
