import { useQuery } from '@tanstack/react-query'
import { getHotspots, getContributors } from '../api/client'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer,
  PieChart, Pie, Legend
} from 'recharts'

const CONTRIBUTOR_COLORS = ['#f97316','#3b82f6','#8b5cf6','#10b981','#eab308','#ec4899','#14b8a6']

function ScoreGauge({ score }) {
  const r = 72, stroke = 14
  const circ = 2 * Math.PI * r
  const offset = circ * (1 - score / 100)
  const color = score >= 80 ? '#22c55e' : score >= 60 ? '#f97316' : '#ef4444'
  return (
    <div className="relative flex items-center justify-center" style={{ width: 184, height: 184 }}>
      <svg width="184" height="184" style={{ transform: 'rotate(-90deg)' }}>
        <circle cx="92" cy="92" r={r} fill="none" stroke="#1f2937" strokeWidth={stroke} />
        <circle cx="92" cy="92" r={r} fill="none" stroke={color} strokeWidth={stroke}
          strokeDasharray={circ} strokeDashoffset={offset} strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset 1.2s ease' }} />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="font-mono font-black text-4xl" style={{ color }}>{score}</span>
        <span className="text-xs font-mono mt-0.5" style={{ color: 'var(--text-muted)' }}>/ 100</span>
      </div>
    </div>
  )
}

function StatCard({ label, value, sub, color }) {
  return (
    <div className="card">
      <div className="font-mono font-black text-3xl mb-1" style={{ color: color || 'var(--text-primary)' }}>
        {value}
      </div>
      <div className="text-sm font-medium">{label}</div>
      {sub && <div className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>{sub}</div>}
    </div>
  )
}

const SubScores = ({ report }) => {
  const items = [
    { label: 'Complexity', value: Math.max(0, 100 - (report.avgComplexity - 5) * 5) },
    { label: 'Dead Code', value: Math.max(0, 100 - report.deadCodeRatio * 2) },
    { label: 'Bus Factor', value: report.busFactor >= 3 ? 100 : report.busFactor === 2 ? 60 : 20 },
    { label: 'Hotspots', value: Math.max(0, 100 - report.hotspotCount * 8) },
    { label: 'Dependencies', value: report.dependencyCount > 50 ? 30 : report.dependencyCount > 25 ? 65 : 90 },
  ]
  return (
    <div className="space-y-2.5 mt-4 w-full">
      {items.map(({ label, value }) => {
        const clr = value >= 80 ? '#22c55e' : value >= 60 ? '#f97316' : '#ef4444'
        return (
          <div key={label} className="flex items-center gap-3">
            <span className="text-xs font-mono w-24" style={{ color: 'var(--text-muted)' }}>{label}</span>
            <div className="flex-1 h-1.5 rounded-full" style={{ background: 'var(--border)' }}>
              <div className="h-1.5 rounded-full transition-all duration-700"
                style={{ width: `${Math.min(100, Math.round(value))}%`, background: clr }} />
            </div>
            <span className="text-xs font-mono w-8 text-right" style={{ color: clr }}>
              {Math.round(value)}
            </span>
          </div>
        )
      })}
    </div>
  )
}

export default function OverviewTab({ jobId, report }) {
  const { data: hotspots = [] } = useQuery({ queryKey: ['hotspots', jobId], queryFn: () => import('../api/client').then(m => m.getHotspots(jobId)) })
  const { data: contributors = [] } = useQuery({ queryKey: ['contributors', jobId], queryFn: () => import('../api/client').then(m => m.getContributors(jobId)) })

  if (!report) return <div style={{ color: 'var(--text-muted)' }}>Loading report...</div>

  const top6Hotspots = hotspots.slice(0, 6)
  const top6Contributors = contributors.slice(0, 6)
  const totalCommits = contributors.reduce((s, c) => s + c.commitCount, 0)

  const pieData = top6Contributors.map((c, i) => ({
    name: c.authorName,
    value: c.commitCount,
    color: CONTRIBUTOR_COLORS[i % CONTRIBUTOR_COLORS.length]
  }))

  return (
    <div className="grid grid-cols-12 gap-4">
      {/* Health Score Card */}
      <div className="col-span-3 card flex flex-col items-center">
        <div className="text-xs font-mono uppercase tracking-widest mb-4" style={{ color: 'var(--text-muted)' }}>
          Health Score
        </div>
        <ScoreGauge score={Math.round(report.healthScore)} />
        <SubScores report={report} />
      </div>

      {/* Stats Grid */}
      <div className="col-span-3 grid grid-rows-3 gap-3">
        <StatCard label="Java Files" value={report.totalFiles} sub="source files analyzed" />
        <StatCard label="Methods" value={report.totalMethods.toLocaleString()} sub="across all classes" />
        <StatCard label="Total Lines" value={report.totalLines.toLocaleString()} sub="lines of code" />
      </div>

      <div className="col-span-3 grid grid-rows-3 gap-3">
        <StatCard label="Avg Complexity" value={report.avgComplexity}
          color={report.avgComplexity > 15 ? '#ef4444' : report.avgComplexity > 10 ? '#f97316' : '#22c55e'}
          sub={`max: ${report.maxComplexity}`} />
        <StatCard label="Dead Code" value={`${report.deadCodeCount}`}
          color={report.deadCodeCount > 10 ? '#ef4444' : '#f97316'}
          sub={`${report.deadCodeRatio}% of methods`} />
        <StatCard label="Bus Factor" value={report.busFactor}
          color={report.busFactor <= 2 ? '#ef4444' : report.busFactor <= 3 ? '#f97316' : '#22c55e'}
          sub={report.busFactor <= 2 ? '⚠ Critical risk' : '✓ Acceptable'} />
      </div>

      <div className="col-span-3 grid grid-rows-3 gap-3">
        <StatCard label="Commits" value={report.totalCommits.toLocaleString()} sub="in full history" />
        <StatCard label="Contributors" value={report.contributorCount} sub="all-time authors" />
        <StatCard label="Dependencies" value={report.dependencyCount} sub="external libraries" />
      </div>

      {/* Hotspots Bar Chart */}
      <div className="col-span-6 card">
        <div className="text-xs font-mono uppercase tracking-widest mb-4" style={{ color: 'var(--text-muted)' }}>
          Top Commit Hotspots
        </div>
        {top6Hotspots.length === 0 ? (
          <div className="text-sm text-center py-10" style={{ color: 'var(--text-muted)' }}>No hotspot data</div>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={top6Hotspots} layout="vertical" margin={{ left: 0, right: 20 }}>
              <XAxis type="number" tick={{ fontSize: 10, fill: '#6b7280' }} />
              <YAxis dataKey="fileName" type="category" tick={{ fontSize: 9, fill: '#9ca3af' }} width={160} />
              <Tooltip contentStyle={{ background: '#111827', border: '1px solid #374151', fontSize: 11, borderRadius: 6 }} />
              <Bar dataKey="commitCount" radius={[0, 4, 4, 0]}>
                {top6Hotspots.map((h, i) => (
                  <Cell key={i} fill={h.riskLevel === 'HIGH' ? '#ef4444' : h.riskLevel === 'MEDIUM' ? '#f97316' : '#3b82f6'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Contributor Pie */}
      <div className="col-span-6 card">
        <div className="text-xs font-mono uppercase tracking-widest mb-1" style={{ color: 'var(--text-muted)' }}>
          Contributor Distribution
        </div>
        {report.busFactor <= 2 && (
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono mb-3"
            style={{ background: '#7f1d1d', color: '#fca5a5' }}>
            ⚠ Bus Factor {report.busFactor} — Critical Knowledge Risk
          </div>
        )}
        {pieData.length === 0 ? (
          <div className="text-sm text-center py-10" style={{ color: 'var(--text-muted)' }}>No contributor data</div>
        ) : (
          <div className="flex items-center gap-4">
            <ResponsiveContainer width={180} height={180}>
              <PieChart>
                <Pie data={pieData} dataKey="value" cx="50%" cy="50%" innerRadius={40} outerRadius={72} paddingAngle={2}>
                  {pieData.map((d, i) => <Cell key={i} fill={d.color} />)}
                </Pie>
                <Tooltip contentStyle={{ background: '#111827', border: '1px solid #374151', fontSize: 11, borderRadius: 6 }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="flex-1 space-y-1.5">
              {pieData.map((d, i) => (
                <div key={i} className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: d.color }} />
                  <span className="text-xs flex-1 truncate" style={{ color: 'var(--text-secondary)' }}>{d.name}</span>
                  <span className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>
                    {totalCommits > 0 ? Math.round(d.value / totalCommits * 100) : 0}%
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
