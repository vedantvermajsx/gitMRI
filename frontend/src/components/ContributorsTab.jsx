import { useQuery } from '@tanstack/react-query'
import { getContributors } from '../api/client'
import { BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ResponsiveContainer } from 'recharts'

const PALETTE = ['#3b82f6','#f97316','#8b5cf6','#10b981','#f59e0b','#ec4899','#06b6d4','#84cc16']

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-md)', borderRadius: 8, padding: '8px 12px', fontFamily: 'IBM Plex Mono', fontSize: 11 }}>
      <div style={{ color: 'var(--text-muted)', marginBottom: 4 }}>{label}</div>
      <div style={{ color: payload[0].fill }}>Commits: <strong>{payload[0].value}</strong></div>
    </div>
  )
}

export default function ContributorsTab({ jobId, report }) {
  const { data: contributors = [], isLoading } = useQuery({
    queryKey: ['contributors', jobId],
    queryFn: () => getContributors(jobId),
  })

  if (isLoading) return (
    <div style={{ fontFamily: 'IBM Plex Mono', color: 'var(--text-muted)', padding: 60, textAlign: 'center', fontSize: 12 }}>
      Loading contributor data...
    </div>
  )

  const total = contributors.reduce((s, c) => s + c.commitCount, 0)
  const busFactor = report?.busFactor || '?'
  const top8 = contributors.slice(0, 8).map(c => ({ ...c, firstName: c.authorName?.split(' ')[0] || '?' }))

  const bfColor = busFactor <= 2 ? '#ef4444' : busFactor <= 3 ? '#f97316' : '#10b981'
  const bfBg    = busFactor <= 2 ? 'rgba(239,68,68,0.08)' : busFactor <= 3 ? 'rgba(249,115,22,0.08)' : 'rgba(16,185,129,0.08)'
  const bfBorder = busFactor <= 2 ? 'rgba(239,68,68,0.3)' : busFactor <= 3 ? 'rgba(249,115,22,0.3)' : 'rgba(16,185,129,0.3)'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Bus factor banner */}
      <div className="card" style={{ background: bfBg, borderColor: bfBorder, padding: '20px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
          <div style={{ flexShrink: 0 }}>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 4 }}>BUS FACTOR</div>
            <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontVariantNumeric: 'tabular-nums', fontWeight: 700, fontSize: 48, color: bfColor, lineHeight: 1 }}>{busFactor}</div>
          </div>
          <div style={{ width: 1, height: 60, background: 'var(--border)', flexShrink: 0 }} />
          <div>
            <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontWeight: 700, fontSize: 16, color: bfColor, marginBottom: 6 }}>
              {busFactor <= 1 ? 'Critical Knowledge Concentration' :
               busFactor <= 2 ? 'High Risk — Immediate Action Needed' :
               busFactor <= 3 ? 'Moderate — Consider Documentation' : 'Healthy Distribution'}
            </div>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.7, maxWidth: 480 }}>
              {busFactor <= 1 ? 'One person holds over 50% of the commit history. If they leave, critical knowledge is lost.' :
               busFactor <= 2 ? 'Only 2 contributors own the majority of the codebase. High risk if either becomes unavailable.' :
               busFactor <= 3 ? '3 contributors account for most work. Manageable — focus on documentation and knowledge sharing.' :
               'Knowledge is well-distributed. Multiple contributors can maintain the project independently.'}
            </div>
          </div>
        </div>
      </div>

      {/* Chart + contributor grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div className="card">
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 16 }}>COMMITS PER AUTHOR</div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={top8} margin={{ bottom: 0 }} barCategoryGap="30%">
              <XAxis dataKey="firstName" tick={{ fontSize: 9, fill: 'var(--text-muted)', fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 9, fill: 'var(--text-muted)', fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="commitCount" radius={[4, 4, 0, 0]}>
                {top8.map((c, i) => <Cell key={i} fill={PALETTE[i % PALETTE.length]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Top contributors list */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em', padding: '16px 20px 12px', borderBottom: '1px solid var(--border)' }}>
            ALL CONTRIBUTORS
          </div>
          <div style={{ overflowY: 'auto', maxHeight: 280 }}>
            {contributors.map((c, i) => {
              const pct = total > 0 ? Math.round(c.commitCount / total * 100) : 0
              const color = PALETTE[i % PALETTE.length]
              return (
                <div key={c.id} style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '11px 20px', borderBottom: '1px solid var(--border)',
                  transition: 'background 0.1s',
                }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-elevated)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  {/* Avatar */}
                  <div style={{
                    width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
                    background: color + '22', border: `1.5px solid ${color}55`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontFamily: 'system-ui, -apple-system, sans-serif', fontWeight: 800, fontSize: 13, color,
                  }}>
                    {(c.authorName || '?')[0].toUpperCase()}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {c.authorName}
                    </div>
                    <div style={{ height: 3, background: 'var(--border)', borderRadius: 99, marginTop: 5, overflow: 'hidden' }}>
                      <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 99 }} />
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, fontWeight: 700, color }}>{c.commitCount}</div>
                    <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)' }}>{pct}%</div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* Contributor cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {contributors.slice(0, 8).map((c, i) => {
          const color = PALETTE[i % PALETTE.length]
          const pct = total > 0 ? Math.round(c.commitCount / total * 100) : 0
          return (
            <div key={c.id} className="card" style={{ borderColor: color + '30', padding: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                <div style={{
                  width: 36, height: 36, borderRadius: '50%', flexShrink: 0,
                  background: `linear-gradient(135deg, ${color}33, ${color}11)`,
                  border: `1.5px solid ${color}55`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontFamily: 'system-ui, -apple-system, sans-serif', fontWeight: 800, fontSize: 15, color,
                }}>
                  {(c.authorName || '?')[0].toUpperCase()}
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {c.authorName}
                  </div>
                  <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {c.authorEmail}
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5, fontFamily: 'IBM Plex Mono', fontSize: 10 }}>
                {[
                  ['Commits', c.commitCount, color],
                  ['Files', c.filesOwned, 'var(--text-secondary)'],
                  ['+Lines', `+${(c.linesAdded || 0).toLocaleString()}`, '#10b981'],
                  ['Share', `${pct}%`, 'var(--text-muted)'],
                ].map(([k, v, vc]) => (
                  <div key={k} style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)' }}>{k}</span>
                    <span style={{ color: vc, fontWeight: 600 }}>{v}</span>
                  </div>
                ))}
              </div>
              <div style={{ marginTop: 10, height: 2, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
                <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 99 }} />
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
