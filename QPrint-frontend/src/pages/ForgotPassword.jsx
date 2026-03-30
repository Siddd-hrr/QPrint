import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Mail, Send } from 'lucide-react';
import toast from 'react-hot-toast';

import AuthLayout from '../components/AuthLayout';
import Button from '../components/ui/Button';
import { InputField } from '../components/ui/InputField';
import api from '../api/axios';
import { getErrorMessage } from '../utils/api';

const schema = z.object({ email: z.string().email('Enter a valid email') });

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(schema), mode: 'onBlur' });

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', { email: values.email.trim().toLowerCase() });
      toast.success('If that email is registered, a reset link has been sent.');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Forgot your password?"
      subtitle="No worries. Enter your email and we will send you a secure reset link."
    >
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <InputField
          label="Email"
          type="email"
          placeholder="you@university.edu"
          icon={Mail}
          error={errors.email?.message}
          {...register('email')}
        />

        <Button type="submit" loading={loading}>
          <Send className="h-4 w-4" />
          Send reset link
        </Button>

        <p className="text-sm text-gray-300 text-center">
          Remembered your password?{' '}
          <Link to="/login" className="text-primary hover:text-primary/80 font-semibold">
            Back to login
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
