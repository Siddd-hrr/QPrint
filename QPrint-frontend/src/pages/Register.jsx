import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Mail, Lock, UserRound, UserPlus } from 'lucide-react';
import toast from 'react-hot-toast';

import AuthLayout from '../components/AuthLayout';
import Button from '../components/ui/Button';
import { InputField, PasswordField } from '../components/ui/InputField';
import PasswordStrength from '../components/ui/PasswordStrength';
import api from '../api/axios';
import { getErrorMessage } from '../utils/api';
import { passwordRegex } from '../utils/password';

const schema = z
  .object({
    firstName: z.string().min(2, 'First name must be at least 2 characters').max(50),
    lastName: z.string().min(2, 'Last name must be at least 2 characters').max(50),
    email: z.string().email('Enter a valid email address'),
    password: z.string().regex(passwordRegex(), 'Must include upper, lower, digit and special character'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords must match',
    path: ['confirmPassword'],
  });

export default function RegisterPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [passwordValue, setPasswordValue] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
    mode: 'onChange',
    reValidateMode: 'onChange',
  });

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      const payload = {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim().toLowerCase(),
        password: values.password,
      };
      const { data } = await api.post('/auth/register', payload);
      const userId = data?.data?.userId;
      toast.success('Registration successful. Check your email for the code!');
      navigate(`/verify-email?userId=${userId}&email=${encodeURIComponent(payload.email)}`);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create account.'));
    } finally {
      setLoading(false);
    }
  };

  const password = watch('password') || passwordValue;

  return (
    <AuthLayout
      title="Create your QPrint account"
      subtitle="Set up your profile so you can upload files, pay securely, and collect prints with a simple OTP."
    >
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid gap-4 sm:grid-cols-2">
          <InputField
            label="First name"
            placeholder="Aarav"
            icon={UserRound}
            error={errors.firstName?.message}
            {...register('firstName')}
          />
          <InputField
            label="Last name"
            placeholder="Sharma"
            icon={UserRound}
            error={errors.lastName?.message}
            {...register('lastName')}
          />
        </div>

        <InputField
          label="Email"
          type="email"
          placeholder="you@university.edu"
          icon={Mail}
          error={errors.email?.message}
          {...register('email')}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <PasswordField
            label="Password"
            placeholder="Create a strong password"
            icon={Lock}
            error={errors.password?.message}
            {...register('password', {
              onChange: (e) => setPasswordValue(e.target.value),
            })}
          />
          <PasswordField
            label="Confirm password"
            placeholder="Re-enter password"
            icon={Lock}
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />
        </div>

        <PasswordStrength password={password} />

        <Button type="submit" loading={loading}>
          <UserPlus className="h-4 w-4" />
          Create account
        </Button>

        <p className="text-sm text-gray-300 text-center">
          Already have an account?{' '}
          <Link to="/login" className="text-primary hover:text-primary/80 font-semibold">
            Sign in
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
