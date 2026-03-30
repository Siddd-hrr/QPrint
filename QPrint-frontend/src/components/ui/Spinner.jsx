import { Loader2 } from 'lucide-react';

export default function Spinner({ label }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-10 text-gray-200">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      {label && <p className="text-sm text-gray-400">{label}</p>}
    </div>
  );
}
