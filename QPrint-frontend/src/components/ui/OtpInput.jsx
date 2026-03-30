import { useEffect, useMemo, useRef } from 'react';
import clsx from 'clsx';

export default function OtpInput({ value, onChange, length = 6, error }) {
  const inputsRef = useRef([]);
  const chars = useMemo(() => value?.split('') || [], [value]);

  const handleChange = (idx, val) => {
    const digit = val.replace(/\D/g, '').slice(-1);
    const next = chars.slice();
    next[idx] = digit;
    const joined = next.join('').slice(0, length);
    onChange(joined);
    if (digit && idx < length - 1) inputsRef.current[idx + 1]?.focus();
  };

  const handleKeyDown = (idx, e) => {
    if (e.key === 'Backspace' && !chars[idx] && idx > 0) {
      const prev = idx - 1;
      inputsRef.current[prev]?.focus();
      const next = chars.slice();
      next[prev] = '';
      onChange(next.join(''));
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
    onChange(pasted);
    const targetIndex = Math.min(pasted.length, length - 1);
    inputsRef.current[targetIndex]?.focus();
  };

  useEffect(() => {
    if (!chars.length) inputsRef.current[0]?.focus();
  }, [chars.length]);

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-center gap-3" onPaste={handlePaste}>
        {Array.from({ length }).map((_, idx) => (
          <input
            key={idx}
            ref={(el) => (inputsRef.current[idx] = el)}
            value={chars[idx] ?? ''}
            inputMode="numeric"
            maxLength={1}
            className={clsx(
              'otp-input h-12 w-11 rounded-lg border border-white/15 bg-white/5 text-center text-lg font-mono text-white focus:outline-none focus:border-primary/60 focus:shadow-[0_0_0_3px_rgba(99,102,241,0.3)] transition',
              error && 'border-danger/70 focus:border-danger/70 focus:shadow-[0_0_0_3px_rgba(239,68,68,0.3)]'
            )}
            onChange={(e) => handleChange(idx, e.target.value)}
            onKeyDown={(e) => handleKeyDown(idx, e)}
          />
        ))}
      </div>
      {error && <p className="text-center text-xs text-danger">{error}</p>}
    </div>
  );
}
