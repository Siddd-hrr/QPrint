import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import Spinner from './ui/Spinner';

export default function ProtectedRoute() {
  const { isAuthenticated, initialized } = useAuthStore((s) => ({
    isAuthenticated: s.isAuthenticated,
    initialized: s.initialized,
  }));

  if (!initialized) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner label="Preparing your session" />
      </div>
    );
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}
