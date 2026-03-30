import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import toast from 'react-hot-toast';
import { Clock, RefreshCw, ShieldCheck } from 'lucide-react';

import AuthLayout from '../components/AuthLayout';
import Button from '../components/ui/Button';
import OtpInput from '../components/ui/OtpInput';
import api from '../api/axios';
import { getErrorMessage } from '../utils/api';

const schema = z.object({ code: z.string().length(6, 'Enter the 6-digit code') });

export default function VerifyEmailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const userId = params.get('userId');
  const email = params.get('email');

  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(45);
  const [expiresIn, setExpiresIn] = useState(15 * 60);

  useEffect(() => {
    if (!userId) navigate('/register');
  }, [userId, navigate]);

  useEffect(() => {
    const timer = setInterval(() => setExpiresIn((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (resendCooldown <= 0) return undefined;
    const timer = setInterval(() => setResendCooldown((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const {
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
    mode: 'onChange',
    reValidateMode: 'onChange',
    defaultValues: { code: '' },
  });

  const otp = watch('code');

  const timeParts = useMemo(() => {
    const minutes = Math.floor(expiresIn / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (expiresIn % 60).toString().padStart(2, '0');
    return `${minutes}:${seconds}`;
  }, [expiresIn]);

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      await api.post('/auth/verify-email', { userId, code: values.code });
      toast.success('Email verified! Your account is active.');
      navigate('/login');
    } catch (err) {
      setError('code', { message: getErrorMessage(err, 'Invalid or expired code') });
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!email) {
      toast.error('Missing email. Please register again.');
      return;
    }
    setResendCooldown(45);
    try {
      await api.post('/auth/resend-verification', { email });
      toast.success('New code sent to your email.');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Unable to resend code right now.'));
    }
  };

  return (
    <AuthLayout
      title="Verify your email"
      subtitle="Enter the 6-digit code we sent to your inbox. This keeps your account secure."
    >
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-3 text-center">
          <p className="text-gray-300 text-sm">Code sent to {email || 'your email address'}.</p>
          <OtpInput
            value={otp}
            onChange={(v) => setValue('code', v, { shouldValidate: true })}
            error={errors.code?.message}
          />
          <div className="flex items-center justify-center gap-3 text-xs text-gray-400">
            <span className="inline-flex items-center gap-1">
              <Clock className="h-4 w-4 text-secondary" /> Expires in {timeParts}
            </span>
            <span className="inline-flex items-center gap-1">
              <ShieldCheck className="h-4 w-4 text-success" /> Secure OTP
            </span>
          </div>
        </div>

        <Button type="submit" loading={loading}>
          Confirm & Activate
        </Button>

        <div className="flex items-center justify-between text-sm text-gray-300">
          <button
            type="button"
            className="inline-flex items-center gap-2 text-primary disabled:opacity-50"
            onClick={handleResend}
            disabled={resendCooldown > 0}
          >
            <RefreshCw className="h-4 w-4" />
            Resend Code {resendCooldown > 0 ? `(${resendCooldown}s)` : ''}
          </button>
          <Link to="/login" className="text-secondary hover:text-secondary/80 font-semibold">
            Back to login
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
}
