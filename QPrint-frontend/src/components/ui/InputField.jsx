import { forwardRef, useState } from 'react';
import clsx from 'clsx';
import { Eye, EyeOff } from 'lucide-react';

export const InputField = forwardRef(function InputField(
  { label, error, icon: Icon, rightSlot, className = '', ...props },
  ref
) {
  return (
    <label className={clsx('block space-y-2', className)}>
      {label && <span className="text-sm text-gray-200">{label}</span>}
      <div className={clsx('flex items-center gap-2 rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 focus-within:border-primary/60 focus-within:bg-white/10 focus-within:shadow-[0_0_0_4px_rgba(99,102,241,0.12)] transition', error && 'border-danger/60')}>
        {Icon && <Icon className="h-4 w-4 text-gray-400" />}
        <input
          ref={ref}
          className="w-full bg-transparent text-sm text-white placeholder:text-gray-500 focus:outline-none"
          {...props}
        />
        {rightSlot}
      </div>
      {error && <p className="text-xs text-danger">{error}</p>}
    </label>
  );
});

export const PasswordField = forwardRef(function PasswordField({ label, error, ...props }, ref) {
  const [show, setShow] = useState(false);
  return (
    <InputField
      label={label}
      type={show ? 'text' : 'password'}
      error={error}
      ref={ref}
      rightSlot={
        <button type="button" className="text-gray-400 hover:text-white" onClick={() => setShow((v) => !v)}>
          {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      }
      {...props}
    />
  );
});
