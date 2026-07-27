export default function ProctoringBar({ anomalyCount = 0, maxAnomalies = 5 }) {
  const severity =
    anomalyCount >= maxAnomalies - 1
      ? 'critical'
      : anomalyCount > 0
        ? 'warning'
        : 'active';

  const config = {
    active: { color: 'var(--green)', label: 'Proctoring Active' },
    warning: { color: 'var(--yellow)', label: 'Warning' },
    critical: { color: 'var(--red)', label: 'Critical' },
  };

  const { color, label } = config[severity];

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '6px 14px',
        borderRadius: 'var(--radius-sm)',
        background: 'var(--bg-secondary)',
        border: '1px solid var(--border)',
        fontSize: 13,
        fontWeight: 500,
        color: 'var(--text)',
        flexShrink: 0,
      }}
    >
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: color,
          boxShadow: `0 0 6px ${color}`,
          flexShrink: 0,
        }}
      />
      <span style={{ color }}>{label}</span>
      <span
        style={{
          color: 'var(--text-secondary)',
          fontSize: 12,
          borderLeft: '1px solid var(--border)',
          paddingLeft: 8,
          marginLeft: 4,
        }}
      >
        {anomalyCount}/{maxAnomalies} anomalies
      </span>
    </div>
  );
}
