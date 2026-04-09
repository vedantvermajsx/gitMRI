import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getComplexity } from '../api/client'

const BANDS = [
  { min: 0,  max: 6,  label: 'SIMPLE',   color: '#10b981', bg: 'rgba(16,185,129,0.12)',  border: 'rgba(16,185,129,0.3)',  riskKey: 'SIMPLE'   },
  { min: 7,  max: 11, label: 'MODERATE', color: '#f59e0b', bg: 'rgba(245,158,11,0.12)',  border: 'rgba(245,158,11,0.3)',  riskKey: 'MODERATE' },
  { min: 12, max: 19, label: 'COMPLEX',  color: '#f97316', bg: 'rgba(249,115,22,0.12)',  border: 'rgba(249,115,22,0.3)',  riskKey: 'COMPLEX'  },
  { min: 20, max: Infinity, label: 'CRITICAL', color: '#ef4444', bg: 'rgba(239,68,68,0.12)', border: 'rgba(239,68,68,0.3)',  riskKey: 'HIGH' },
]
const getBand = cc => BANDS.find(b => cc >= b.min && cc <= b.max) || BANDS[0]

function HeatGrid({ files, hovered, onHover }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, padding: 4 }}>
      {files.map((f, i) => {
        const b = getBand(f.maxCC)
        const isHov = hovered === f.name
        return (
          <div key={i}
            onMouseEnter={() => onHover(f)}
            onMouseLeave={() => onHover(null)}
            style={{
              width: 136, height: 54, borderRadius: 8,
              padding: '8px 10px',
              background: isHov ? b.color + '28' : b.color + '10',
              border: `1px solid ${isHov ? b.color + 'aa' : b.color + '30'}`,
              display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
              cursor: 'default', position: 'relative', overflow: 'hidden',
              boxShadow: isHov ? `0 0 16px ${b.color}33` : 'none',
              transition: 'all 0.12s ease',
            }}>
            {/* CC badge */}
            <div style={{
              position: 'absolute', top: 6, right: 6,
              fontFamily: 'IBM Plex Mono', fontWeight: 700, fontSize: 10,
              background: b.color, color: '#000', borderRadius: 4,
              padding: '1px 5px', lineHeight: 1.4,
            }}>{f.maxCC}</div>
            {/* Name */}
            <div style={{
              fontFamily: 'IBM Plex Mono', fontSize: 10.5, fontWeight: 600,
              color: b.color, lineHeight: 1.3,
              maxWidth: '76%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>{f.name.replace('.java','')}</div>
            {/* Bar */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 8, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                {f.methodCount}m
              </span>
              <div style={{ flex: 1, height: 2, background: 'rgba(255,255,255,0.08)', borderRadius: 99, overflow: 'hidden' }}>
                <div style={{ width: `${Math.min(100,(f.maxCC/30)*100)}%`, height: '100%', background: b.color, opacity: 0.7 }} />
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

function HoverCard({ file }) {
  if (!file) return (
    <div style={{
      border: '1px dashed var(--border-md)', borderRadius: 10, padding: 20,
      fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'var(--text-muted)',
      textAlign: 'center', lineHeight: 1.8,
    }}>
      Hover a cell<br/>to inspect
    </div>
  )
  const b = getBand(file.maxCC)
  return (
    <div style={{
      background: b.color + '0a', border: `1px solid ${b.color}44`,
      borderRadius: 10, padding: 16,
    }}>
      <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, fontWeight: 700, color: b.color, marginBottom: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {file.name}
      </div>
      {[
        ['Peak CC', file.maxCC, b.color],
        ['Avg CC', file.avgCC.toFixed(1), 'var(--text-secondary)'],
        ['Methods', file.methodCount, 'var(--text-secondary)'],
        ['Risk', b.label, b.color],
      ].map(([k, v, c]) => (
        <div key={k} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontFamily: 'IBM Plex Mono', fontSize: 11 }}>
          <span style={{ color: 'var(--text-muted)' }}>{k}</span>
          <span style={{ color: c, fontWeight: 600 }}>{v}</span>
        </div>
      ))}
    </div>
  )
}

export default function ComplexityTab({ jobId }) {
  const { data: methods = [], isLoading } = useQuery({
    queryKey: ['complexity', jobId],
    queryFn: () => getComplexity(jobId),
  })
  const [hovFile, setHovFile] = useState(null)
  const [hovData, setHovData] = useState(null)
  const [sort, setSort] = useState('maxCC')
  const [risk, setRisk] = useState('ALL')
  const [page, setPage] = useState(0)
  const PAGE = 30

  if (isLoading) return (
    <div style={{ fontFamily: 'IBM Plex Mono', color: 'var(--text-muted)', padding: 60, textAlign: 'center', fontSize: 12 }}>
      Analyzing complexity metrics...
    </div>
  )

  // Build file map
  const fileMap = {}
  methods.forEach(m => {
    const k = m.filePath || 'unknown'
    if (!fileMap[k]) fileMap[k] = { name: k.split('/').pop(), fullPath: k, scores: [] }
    fileMap[k].scores.push(m.ccScore)
  })
  const files = Object.values(fileMap).map(f => ({
    name: f.name, fullPath: f.fullPath,
    maxCC: Math.max(...f.scores),
    avgCC: f.scores.reduce((a,b) => a+b, 0) / f.scores.length,
    methodCount: f.scores.length,
  })).sort((a,b) =>
    sort === 'maxCC' ? b.maxCC - a.maxCC :
    sort === 'avgCC' ? b.avgCC - a.avgCC :
    b.methodCount - a.methodCount
  )

  // Table
  const bandMap = { ALL: null, SIMPLE: 'SIMPLE', MODERATE: 'MODERATE', COMPLEX: 'COMPLEX', CRITICAL: 'HIGH' }
  const filteredMethods = risk === 'ALL' ? methods : methods.filter(m => m.riskLevel === bandMap[risk])
  const paged = filteredMethods.slice(page * PAGE, (page + 1) * PAGE)
  const totalPages = Math.ceil(filteredMethods.length / PAGE)

  const bandCounts = BANDS.map(b => ({ ...b, count: methods.filter(m => getBand(m.ccScore).label === b.label).length }))

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Summary stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        {bandCounts.map(b => (
          <div key={b.label} className="card" style={{ padding: '14px 18px', borderColor: b.color + '30' }}>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 6 }}>{b.label}</div>
            <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontVariantNumeric: 'tabular-nums', fontWeight: 700, fontSize: 28, color: b.color, lineHeight: 1 }}>{b.count}</div>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)', marginTop: 4 }}>methods</div>
          </div>
        ))}
      </div>

      {/* Heatmap card */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)' }}>
          <div>
            <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em' }}>
              FILE COMPLEXITY HEATMAP
            </span>
            <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', marginLeft: 12 }}>
              {files.length} files · color = peak CC
            </span>
          </div>
          <div style={{ display: 'flex', gap: 4 }}>
            {[['maxCC','Peak CC'],['avgCC','Avg CC'],['methodCount','Methods']].map(([k,l]) => (
              <button key={k} onClick={() => setSort(k)} style={{
                fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '4px 12px', borderRadius: 6,
                cursor: 'pointer', border: '1px solid',
                background: sort === k ? 'var(--accent)' : 'var(--bg-elevated)',
                color: sort === k ? 'white' : 'var(--text-muted)',
                borderColor: sort === k ? 'var(--accent)' : 'var(--border-md)',
              }}>{l}</button>
            ))}
          </div>
        </div>

        {/* Distribution bar */}
        <div style={{ padding: '12px 20px', borderBottom: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div style={{ height: 6, borderRadius: 99, overflow: 'hidden', display: 'flex' }}>
            {bandCounts.map(b => b.count > 0 && (
              <div key={b.label} title={`${b.label}: ${b.count}`}
                style={{ width: `${(b.count / methods.length) * 100}%`, background: b.color }} />
            ))}
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            {bandCounts.map(b => (
              <div key={b.label} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <div style={{ width: 7, height: 7, borderRadius: 2, background: b.color }} />
                <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>
                  {b.label} <strong style={{ color: b.color }}>{b.count}</strong>
                </span>
              </div>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 0 }}>
          <div style={{ flex: 1, maxHeight: 340, overflowY: 'auto', padding: 16 }}>
            <HeatGrid files={files} hovered={hovFile} onHover={f => { setHovFile(f?.name ?? null); setHovData(f ?? null) }} />
          </div>
          <div style={{ width: 200, borderLeft: '1px solid var(--border)', padding: 16, flexShrink: 0 }}>
            <HoverCard file={hovData} />
          </div>
        </div>
      </div>

      {/* Method table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em' }}>
            METHOD DETAIL — {filteredMethods.length}
          </span>
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {[['ALL','All'], ...BANDS.map(b => [b.riskKey === 'HIGH' ? 'CRITICAL' : b.label, b.label])].map(([key, lbl]) => {
              const b = BANDS.find(b => (b.riskKey === 'HIGH' ? 'CRITICAL' : b.label) === key)
              const count = key === 'ALL' ? methods.length : (b ? bandCounts.find(bc => bc.label === b.label)?.count : 0)
              return (
                <button key={key} onClick={() => { setRisk(key); setPage(0) }} style={{
                  fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '4px 12px', borderRadius: 6, cursor: 'pointer', border: '1px solid',
                  background: risk === key ? (b?.color || 'var(--accent)') : 'var(--bg-elevated)',
                  color: risk === key ? '#000' : 'var(--text-muted)',
                  borderColor: risk === key ? (b?.color || 'var(--accent)') : 'var(--border-md)',
                  fontWeight: risk === key ? 700 : 400,
                }}>
                  {lbl} ({count})
                </button>
              )
            })}
          </div>
        </div>

        <table className="data-table">
          <thead>
            <tr>
              {['File', 'Class', 'Method', 'CC', 'Risk', 'Lines', 'Depth'].map(h => <th key={h}>{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {paged.map(m => {
              const b = getBand(m.ccScore)
              return (
                <tr key={m.id}>
                  <td style={{ color: 'var(--text-muted)', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={m.filePath}>
                    {(m.filePath || '').split('/').pop()}
                  </td>
                  <td style={{ color: 'var(--text-secondary)', maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {m.className}
                  </td>
                  <td style={{ color: 'var(--text-primary)', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={m.methodName}>
                    {m.methodName}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
                      <div style={{ width: 32, height: 4, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
                        <div style={{ width: `${Math.min(100,(m.ccScore/30)*100)}%`, height: '100%', background: b.color }} />
                      </div>
                      <span style={{ color: b.color, fontWeight: 700, fontSize: 12 }}>{m.ccScore}</span>
                    </div>
                  </td>
                  <td>
                    <span style={{ padding: '2px 8px', borderRadius: 99, fontSize: 9, fontFamily: 'IBM Plex Mono', fontWeight: 600, background: b.bg, color: b.color, border: `1px solid ${b.border}` }}>
                      {b.label}
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-muted)' }}>{m.lineCount}</td>
                  <td style={{ color: 'var(--text-muted)' }}>
                    <span style={{ letterSpacing: -2, color: b.color + '88' }}>{'▪'.repeat(Math.min(m.nestingDepth, 5))}</span>
                    <span style={{ marginLeft: 4 }}>{m.nestingDepth}</span>
                  </td>
                </tr>
              )
            })}
            {paged.length === 0 && (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No methods match</td></tr>
            )}
          </tbody>
        </table>

        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', borderTop: '1px solid var(--border)' }}>
            <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>
              Page {page+1} / {totalPages}
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button onClick={() => setPage(p => Math.max(0, p-1))} disabled={page===0} className="btn btn-ghost" style={{ fontSize: 10, padding: '4px 12px', opacity: page===0?0.4:1 }}>← Prev</button>
              <button onClick={() => setPage(p => Math.min(totalPages-1, p+1))} disabled={page>=totalPages-1} className="btn btn-ghost" style={{ fontSize: 10, padding: '4px 12px', opacity: page>=totalPages-1?0.4:1 }}>Next →</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
