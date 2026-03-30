export default function Placeholder({ title }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background text-white">
      <div className="glass-card px-8 py-6 text-center max-w-md w-full">
        <h1 className="text-2xl font-semibold mb-3">{title}</h1>
        <p className="text-sm text-gray-300">UI coming soon per Phase 1 spec.</p>
      </div>
    </div>
  );
}
