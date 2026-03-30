export default function Logo({ size = 'text-2xl' }) {
  return (
    <div className={`flex items-center gap-2 font-display font-semibold ${size}`}>
      <div className="relative">
        <span className="text-primary">Print</span>
        <span className="text-secondary">Ease</span>
      </div>
    </div>
  );
}
