import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Mail, Lock, LogIn } from 'lucide-react';
import toast from 'react-hot-toast';

import AuthLayout from '../components/AuthLayout';
import Button from '../components/ui/Button';
import { InputField, PasswordField } from '../components/ui/InputField';
import api from '../api/axios';
import { useAuthStore } from '../store/authStore';
import { getErrorMessage } from '../utils/api';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const setUser = useAuthStore((s) => s.setUser);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(schema), mode: 'onBlur' });

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      const { data } = await api.post('/auth/login', values);
      const payload = data?.data || {};

      if (payload.needsVerification && payload.userId) {
        toast.error('Please verify your email to continue.');
        navigate(`/verify-email?userId=${payload.userId}&email=${encodeURIComponent(values.email)}`);
        return;
      }

      const accessToken = payload.accessToken;
      if (!accessToken) throw new Error('Missing access token');

      setAuth(accessToken, null);
      const me = await api.get('/auth/me');
      setUser(me?.data?.data);

      toast.success('Welcome back to QPrint!');
      navigate('/home');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Unable to log in. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Print smarter. Not harder."
      subtitle="Skip the queues, upload your files, and collect prints with a single OTP. Secure, fast, and built for campus life."
    >
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-4">
          <InputField
            label="Email"
            type="email"
            placeholder="you@university.edu"
            icon={Mail}
            error={errors.email?.message}
            {...register('email')}
          />
          <PasswordField
            label="Password"
            placeholder="••••••••"
            icon={Lock}
            error={errors.password?.message}
            {...register('password')}
          />
        </div>

        <div className="flex items-center justify-between text-sm text-gray-300">
          <Link to="/forgot-password" className="text-primary hover:text-primary/80 font-medium">
            Forgot password?
          </Link>
          <Link to="/register" className="text-secondary hover:text-secondary/80 font-medium">
            New here? Create account
          </Link>
        </div>

        <Button type="submit" loading={loading}>
          <LogIn className="h-4 w-4" />
          Sign in
        </Button>
      </form>
    </AuthLayout>
  );
}
