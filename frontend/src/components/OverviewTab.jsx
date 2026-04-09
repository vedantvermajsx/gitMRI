import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer, PieChart, Pie } from 'recharts'

const COLORS = ['#3b82f6','#f97316','#8b5cf6','#10b981','#f59e0b','#ec4899','#06b6d4','#84cc16']

function ScoreGauge({ score }) {
  score = Math.round(score)
  const r = 68, stroke = 12, circ = 2 * Math.PI * r
  const offset = circ * (1 - score / 100)
  const color = score >= 80 ? '#10b981' : score >= 60 ? '#f97316' : '#ef4444'
  const trackColor = score >= 80 ? 'rgba(16,185,129,0.1)' : score >= 60 ? 'rgba(249,115,22,0.1)' : 'rgba(239,68,68,0.1)'
  return (
    <div style={{ position: 'relative', width: 176, height: 176, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <svg width={176} height={176} style={{ transform: 'rotate(-90deg)', position: 'absolute' }}>
        <circle cx={88} cy={88} r={r} fill="none" stroke={trackColor} strokeWidth={stroke} />
        <circle cx={88} cy={88} r={r} fill="none" stroke={color} strokeWidth={stroke}
          strokeDasharray={circ} strokeDashoffset={offset} strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset 1.4s cubic-bezier(0.16,1,0.3,1)', filter: `drop-shadow(0 0 8px ${color}88)` }} />
      </svg>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <span style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontVariantNumeric: 'tabular-nums', fontWeight: 700, fontSize: 38, color, lineHeight: 1 }}>{score}</span>
        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>/ 100</span>
      </div>
    </div>
  )
}

function MetricRow({ label, value, max = 100 }) {
  const pct = Math.min(100, Math.max(0, value))
  const color = pct >= 75 ? '#10b981' : pct >= 50 ? '#f97316' : '#ef4444'
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', width: 88, flexShrink: 0 }}>{label}</span>
      <div style={{ flex: 1, height: 3, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 99, transition: 'width 1s cubic-bezier(0.16,1,0.3,1)' }} />
      </div>
      <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color, width: 28, textAlign: 'right' }}>{Math.round(pct)}</span>
    </div>
  )
}

function StatCard({ label, value, sub, color, icon }) {
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>{label.toUpperCase()}</span>
        {icon && <span style={{ fontSize: 14, opacity: 0.5 }}>{icon}</span>}
      </div>
      <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontVariantNumeric: 'tabular-nums', fontWeight: 700, fontSize: 26, color: color || 'var(--text-primary)', lineHeight: 1.1 }}>
        {value}
      </div>
      {sub && <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>{sub}</div>}
    </div>
  )
}

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-md)', borderRadius: 8, padding: '8px 12px', fontFamily: 'IBM Plex Mono', fontSize: 11 }}>
      <div style={{ color: 'var(--text-muted)', marginBottom: 4 }}>{label}</div>
      {payload.map((p, i) => (
        <div key={i} style={{ color: p.fill || p.color }}>{p.name}: <strong>{p.value}</strong></div>
      ))}
    </div>
  )
}

export default function OverviewTab({ jobId, report }) {
  const { data: hotspots = [] } = useQuery({ queryKey: ['hotspots', jobId], queryFn: () => import('../api/client').then(m => m.getHotspots(jobId)) })
  const { data: contributors = [] } = useQuery({ queryKey: ['contributors', jobId], queryFn: () => import('../api/client').then(m => m.getContributors(jobId)) })

  if (!report) return <div style={{ fontFamily: 'IBM Plex Mono', color: 'var(--text-muted)', padding: 40 }}>Loading report data...</div>

  const totalCommits = contributors.reduce((s, c) => s + c.commitCount, 0)
  const top6Hotspots = hotspots.slice(0, 6).map(h => ({ ...h, fileName: (h.fileName || h.filePath || '').replace('.java', '') }))
  const top6Contributors = contributors.slice(0, 6)
  const pieData = top6Contributors.map((c, i) => ({ name: c.authorName?.split(' ')[0] || '?', value: c.commitCount, color: COLORS[i % COLORS.length] }))

  const subScores = [
    { label: 'Complexity', value: Math.max(0, 100 - (report.avgComplexity - 5) * 5) },
    { label: 'Dead Code',  value: Math.max(0, 100 - report.deadCodeRatio * 2) },
    { label: 'Bus Factor', value: report.busFactor >= 4 ? 100 : report.busFactor === 3 ? 75 : report.busFactor === 2 ? 45 : 15 },
    { label: 'Hotspots',   value: Math.max(0, 100 - report.hotspotCount * 8) },
    { label: 'Deps',       value: report.dependencyCount > 50 ? 30 : report.dependencyCount > 25 ? 65 : 92 },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Top row */}
      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: 16 }}>

        {/* Score card */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: 24, gap: 0 }}>
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 16 }}>HEALTH SCORE</div>
          <ScoreGauge score={report.healthScore} />
          <div style={{ width: '100%', marginTop: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
            {subScores.map(s => <MetricRow key={s.label} label={s.label} value={s.value} />)}
          </div>
        </div>

        {/* Stats grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gridTemplateRows: 'repeat(3, 1fr)', gap: 12 }}>
          <StatCard label="Java Files" value={report.totalFiles} sub="source files" icon="◈" />
          <StatCard label="Methods" value={(report.totalMethods || 0).toLocaleString()} sub="all classes" icon="◎" />
          <StatCard label="Lines" value={(report.totalLines || 0).toLocaleString()} sub="of code" icon="▣" />
          <StatCard label="Avg CC" value={report.avgComplexity} sub={`peak: ${report.maxComplexity}`} icon="~"
            color={report.avgComplexity > 15 ? '#ef4444' : report.avgComplexity > 10 ? '#f97316' : '#10b981'} />
          <StatCard label="Dead Code" value={report.deadCodeCount} sub={`${report.deadCodeRatio}% of methods`} icon="◎"
            color={report.deadCodeCount > 10 ? '#ef4444' : '#f97316'} />
          <StatCard label="Bus Factor" value={report.busFactor} sub={report.busFactor <= 2 ? '⚠ High risk' : '✓ OK'} icon="◉"
            color={report.busFactor <= 2 ? '#ef4444' : report.busFactor <= 3 ? '#f97316' : '#10b981'} />
          <StatCard label="Commits" value={(report.totalCommits || 0).toLocaleString()} sub="full history" icon="↑" />
          <StatCard label="Contributors" value={report.contributorCount} sub="all-time" icon="◉" />
          <StatCard label="Dependencies" value={report.dependencyCount} sub="libraries" icon="⬡" />
        </div>
      </div>

      {/* Bottom row: charts */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>

        {/* Hotspots */}
        <div className="card">
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 16 }}>TOP COMMIT HOTSPOTS</div>
          {top6Hotspots.length === 0 ? (
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', textAlign: 'center', padding: '40px 0' }}>No hotspot data</div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={top6Hotspots} layout="vertical" margin={{ left: 0, right: 16 }}>
                <XAxis type="number" tick={{ fontSize: 9, fill: 'var(--text-muted)', fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
                <YAxis dataKey="fileName" type="category" tick={{ fontSize: 9, fill: 'var(--text-secondary)', fontFamily: 'IBM Plex Mono' }} width={150} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="commitCount" name="Commits" radius={[0, 4, 4, 0]} maxBarSize={14}>
                  {top6Hotspots.map((h, i) => (
                    <Cell key={i} fill={h.riskLevel === 'HIGH' ? '#ef4444' : h.riskLevel === 'MEDIUM' ? '#f97316' : '#3b82f6'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Contributors */}
        <div className="card">
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 12 }}>CONTRIBUTOR DISTRIBUTION</div>
          {report.busFactor <= 2 && (
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px', borderRadius: 99,
              background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
              fontFamily: 'IBM Plex Mono', fontSize: 10, color: '#ef4444', marginBottom: 12,
            }}>
              ⚠ Bus Factor {report.busFactor} — Critical Knowledge Risk
            </div>
          )}
          {pieData.length === 0 ? (
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', textAlign: 'center', padding: '40px 0' }}>No contributor data</div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <ResponsiveContainer width={160} height={160}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" cx="50%" cy="50%" innerRadius={36} outerRadius={66} paddingAngle={2}>
                    {pieData.map((d, i) => <Cell key={i} fill={d.color} />)}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 7 }}>
                {pieData.map((d, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ width: 6, height: 6, borderRadius: '50%', background: d.color, flexShrink: 0 }} />
                    <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>{d.name}</span>
                    <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>
                      {totalCommits > 0 ? Math.round(d.value / totalCommits * 100) : 0}%
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
