import { useQuery } from '@tanstack/react-query'
import { getContributors } from '../api/client'
import { BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer } from 'recharts'

const COLORS = ['#f97316','#3b82f6','#8b5cf6','#10b981','#eab308','#ec4899','#14b8a6','#f43f5e']

export default function ContributorsTab({ jobId, report }) {
  const { data: contributors = [], isLoading } = useQuery({
    queryKey: ['contributors', jobId],
    queryFn: () => getContributors(jobId),
  })

  if (isLoading) return <div className="py-20 text-center font-mono text-sm" style={{ color: 'var(--text-muted)' }}>Loading contributor data...</div>

  const top8 = contributors.slice(0, 8)
  const total = contributors.reduce((s, c) => s + c.commitCount, 0)
  const busFactor = report?.busFactor || '?'

  return (
    <div className="space-y-4">
      {/* Bus Factor Alert */}
      <div className="card" style={{
        borderColor: busFactor <= 2 ? '#7f1d1d' : busFactor <= 3 ? '#431407' : 'var(--border)',
      }}>
        <div className="flex items-center gap-4">
          <div className="text-4xl">{busFactor <= 2 ? '⚠️' : busFactor <= 3 ? '🟡' : '✅'}</div>
          <div>
            <div className="font-mono font-black text-2xl mb-0.5"
              style={{ color: busFactor <= 2 ? '#ef4444' : busFactor <= 3 ? '#f97316' : '#22c55e' }}>
              Bus Factor: {busFactor}
            </div>
            <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              {busFactor <= 1
                ? 'Critical: One person holds over 50% of knowledge. Immediate action needed.'
                : busFactor <= 2
                ? 'High risk: Knowledge concentrated in 2 contributors. Consider documentation and pair programming.'
                : busFactor <= 3
                ? 'Moderate: 3 people own the majority of the codebase. Manageable with proper documentation.'
                : 'Healthy: Knowledge is well distributed across contributors.'}
            </div>
          </div>
        </div>
      </div>

      {/* Commit Distribution Bar Chart */}
      <div className="card">
        <div className="text-xs font-mono uppercase tracking-widest mb-4" style={{ color: 'var(--text-muted)' }}>
          Commits Per Contributor
        </div>
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={top8} margin={{ top: 0, bottom: 20 }}>
            <XAxis dataKey="authorName" tick={{ fontSize: 10, fill: '#9ca3af' }}
              tickFormatter={n => n.split(' ')[0]} />
            <YAxis tick={{ fontSize: 10, fill: '#6b7280' }} />
            <Tooltip
              contentStyle={{ background: '#111827', border: '1px solid #374151', fontSize: 11, borderRadius: 6 }}
              formatter={(value, name) => [value, 'Commits']} />
            <Bar dataKey="commitCount" radius={[4, 4, 0, 0]} maxBarSize={60}>
              {top8.map((c, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Contributor Cards Grid */}
      <div className="grid grid-cols-4 gap-3">
        {contributors.map((c, i) => {
          const color = COLORS[i % COLORS.length]
          const pct = total > 0 ? Math.round(c.commitCount / total * 100) : 0
          return (
            <div key={c.id} className="card" style={{ borderColor: color + '40' }}>
              <div className="flex items-start gap-3 mb-3">
                <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0"
                  style={{ background: color + '22', color }}>
                  {(c.authorName || '?')[0].toUpperCase()}
                </div>
                <div className="min-w-0">
                  <div className="font-bold text-sm truncate" title={c.authorName}>{c.authorName}</div>
                  <div className="text-xs truncate" style={{ color: 'var(--text-muted)' }}>{c.authorEmail}</div>
                </div>
              </div>
              <div className="space-y-1.5 font-mono text-xs">
                <div className="flex justify-between">
                  <span style={{ color: 'var(--text-muted)' }}>Commits</span>
                  <span style={{ color }}>{c.commitCount}</span>
                </div>
                <div className="flex justify-between">
                  <span style={{ color: 'var(--text-muted)' }}>Files</span>
                  <span>{c.filesOwned}</span>
                </div>
                <div className="flex justify-between">
                  <span style={{ color: 'var(--text-muted)' }}>Share</span>
                  <span>{pct}%</span>
                </div>
                <div className="flex justify-between">
                  <span style={{ color: 'var(--text-muted)' }}>+Lines</span>
                  <span style={{ color: '#22c55e' }}>+{c.linesAdded?.toLocaleString() || 0}</span>
                </div>
              </div>
              <div className="mt-3 h-1 rounded-full" style={{ background: 'var(--border)' }}>
                <div className="h-1 rounded-full transition-all duration-700"
                  style={{ width: `${pct}%`, background: color }} />
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
