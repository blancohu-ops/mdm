type StatCardProps = {
  value: string;
  label: string;
};

export function StatCard({ value, label }: StatCardProps) {
  return (
    <div className="min-w-[9rem] rounded-2xl border border-white/12 bg-white/8 px-5 py-4 backdrop-blur-md">
      <div className="font-display text-3xl font-extrabold text-white">{value}</div>
      <div className="mt-1 text-xs uppercase tracking-[0.24em] text-white/60">{label}</div>
    </div>
  );
}
