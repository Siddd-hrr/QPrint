import { Suspense, lazy, useEffect, useState } from 'react';
import { BrowserRouter, Route, Routes, Navigate } from 'react-router-dom';

import ProtectedRoute from './components/ProtectedRoute';
import Spinner from './components/ui/Spinner';
import LoginPage from './pages/Login';
import RegisterPage from './pages/Register';
import VerifyEmailPage from './pages/VerifyEmail';
import ForgotPasswordPage from './pages/ForgotPassword';
import ResetPasswordPage from './pages/ResetPassword';
import UploadPage from './pages/Upload';
import CartPage from './pages/Cart';
import CheckoutPage from './pages/Checkout';
import CheckoutSuccessPage from './pages/CheckoutSuccess';
import OrdersPage from './pages/Orders';
import OrderDetailPage from './pages/OrderDetail';
import TransactionsPage from './pages/Transactions';
import TransactionDetailPage from './pages/TransactionDetail';
import HomePage from './pages/Home';
import SettingsPage from './pages/Settings';
import api from './api/axios';
import { useAuthStore } from './store/authStore';

const Placeholder = lazy(() => import('./pages/Placeholder'));

export default function App() {
  const [bootstrapping, setBootstrapping] = useState(true);
  const setAuth = useAuthStore((s) => s.setAuth);
  const setUser = useAuthStore((s) => s.setUser);
  const setInitialized = useAuthStore((s) => s.setInitialized);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const { data } = await api.post('/auth/refresh', {});
        const accessToken = data?.data?.accessToken;
        if (accessToken) {
          setAuth(accessToken, null);
          const me = await api.get('/auth/me');
          setUser(me?.data?.data);
        }
      } catch {
        useAuthStore.getState().clearAuth();
      } finally {
        setInitialized(true);
        setBootstrapping(false);
      }
    };

    bootstrap();
  }, [setAuth, setUser, setInitialized]);

  if (bootstrapping) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Loading QPrint" />
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Suspense fallback={<div className="text-white p-6">Loading...</div>}>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/home" element={<HomePage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/checkout/success" element={<CheckoutSuccessPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/:orderId" element={<OrderDetailPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/transactions/:id" element={<TransactionDetailPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>

          <Route path="*" element={<Placeholder title="Not Found" />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
