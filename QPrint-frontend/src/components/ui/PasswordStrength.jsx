import clsx from 'clsx';
import { Check, X } from 'lucide-react';
import { getPasswordChecks, getPasswordScore } from '../../utils/password';

export default function PasswordStrength({ password }) {
  const checks = getPasswordChecks(password);
  const score = getPasswordScore(checks);
  const labels = ['Weak', 'Weak', 'Okay', 'Strong', 'Very Strong'];
  const label = labels[Math.max(0, score - 1)] || 'Weak';

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2 text-xs text-gray-300">
        <span className="uppercase tracking-wide text-gray-400">Strength</span>
        <span className={clsx('font-semibold', score >= 4 ? 'text-success' : score >= 3 ? 'text-secondary' : 'text-danger')}>
          {label}
        </span>
      </div>
      <div className="flex gap-1">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={clsx(
              'h-1.5 w-full rounded-full transition',
              score >= i ? (score >= 3 ? 'bg-primary' : 'bg-danger') : 'bg-white/10'
            )}
          />
        ))}
      </div>
      <div className="grid gap-2 text-xs text-gray-300 sm:grid-cols-2">
        {checks.map((check) => (
          <div key={check.label} className="flex items-center gap-2">
            {check.ok ? (
              <Check className="h-3.5 w-3.5 text-success" />
            ) : (
              <X className="h-3.5 w-3.5 text-danger" />
            )}
            <span className={clsx(check.ok ? 'text-gray-200' : 'text-gray-400')}>{check.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
