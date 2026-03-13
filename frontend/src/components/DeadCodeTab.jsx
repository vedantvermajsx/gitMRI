import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getDeadCode } from '../api/client'

const RISK_CONFIG = {
  HIGH:   { bg: '#7f1d1d', text: '#fca5a5', dot: '#ef4444' },
  MEDIUM: { bg: '#431407', text: '#fed7aa', dot: '#f97316' },
  LOW:    { bg: '#422006', text: '#fde68a', dot: '#eab308' },
}

const TYPE_CONFIG = {
  CLASS:  { bg: '#1e1b4b', text: '#a5b4fc' },
  METHOD: { bg: '#0c2338', text: '#7dd3fc' },
  FIELD:  { bg: '#0a1f1a', text: '#6ee7b7' },
}

export default function DeadCodeTab({ jobId }) {
  const [filter, setFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const { data: allItems = [], isLoading } = useQuery({
    queryKey: ['deadcode', jobId],
    queryFn: () => getDeadCode(jobId),
  })

  if (isLoading) return <div className="py-20 text-center font-mono text-sm" style={{ color: 'var(--text-muted)' }}>Scanning for dead code...</div>

  const filtered = allItems.filter(d => {
    if (filter !== 'ALL' && d.riskLevel !== filter) return false
    if (typeFilter !== 'ALL' && d.itemType !== typeFilter) return false
    if (search && !d.name.toLowerCase().includes(search.toLowerCase()) &&
        !(d.filePath || '').toLowerCase().includes(search.toLowerCase())) return false
    return true
  })

  const counts = { HIGH: 0, MEDIUM: 0, LOW: 0 }
  allItems.forEach(d => { if (counts[d.riskLevel] !== undefined) counts[d.riskLevel]++ })

  return (
    <div className="space-y-4">
      {/* Summary cards */}
      <div className="grid grid-cols-4 gap-3">
        <div className="card">
          <div className="font-mono font-black text-3xl mb-1">{allItems.length}</div>
          <div className="text-sm">Total Findings</div>
        </div>
        {['HIGH', 'MEDIUM', 'LOW'].map(r => (
          <div key={r} className="card cursor-pointer transition-colors hover:border-indigo-600"
            onClick={() => setFilter(filter === r ? 'ALL' : r)}
            style={{ borderColor: filter === r ? 'var(--accent)' : 'var(--border)' }}>
            <div className="font-mono font-black text-3xl mb-1"
              style={{ color: RISK_CONFIG[r].dot }}>{counts[r]}</div>
            <div className="text-sm">{r} Risk</div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="card">
        <div className="flex flex-wrap items-center gap-3 mb-4">
          {/* Search */}
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search by name or file..."
            className="px-3 py-1.5 rounded-lg text-sm font-mono outline-none flex-1 min-w-[200px]"
            style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)', color: 'var(--text-primary)' }} />

          {/* Risk filter */}
          <div className="flex gap-1">
            {['ALL', 'HIGH', 'MEDIUM', 'LOW'].map(r => (
              <button key={r} onClick={() => setFilter(r)}
                className="px-3 py-1 rounded text-xs font-mono uppercase tracking-wider"
                style={{
                  background: filter === r ? 'var(--accent)' : 'var(--bg-elevated)',
                  color: filter === r ? 'white' : 'var(--text-muted)',
                  border: '1px solid var(--border)',
                }}>
                {r}
              </button>
            ))}
          </div>

          {/* Type filter */}
          <div className="flex gap-1">
            {['ALL', 'CLASS', 'METHOD', 'FIELD'].map(t => (
              <button key={t} onClick={() => setTypeFilter(t)}
                className="px-3 py-1 rounded text-xs font-mono uppercase tracking-wider"
                style={{
                  background: typeFilter === t ? 'var(--bg-elevated)' : 'transparent',
                  color: typeFilter === t ? 'var(--text-primary)' : 'var(--text-muted)',
                  border: `1px solid ${typeFilter === t ? 'var(--border-light)' : 'transparent'}`,
                }}>
                {t}
              </button>
            ))}
          </div>

          <span className="text-xs font-mono ml-auto" style={{ color: 'var(--text-muted)' }}>
            {filtered.length} / {allItems.length} results
          </span>
        </div>

        {filtered.length === 0 ? (
          <div className="text-center py-12 text-sm" style={{ color: 'var(--text-muted)' }}>
            {allItems.length === 0 ? '✅ No dead code detected!' : 'No results match your filters'}
          </div>
        ) : (
          <div className="overflow-auto">
            <table className="w-full text-xs font-mono min-w-[800px]">
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  {['Type', 'Name', 'File', 'Line', 'Risk', 'Reason'].map(h => (
                    <th key={h} className="text-left py-2 pr-4 font-medium" style={{ color: 'var(--text-muted)' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map(d => {
                  const risk = RISK_CONFIG[d.riskLevel] || RISK_CONFIG.LOW
                  const type = TYPE_CONFIG[d.itemType] || TYPE_CONFIG.METHOD
                  return (
                    <tr key={d.id} style={{ borderBottom: '1px solid var(--bg-elevated)' }}
                      className="hover:bg-gray-900 transition-colors">
                      <td className="py-3 pr-4">
                        <span className="px-2 py-0.5 rounded text-xs" style={{ background: type.bg, color: type.text }}>
                          {d.itemType}
                        </span>
                      </td>
                      <td className="py-3 pr-4 max-w-[200px]">
                        <span style={{ color: 'var(--text-primary)' }} title={d.qualifiedName}>{d.name}</span>
                      </td>
                      <td className="py-3 pr-4 max-w-[160px] truncate" style={{ color: 'var(--text-muted)' }}>
                        {(d.filePath || '').split('/').pop()}
                      </td>
                      <td className="py-3 pr-4" style={{ color: 'var(--text-muted)' }}>{d.lineNumber}</td>
                      <td className="py-3 pr-4">
                        <span className="flex items-center gap-1.5">
                          <span className="w-1.5 h-1.5 rounded-full" style={{ background: risk.dot }} />
                          <span style={{ color: risk.dot }}>{d.riskLevel}</span>
                        </span>
                      </td>
                      <td className="py-3 pr-4" style={{ color: 'var(--text-secondary)' }}>{d.reason}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
