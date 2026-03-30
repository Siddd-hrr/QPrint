import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Lock, Shield } from 'lucide-react';
import toast from 'react-hot-toast';

import AuthLayout from '../components/AuthLayout';
import Button from '../components/ui/Button';
import { PasswordField } from '../components/ui/InputField';
import PasswordStrength from '../components/ui/PasswordStrength';
import api from '../api/axios';
import { getErrorMessage } from '../utils/api';
import { passwordRegex } from '../utils/password';

const schema = z
  .object({
    newPassword: z.string().regex(passwordRegex(), 'Must include upper, lower, digit and special character'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords must match',
    path: ['confirmPassword'],
  });

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [passwordValue, setPasswordValue] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm({ resolver: zodResolver(schema), mode: 'onBlur' });
  const password = watch('newPassword') || passwordValue;

  const onSubmit = async (values) => {
    if (!token) {
      toast.error('Reset link is missing or invalid.');
      return;
    }
    setLoading(true);
    try {
      await api.post('/auth/reset-password', {
        token,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      toast.success('Password reset! Please log in with your new password.');
      navigate('/login');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not reset password.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Set a new password"
      subtitle="Choose a strong password to secure your account."
    >
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid gap-4 sm:grid-cols-2">
          <PasswordField
            label="New password"
            placeholder="••••••••"
            icon={Lock}
            error={errors.newPassword?.message}
            {...register('newPassword', {
              onChange: (e) => setPasswordValue(e.target.value),
            })}
          />
          <PasswordField
            label="Confirm password"
            placeholder="Re-enter password"
            icon={Shield}
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />
        </div>

        <PasswordStrength password={password} />

        <Button type="submit" loading={loading}>
          Update password
        </Button>

        <p className="text-sm text-gray-300 text-center">
          Go back to{' '}
          <Link to="/login" className="text-primary hover:text-primary/80 font-semibold">
            login
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
