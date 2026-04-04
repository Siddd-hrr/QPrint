import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Logo from './Logo';

export default function Header() {
  const navigate = useNavigate();

  return (
    <div className="w-full bg-gradient-to-r from-primary/10 to-secondary/10 border-b border-white/10 px-4 py-4 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="text-gray-400 hover:text-white transition-colors"
          title="Go back"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <button
          onClick={() => navigate('/home')}
          className="flex items-center gap-2 hover:opacity-80 transition-opacity"
          title="Go to Home"
        >
          <Logo size="text-lg" />
          <span className="text-sm text-gray-300">QPrint</span>
        </button>
      </div>
    </div>
  );
}