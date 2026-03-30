import clsx from 'clsx';
import { Loader2 } from 'lucide-react';

export default function Button({ children, loading = false, variant = 'primary', className = '', ...props }) {
  const base = 'inline-flex w-full items-center justify-center gap-2 rounded-lg px-4 py-3 text-sm font-semibold transition duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-60 disabled:cursor-not-allowed';
  const variants = {
    primary: 'bg-primary hover:bg-primary/90 text-white shadow-lg shadow-primary/25',
    ghost: 'bg-white/5 hover:bg-white/10 text-white border border-white/10',
    danger: 'bg-danger hover:bg-danger/90 text-white shadow-lg shadow-danger/25',
  };

  return (
    <button className={clsx(base, variants[variant], className)} disabled={loading || props.disabled} {...props}>
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  );
}
