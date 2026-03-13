import { useQuery } from '@tanstack/react-query'
import { getComplexity } from '../api/client'
import { Treemap, ResponsiveContainer, Tooltip } from 'recharts'

const ccColor = (v) => v >= 20 ? '#ef4444' : v >= 12 ? '#f97316' : v >= 7 ? '#eab308' : '#22c55e'
const riskBadge = (r) => {
  const map = { HIGH: ['#7f1d1d','#fca5a5'], COMPLEX: ['#431407','#fed7aa'], MODERATE: ['#422006','#fde68a'], SIMPLE: ['#14532d','#86efac'] }
  return map[r] || map.SIMPLE
}

const LEGEND = [
  { c: '#22c55e', l: '1–6 Simple' },
  { c: '#eab308', l: '7–11 Moderate' },
  { c: '#f97316', l: '12–19 Complex' },
  { c: '#ef4444', l: '20+ High Risk' },
]

function CustomTreemapContent({ x, y, width, height, name, value }) {
  return (
    <g>
      <rect x={x + 1} y={y + 1} width={width - 2} height={height - 2}
        fill={ccColor(value || 1)} rx="4" opacity="0.85" />
      {width > 50 && height > 30 && (
        <>
          <text x={x + width / 2} y={y + height / 2 - 7} textAnchor="middle"
            fontSize={Math.min(10, width / 9)} fill="white" fontWeight="600">
            {(name || '').replace('.java', '')}
          </text>
          <text x={x + width / 2} y={y + height / 2 + 8} textAnchor="middle"
            fontSize={9} fill="rgba(255,255,255,0.6)">
            CC: {value}
          </text>
        </>
      )}
    </g>
  )
}

export default function ComplexityTab({ jobId }) {
  const { data: methods = [], isLoading } = useQuery({
    queryKey: ['complexity', jobId],
    queryFn: () => getComplexity(jobId),
  })

  if (isLoading) return <div className="py-20 text-center font-mono text-sm" style={{ color: 'var(--text-muted)' }}>Loading complexity data...</div>

  // Build file-level treemap data (max CC per file)
  const fileMap = {}
  methods.forEach(m => {
    const f = m.filePath || 'unknown'
    if (!fileMap[f] || m.ccScore > fileMap[f].value) {
      fileMap[f] = {
        name: f.split('/').pop(),
        value: m.ccScore,
        size: Math.max(fileMap[f]?.size || 0, 1) + 1,
      }
    }
  })
  const treemapData = Object.values(fileMap).sort((a, b) => b.value - a.value)

  const top20 = methods.slice(0, 20)

  return (
    <div className="space-y-4">
      {/* Treemap */}
      <div className="card">
        <div className="flex items-center justify-between mb-2">
          <div className="text-xs font-mono uppercase tracking-widest" style={{ color: 'var(--text-muted)' }}>
            File Complexity Heatmap
          </div>
          <div className="flex gap-3">
            {LEGEND.map(l => (
              <div key={l.l} className="flex items-center gap-1.5">
                <div className="w-2.5 h-2.5 rounded-sm" style={{ background: l.c }} />
                <span className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>{l.l}</span>
              </div>
            ))}
          </div>
        </div>
        <p className="text-xs mb-4" style={{ color: 'var(--text-muted)' }}>
          Tile area ∝ method count · Color = peak cyclomatic complexity
        </p>
        {treemapData.length > 0 ? (
          <ResponsiveContainer width="100%" height={280}>
            <Treemap data={treemapData} dataKey="size" nameKey="name" aspectRatio={4 / 3}
              content={<CustomTreemapContent />}>
              <Tooltip contentStyle={{ background: '#111827', border: '1px solid #374151', fontSize: 11, borderRadius: 6 }}
                formatter={(val, name, props) => [`CC: ${props.payload.value}`, props.payload.name]} />
            </Treemap>
          </ResponsiveContainer>
        ) : (
          <div className="text-center py-16 text-sm" style={{ color: 'var(--text-muted)' }}>No data yet</div>
        )}
      </div>

      {/* Methods Table */}
      <div className="card">
        <div className="text-xs font-mono uppercase tracking-widest mb-4" style={{ color: 'var(--text-muted)' }}>
          Top Complex Methods ({methods.length} total)
        </div>
        <div className="overflow-auto">
          <table className="w-full text-xs font-mono min-w-[700px]">
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)' }}>
                {['File', 'Class', 'Method', 'CC', 'Risk', 'Lines', 'Nesting'].map(h => (
                  <th key={h} className="text-left py-2 pr-4 font-medium" style={{ color: 'var(--text-muted)' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {top20.map(m => {
                const [bg, fg] = riskBadge(m.riskLevel)
                return (
                  <tr key={m.id} style={{ borderBottom: '1px solid var(--bg-elevated)' }}>
                    <td className="py-2.5 pr-4 truncate max-w-[160px]" style={{ color: 'var(--text-muted)' }}>
                      {(m.filePath || '').split('/').pop()}
                    </td>
                    <td className="py-2.5 pr-4" style={{ color: 'var(--text-secondary)' }}>{m.className}</td>
                    <td className="py-2.5 pr-4 max-w-[200px] truncate" style={{ color: 'var(--text-primary)' }}>{m.methodName}</td>
                    <td className="py-2.5 pr-4">
                      <span className="font-black text-sm" style={{ color: ccColor(m.ccScore) }}>{m.ccScore}</span>
                    </td>
                    <td className="py-2.5 pr-4">
                      <span className="px-2 py-0.5 rounded-full text-xs" style={{ background: bg, color: fg }}>
                        {m.riskLevel}
                      </span>
                    </td>
                    <td className="py-2.5 pr-4" style={{ color: 'var(--text-muted)' }}>{m.lineCount}</td>
                    <td className="py-2.5" style={{ color: 'var(--text-muted)' }}>{m.nestingDepth}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
