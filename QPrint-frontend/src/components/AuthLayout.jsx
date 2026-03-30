import { motion } from 'framer-motion';
import { ArrowRight, ShieldCheck, Sparkles } from 'lucide-react';
import Logo from './Logo';

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-background text-white px-4 py-10">
      <div className="auth-bg" />
      <div className="floating-shape bg-primary/40" style={{ top: '8%', left: '6%' }} />
      <div className="floating-shape bg-secondary/30" style={{ bottom: '12%', right: '8%', animationDelay: '2s' }} />

      <div className="relative z-10 max-w-6xl mx-auto grid gap-8 md:grid-cols-[1.1fr_0.9fr] items-center">
        <div className="space-y-6">
          <Logo size="text-3xl" />
          <div className="space-y-3">
            <motion.h1
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4 }}
              className="text-4xl md:text-5xl font-display font-semibold"
            >
              {title}
            </motion.h1>
            {subtitle && <p className="text-gray-300 max-w-xl leading-relaxed">{subtitle}</p>}
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <FeaturePill icon={<ShieldCheck size={18} />} label="Secure by design" />
            <FeaturePill icon={<Sparkles size={18} />} label="Built for students" />
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45 }}
          className="glass-card p-7 md:p-8 shadow-2xl border border-white/10"
        >
          {children}
        </motion.div>
      </div>
    </div>
  );
}

function FeaturePill({ icon, label }) {
  return (
    <div className="flex items-center gap-2 rounded-full bg-white/5 px-3 py-2 text-sm text-gray-200 border border-white/10">
      <span className="text-primary">{icon}</span>
      <span>{label}</span>
      <ArrowRight size={14} className="ml-auto text-gray-400" />
    </div>
  );
}
