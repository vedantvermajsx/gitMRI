import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getDeadCode } from '../api/client'

const RISK = {
  HIGH:   { color: '#ef4444', bg: 'rgba(239,68,68,0.1)',   border: 'rgba(239,68,68,0.25)'   },
  MEDIUM: { color: '#f97316', bg: 'rgba(249,115,22,0.1)',  border: 'rgba(249,115,22,0.25)'  },
  LOW:    { color: '#f59e0b', bg: 'rgba(245,158,11,0.1)',  border: 'rgba(245,158,11,0.25)'  },
}
const TYPE = {
  CLASS:  { color: '#8b5cf6', bg: 'rgba(139,92,246,0.12)'  },
  METHOD: { color: '#3b82f6', bg: 'rgba(59,130,246,0.12)'  },
  FIELD:  { color: '#10b981', bg: 'rgba(16,185,129,0.12)'  },
}

export default function DeadCodeTab({ jobId }) {
  const [riskFilter, setRiskFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const PAGE = 30

  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deadcode', jobId],
    queryFn: () => getDeadCode(jobId),
  })

  if (isLoading) return (
    <div style={{ fontFamily: 'IBM Plex Mono', color: 'var(--text-muted)', padding: 60, textAlign: 'center', fontSize: 12 }}>
      Scanning for dead code...
    </div>
  )

  const filtered = items.filter(d => {
    if (riskFilter !== 'ALL' && d.riskLevel !== riskFilter) return false
    if (typeFilter !== 'ALL' && d.itemType !== typeFilter) return false
    if (search) {
      const q = search.toLowerCase()
      if (!d.name?.toLowerCase().includes(q) && !(d.filePath || '').toLowerCase().includes(q)) return false
    }
    return true
  })

  const counts = { HIGH: 0, MEDIUM: 0, LOW: 0 }
  items.forEach(d => { if (counts[d.riskLevel] !== undefined) counts[d.riskLevel]++ })
  const paged = filtered.slice(page * PAGE, (page + 1) * PAGE)
  const totalPages = Math.ceil(filtered.length / PAGE)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Summary row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {[
          { label: 'TOTAL FINDINGS', value: items.length, color: 'var(--text-primary)', sub: 'detected' },
          { label: 'HIGH RISK', value: counts.HIGH, color: RISK.HIGH.color, sub: 'immediate attention' },
          { label: 'MEDIUM RISK', value: counts.MEDIUM, color: RISK.MEDIUM.color, sub: 'should address' },
          { label: 'LOW RISK', value: counts.LOW, color: RISK.LOW.color, sub: 'can defer' },
        ].map(s => (
          <div key={s.label} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.1em', marginBottom: 8 }}>{s.label}</div>
            <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', fontVariantNumeric: 'tabular-nums', fontWeight: 700, fontSize: 32, color: s.color, lineHeight: 1 }}>{s.value}</div>
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>{s.sub}</div>
          </div>
        ))}
      </div>

      {/* Filter + table card */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>

        {/* Filters toolbar */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10, padding: '14px 20px',
          borderBottom: '1px solid var(--border)', flexWrap: 'wrap',
        }}>
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search name or file..."
            className="input"
            style={{ maxWidth: 240, padding: '7px 12px', fontSize: 11 }}
          />

          <div style={{ display: 'flex', gap: 4 }}>
            {['ALL', 'HIGH', 'MEDIUM', 'LOW'].map(r => (
              <button key={r} onClick={() => { setRiskFilter(r); setPage(0) }} style={{
                fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '5px 12px', borderRadius: 6,
                cursor: 'pointer', border: '1px solid',
                background: riskFilter === r ? (r === 'ALL' ? 'var(--accent)' : RISK[r]?.bg || 'var(--accent)') : 'var(--bg-elevated)',
                color: riskFilter === r ? (r === 'ALL' ? 'white' : RISK[r]?.color || 'white') : 'var(--text-muted)',
                borderColor: riskFilter === r ? (r === 'ALL' ? 'var(--accent)' : RISK[r]?.border || 'var(--accent)') : 'var(--border-md)',
                fontWeight: riskFilter === r ? 600 : 400,
              }}>{r}</button>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 4 }}>
            {['ALL', 'CLASS', 'METHOD'].map(t => (
              <button key={t} onClick={() => { setTypeFilter(t); setPage(0) }} style={{
                fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '5px 12px', borderRadius: 6,
                cursor: 'pointer', border: '1px solid',
                background: typeFilter === t ? (TYPE[t]?.bg || 'var(--accent)') : 'transparent',
                color: typeFilter === t ? (TYPE[t]?.color || 'white') : 'var(--text-muted)',
                borderColor: typeFilter === t ? (TYPE[t]?.color + '55' || 'var(--accent)') : 'transparent',
              }}>{t}</button>
            ))}
          </div>

          <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', marginLeft: 'auto' }}>
            {filtered.length} results
          </span>
        </div>

        {/* Table */}
        {filtered.length === 0 ? (
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', textAlign: 'center', padding: '60px 0' }}>
            {items.length === 0 ? '✓ No dead code detected' : 'No results match your filters'}
          </div>
        ) : (
          <>
            <table className="data-table">
              <thead>
                <tr>
                  {['Type', 'Name', 'File', 'Line', 'Risk', 'Reason'].map(h => (
                    <th key={h}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paged.map(d => {
                  const risk = RISK[d.riskLevel] || RISK.LOW
                  const type = TYPE[d.itemType] || TYPE.METHOD
                  return (
                    <tr key={d.id}>
                      <td>
                        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '3px 8px', borderRadius: 5, background: type.bg, color: type.color, fontWeight: 600 }}>
                          {d.itemType}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-primary)', maxWidth: 200 }}>
                        <span title={d.qualifiedName}>{d.name}</span>
                      </td>
                      <td style={{ color: 'var(--text-muted)', maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {(d.filePath || '').split('/').pop()}
                      </td>
                      <td style={{ color: 'var(--text-muted)' }}>{d.lineNumber}</td>
                      <td>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '3px 8px', borderRadius: 99, background: risk.bg, color: risk.color, border: `1px solid ${risk.border}` }}>
                          <span style={{ width: 4, height: 4, borderRadius: '50%', background: risk.color }} />
                          {d.riskLevel}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', maxWidth: 280, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={d.reason}>
                        {d.reason}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', borderTop: '1px solid var(--border)' }}>
                <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>
                  Page {page + 1} / {totalPages}
                </span>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="btn btn-ghost" style={{ fontSize: 10, padding: '4px 12px', opacity: page === 0 ? 0.4 : 1 }}>← Prev</button>
                  <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="btn btn-ghost" style={{ fontSize: 10, padding: '4px 12px', opacity: page >= totalPages - 1 ? 0.4 : 1 }}>Next →</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
