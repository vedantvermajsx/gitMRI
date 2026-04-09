import { useQuery } from '@tanstack/react-query'
import { getDependencies } from '../api/client'
import { useState, useRef, useEffect, useCallback } from 'react'

const SCOPE_CFG = {
  root:      { color: '#6366f1', bg: 'rgba(99,102,241,0.15)',  border: 'rgba(99,102,241,0.35)' },
  compile:   { color: '#10b981', bg: 'rgba(16,185,129,0.1)',   border: 'rgba(16,185,129,0.25)' },
  runtime:   { color: '#8b5cf6', bg: 'rgba(139,92,246,0.1)',   border: 'rgba(139,92,246,0.25)' },
  test:      { color: '#f59e0b', bg: 'rgba(245,158,11,0.1)',   border: 'rgba(245,158,11,0.25)' },
  provided:  { color: '#ec4899', bg: 'rgba(236,72,153,0.1)',   border: 'rgba(236,72,153,0.25)' },
  processor: { color: '#06b6d4', bg: 'rgba(6,182,212,0.1)',    border: 'rgba(6,182,212,0.25)'  },
  dev:       { color: '#f97316', bg: 'rgba(249,115,22,0.1)',   border: 'rgba(249,115,22,0.25)' },
  peer:      { color: '#84cc16', bg: 'rgba(132,204,22,0.1)',   border: 'rgba(132,204,22,0.25)' },
}
const getCfg = s => SCOPE_CFG[s] || SCOPE_CFG.compile

// ─── Module-level explanation cache ──────────────────────────────────────────
const explainCache = {}
const pendingFetches = {}

async function getExplanation(groupId, artifactId, scope) {
  const key = `${groupId}:${artifactId}`
  if (explainCache[key]) return explainCache[key]
  if (pendingFetches[key]) return pendingFetches[key]

  pendingFetches[key] = fetch('/api/explain', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ groupId, artifactId, scope }),
  })
    .then(r => r.json())
    .then(d => {
      explainCache[key] = d.explanation || 'No description available.'
      delete pendingFetches[key]
      return explainCache[key]
    })
    .catch(() => {
      delete pendingFetches[key]
      return 'Could not load explanation.'
    })

  return pendingFetches[key]
}

// ─── Tooltip component (fixed-position, above cursor) ────────────────────────
function DepTooltip({ dep, x, y }) {
  const cfg = getCfg(dep.scope)
  const [text, setText]       = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setText(null)
    getExplanation(dep.groupId, dep.artifactId, dep.scope).then(t => {
      if (alive) { setText(t); setLoading(false) }
    })
    return () => { alive = false }
  }, [dep.groupId, dep.artifactId, dep.scope])

  const W   = 280
  const left = Math.max(8, Math.min(x - W / 2, (window.innerWidth || 1200) - W - 8))
  const top  = y - 12  // we'll use transform to push up

  return (
    <div style={{
      position: 'fixed',
      left,
      top,
      transform: 'translateY(-100%)',
      width: W,
      zIndex: 9999,
      pointerEvents: 'none',
    }}>
      <div style={{
        background: '#0b1120',
        border: `1px solid ${cfg.color}44`,
        borderRadius: 11,
        padding: '11px 14px 12px',
        boxShadow: `0 12px 40px rgba(0,0,0,0.7), 0 0 0 1px ${cfg.color}18, 0 0 20px ${cfg.color}10`,
      }}>
        {/* Header row */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 8 }}>
          <div style={{
            width: 16, height: 16, borderRadius: 5, flexShrink: 0,
            background: `linear-gradient(135deg, #6366f1, #06b6d4)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 8, fontWeight: 800, color: 'white',
          }}>AI</div>
          <span style={{
            fontSize: 10, letterSpacing: '0.07em', color: 'rgba(140,160,190,0.65)',
            fontFamily: 'system-ui, -apple-system, sans-serif',
          }}>WHAT THIS DOES</span>
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 5 }}>
            <div style={{ width: 5, height: 5, borderRadius: '50%', background: cfg.color }}/>
            <span style={{ fontSize: 9, color: cfg.color, fontFamily: 'system-ui, sans-serif', fontWeight: 600 }}>
              {dep.scope}
            </span>
          </div>
        </div>

        {/* Dep name */}
        <div style={{
          fontSize: 12, fontWeight: 600, color: '#d4e0f5', marginBottom: 8,
          fontFamily: 'system-ui, -apple-system, sans-serif', lineHeight: 1.3,
        }}>
          {dep.artifactId}
          {dep.version && dep.version !== 'unknown' && dep.version !== 'managed' && (
            <span style={{ fontWeight: 400, color: 'rgba(130,150,180,0.6)', marginLeft: 6, fontSize: 10 }}>
              {dep.version}
            </span>
          )}
        </div>

        {/* Explanation or skeleton */}
        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
            {[92, 68].map((w, i) => (
              <div key={i} style={{
                height: 7, borderRadius: 4, width: `${w}%`,
                background: 'linear-gradient(90deg,#1a2338 25%,#243050 50%,#1a2338 75%)',
                backgroundSize: '200% 100%',
                animation: 'shimmer 1.3s ease infinite',
              }}/>
            ))}
          </div>
        ) : (
          <p style={{
            margin: 0, fontSize: 12, lineHeight: 1.65,
            color: 'rgba(195,215,240,0.85)',
            fontFamily: 'system-ui, -apple-system, sans-serif',
          }}>
            {text}
          </p>
        )}
      </div>
      {/* Arrow pointing down */}
      <div style={{ display: 'flex', justifyContent: 'center', marginTop: -1 }}>
        <div style={{
          width: 0, height: 0,
          borderLeft: '7px solid transparent',
          borderRight: '7px solid transparent',
          borderTop: `7px solid ${cfg.color}44`,
        }}/>
      </div>
    </div>
  )
}

// ─── Hook: track mouse position for tooltip anchoring ────────────────────────
function useMousePos() {
  const [pos, setPos] = useState({ x: 0, y: 0 })
  const onMove = useCallback(e => setPos({ x: e.clientX, y: e.clientY }), [])
  useEffect(() => {
    window.addEventListener('mousemove', onMove, { passive: true })
    return () => window.removeEventListener('mousemove', onMove)
  }, [onMove])
  return pos
}

// ─── Wheel graph ──────────────────────────────────────────────────────────────
function WheelGraph({ nodes, edges, onHoverDep }) {
  const [hovered, setHovered] = useState(null)

  const handleEnter = (n) => {
    setHovered(n.id)
    if (n.nodeType !== 'ROOT') onHoverDep(n)
  }
  const handleLeave = () => {
    setHovered(null)
    onHoverDep(null)
  }

  if (!nodes.length) return null

  const roots = nodes.filter(n => n.nodeType === 'ROOT')
  const deps  = nodes.filter(n => n.nodeType !== 'ROOT')

  const NODE_R      = 9
  const ROOT_R      = 20
  const MIN_ARC_GAP = 34
  const LABEL_PAD   = 90

  const ringR = Math.max(100,
    deps.length > 1 ? Math.ceil((MIN_ARC_GAP * deps.length) / (2 * Math.PI)) : 100)

  const canvasR = ringR + LABEL_PAD
  const W = canvasR * 2, H = canvasR * 2
  const CX = canvasR,    CY = canvasR

  const pos = {}
  roots.forEach((r, i) => {
    const a  = roots.length === 1 ? -Math.PI / 2 : (i / roots.length) * 2 * Math.PI - Math.PI / 2
    const rr = roots.length === 1 ? 0 : ROOT_R * 2.4
    pos[r.id] = { x: CX + rr * Math.cos(a), y: CY + rr * Math.sin(a) }
  })
  deps.forEach((n, i) => {
    const a = (i / Math.max(deps.length, 1)) * 2 * Math.PI - Math.PI / 2
    pos[n.id] = { x: CX + ringR * Math.cos(a), y: CY + ringR * Math.sin(a) }
  })

  const labelR = ringR + NODE_R + 8

  return (
    <svg
      viewBox={`0 0 ${W} ${H}`}
      width="100%" height="100%"
      preserveAspectRatio="xMidYMid meet"
      style={{ display: 'block' }}
    >
      <defs>
        <filter id="glow"     x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur stdDeviation="5" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="softglow" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="2.5" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      <circle cx={CX} cy={CY} r={ringR}
        fill="none" stroke="rgba(255,255,255,0.04)"
        strokeWidth="1" strokeDasharray="4 8"/>

      {edges.map((e, i) => {
        const sp = pos[e.sourceId], tp = pos[e.targetId]
        if (!sp || !tp) return null
        const dx  = tp.x - sp.x, dy = tp.y - sp.y
        const len = Math.sqrt(dx*dx + dy*dy)
        if (len < 1) return null
        const tn   = nodes.find(n => n.id === e.targetId)
        const endR = tn?.nodeType === 'ROOT' ? ROOT_R + 2 : NODE_R + 2
        const ex = tp.x - (dx/len)*endR, ey = tp.y - (dy/len)*endR
        const active = hovered === e.sourceId || hovered === e.targetId
        return (
          <line key={i} x1={sp.x} y1={sp.y} x2={ex} y2={ey}
            stroke={active ? getCfg(tn?.scope).color : 'rgba(255,255,255,0.07)'}
            strokeWidth={active ? 1.5 : 1}
            opacity={hovered && !active ? 0.04 : 1}
            style={{ transition: 'all 0.18s' }}
          />
        )
      })}

      {deps.map(n => {
        const p = pos[n.id]
        if (!p) return null
        const cfg    = getCfg(n.scope)
        const isHov  = hovered === n.id
        const isConn = edges.some(e => e.sourceId === hovered && e.targetId === n.id)
        const dim    = !!(hovered && !isHov && !isConn)

        const dx  = p.x - CX, dy = p.y - CY
        const ang = Math.atan2(dy, dx)
        const lx  = CX + labelR * Math.cos(ang)
        const ly  = CY + labelR * Math.sin(ang)
        const anchor = Math.abs(Math.cos(ang)) < 0.15 ? 'middle'
                     : Math.cos(ang) > 0 ? 'start' : 'end'
        const label = (n.artifactId || '').replace(/^@[^/]+\//, '').slice(0, 22)

        return (
          <g key={n.id}
            onMouseEnter={() => handleEnter(n)}
            onMouseLeave={handleLeave}
            style={{ cursor: 'default' }}>
            {isHov && <circle cx={p.x} cy={p.y} r={NODE_R+9} fill={cfg.color} opacity={0.12}/>}
            <circle cx={p.x} cy={p.y} r={NODE_R}
              fill={cfg.bg} stroke={cfg.color}
              strokeWidth={isHov ? 1.8 : 0.8}
              opacity={dim ? 0.1 : 1}
              filter={isHov ? 'url(#softglow)' : 'none'}
              style={{ transition: 'all 0.15s' }}
            />
            <text x={lx} y={ly}
              textAnchor={anchor} dominantBaseline="middle"
              fontSize={9} fontFamily="system-ui,-apple-system,sans-serif"
              fontWeight={isHov ? 600 : 400}
              fill={dim ? 'rgba(255,255,255,0.06)' : isHov ? cfg.color : 'rgba(180,195,220,0.7)'}
              style={{ transition: 'fill 0.15s', pointerEvents: 'none' }}>
              {label}
            </text>
          </g>
        )
      })}

      {roots.map(n => {
        const p     = pos[n.id]
        if (!p) return null
        const isHov = hovered === n.id
        const label = (n.artifactId || '').slice(0, 13)
        return (
          <g key={n.id}
            onMouseEnter={() => handleEnter(n)}
            onMouseLeave={handleLeave}
            style={{ cursor: 'default' }}>
            {isHov && <circle cx={p.x} cy={p.y} r={ROOT_R+10} fill="#6366f1" opacity={0.12}/>}
            <circle cx={p.x} cy={p.y} r={ROOT_R}
              fill="#6366f1" stroke="rgba(255,255,255,0.2)" strokeWidth={1.5}
              filter={isHov ? 'url(#glow)' : 'none'}
              style={{ transition: 'all 0.15s' }}
            />
            <text x={p.x} y={p.y - 2} textAnchor="middle" dominantBaseline="middle"
              fontSize={7.5} fontFamily="system-ui,-apple-system,sans-serif"
              fontWeight={700} fill="white" style={{ pointerEvents: 'none' }}>
              {label}
            </text>
            <text x={p.x} y={p.y + 9} textAnchor="middle"
              fontSize={6} fontFamily="system-ui,-apple-system,sans-serif"
              fill="rgba(255,255,255,0.45)" style={{ pointerEvents: 'none' }}>
              ROOT
            </text>
          </g>
        )
      })}
    </svg>
  )
}

// ─── Dep row (list below wheel) ───────────────────────────────────────────────
function DepRow({ n, onHover }) {
  const cfg = getCfg(n.scope)
  const [hov, setHov] = useState(false)

  return (
    <div
      onMouseEnter={e => { setHov(true); onHover(n, e) }}
      onMouseLeave={() => { setHov(false); onHover(null) }}
      style={{
        display: 'flex', alignItems: 'center', gap: 11,
        padding: '8px 16px',
        borderBottom: '1px solid var(--border)',
        background: hov ? 'var(--bg-elevated)' : 'transparent',
        transition: 'background 0.1s',
        cursor: 'default',
      }}>
      <div style={{
        width: 6, height: 6, borderRadius: '50%', flexShrink: 0,
        background: cfg.color,
        boxShadow: hov ? `0 0 6px ${cfg.color}99` : 'none',
        transition: 'box-shadow 0.15s',
      }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 12.5, fontWeight: 500, letterSpacing: '-0.01em',
          color: hov ? '#e8edf5' : 'rgba(210,222,240,0.82)',
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          fontFamily: 'system-ui,-apple-system,sans-serif',
        }}>
          {n.artifactId}
        </div>
        <div style={{
          fontSize: 10.5, color: 'var(--text-muted)', marginTop: 1,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          fontFamily: 'system-ui,-apple-system,sans-serif',
        }}>
          {n.groupId}
        </div>
      </div>
      <div style={{ flexShrink: 0, textAlign: 'right' }}>
        {n.version && n.version !== 'unknown' && n.version !== 'managed' && (
          <div style={{
            fontSize: 10.5, color: 'var(--text-muted)', marginBottom: 3,
            fontVariantNumeric: 'tabular-nums', fontFamily: 'system-ui,-apple-system,sans-serif',
          }}>{n.version}</div>
        )}
        <span style={{
          fontSize: 9.5, fontWeight: 600, letterSpacing: '0.03em',
          padding: '2px 7px', borderRadius: 99,
          background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}`,
          fontFamily: 'system-ui,-apple-system,sans-serif',
        }}>{n.scope}</span>
      </div>
    </div>
  )
}

// ─── Main ─────────────────────────────────────────────────────────────────────
export default function DependenciesTab({ jobId }) {
  const { data, isLoading } = useQuery({
    queryKey: ['dependencies', jobId],
    queryFn: () => getDependencies(jobId),
  })
  const [search, setSearch]           = useState('')
  const [scopeFilter, setScopeFilter] = useState('ALL')
  const [hoveredDep, setHoveredDep]   = useState(null)
  const mousePos = useMousePos()

  if (isLoading) return (
    <div style={{ color: 'var(--text-muted)', padding: 60, textAlign: 'center', fontSize: 13 }}>
      Parsing dependency graph…
    </div>
  )

  const nodes  = data?.nodes || []
  const edges  = data?.edges || []
  const deps   = nodes.filter(n => n.nodeType !== 'ROOT')
  const roots  = nodes.filter(n => n.nodeType === 'ROOT')
  const scopes = [...new Set(deps.map(d => d.scope))].sort()

  const filtered = deps.filter(d => {
    if (scopeFilter !== 'ALL' && d.scope !== scopeFilter) return false
    if (search) {
      const q = search.toLowerCase()
      return d.artifactId?.toLowerCase().includes(q) || d.groupId?.toLowerCase().includes(q)
    }
    return true
  })

  const byScopeCount = deps.reduce((acc, n) => {
    acc[n.scope] = (acc[n.scope] || 0) + 1; return acc
  }, {})

  if (nodes.length === 0) return (
    <div className="card" style={{ textAlign: 'center', padding: 60 }}>
      <div style={{ fontSize: 30, marginBottom: 12, opacity: 0.25 }}>⬡</div>
      <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 8 }}>No dependencies found</div>
      <p style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.7 }}>
        Could not find pom.xml, build.gradle, or package.json.
      </p>
    </div>
  )

  const numStyle = {
    fontFamily: 'system-ui,-apple-system,"Segoe UI",sans-serif',
    fontVariantNumeric: 'tabular-nums',
    fontWeight: 700, lineHeight: 1,
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>

      {/* Tooltip (follows mouse globally) */}
      {hoveredDep && (
        <DepTooltip dep={hoveredDep} x={mousePos.x} y={mousePos.y} />
      )}

      {/* ── Stat row ── */}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="card" style={{ padding: '11px 18px', display: 'flex', alignItems: 'baseline', gap: 9 }}>
          <span style={{ ...numStyle, fontSize: 28, color: 'var(--accent)' }}>{deps.length}</span>
          <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>dependencies</span>
        </div>
        <div className="card" style={{ padding: '11px 18px', display: 'flex', alignItems: 'baseline', gap: 9 }}>
          <span style={{ ...numStyle, fontSize: 28, color: 'var(--purple)' }}>{roots.length}</span>
          <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>build {roots.length === 1 ? 'file' : 'files'}</span>
        </div>
        {Object.entries(byScopeCount).map(([scope, count]) => {
          const cfg = getCfg(scope), active = scopeFilter === scope
          return (
            <div key={scope} className="card"
              onClick={() => setScopeFilter(active ? 'ALL' : scope)}
              style={{
                padding: '11px 18px', display: 'flex', alignItems: 'baseline', gap: 8,
                cursor: 'pointer', borderColor: active ? cfg.color : cfg.border,
                background: active ? cfg.bg : 'var(--bg-card)', transition: 'all 0.15s',
              }}>
              <span style={{ ...numStyle, fontSize: 22, color: cfg.color }}>{count}</span>
              <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{scope}</span>
            </div>
          )
        })}
      </div>

      {/* ── Build files ── */}
      {roots.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {roots.map((r, i) => (
            <div key={i} style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              padding: '5px 13px', borderRadius: 8,
              background: getCfg('root').bg, border: `1px solid ${getCfg('root').border}`,
              fontSize: 12, fontFamily: 'system-ui,-apple-system,sans-serif',
            }}>
              <span style={{ color: '#6366f1' }}>⬡</span>
              <span style={{ color: 'var(--text-secondary)' }}>{r.groupId}</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{r.artifactId}</span>
              {r.version && <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>v{r.version}</span>}
            </div>
          ))}
        </div>
      )}

      {/* ── Wheel — full width ── */}
      <div className="card" style={{ padding: 0, overflow: 'visible' }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '12px 18px', borderBottom: '1px solid var(--border)',
        }}>
          <span style={{ fontSize: 10.5, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>
            DEPENDENCY WHEEL
            <span style={{ marginLeft: 10, fontWeight: 400, color: 'var(--text-muted)', fontSize: 10 }}>
              — hover a node for AI explanation
            </span>
          </span>
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }}>
            {scopes.map(s => {
              const cfg = getCfg(s)
              return (
                <div key={s} style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                  <div style={{ width: 5, height: 5, borderRadius: '50%', background: cfg.color }}/>
                  <span style={{ fontSize: 10, color: 'var(--text-muted)', fontFamily: 'system-ui,sans-serif' }}>{s}</span>
                </div>
              )
            })}
          </div>
        </div>
        {/* SVG fills available width, aspect-ratio preserved */}
        <div style={{ width: '100%', minHeight: 500, padding: 20 }}>
          <WheelGraph nodes={nodes} edges={edges} onHoverDep={setHoveredDep}/>
        </div>
      </div>

      {/* ── Package list — full width, below wheel ── */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {/* List header */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 14,
          padding: '12px 18px', borderBottom: '1px solid var(--border)', flexWrap: 'wrap',
        }}>
          <span style={{ fontSize: 10.5, color: 'var(--text-muted)', letterSpacing: '0.08em', flexShrink: 0 }}>
            PACKAGES — <span style={{ fontVariantNumeric: 'tabular-nums' }}>{filtered.length}</span>
          </span>

          {/* Search */}
          <div style={{ position: 'relative', flex: '1 1 200px', maxWidth: 320 }}>
            <span style={{
              position: 'absolute', left: 9, top: '50%', transform: 'translateY(-50%)',
              fontSize: 13, color: 'var(--text-muted)', pointerEvents: 'none',
            }}>⌕</span>
            <input
              value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Filter packages…"
              style={{
                width: '100%', background: 'var(--bg-elevated)',
                border: '1px solid var(--border-md)', borderRadius: 8,
                color: 'var(--text-primary)', fontSize: 12,
                padding: '6px 10px 6px 28px', outline: 'none',
                transition: 'border-color 0.15s', boxSizing: 'border-box',
                fontFamily: 'system-ui,-apple-system,sans-serif',
              }}
              onFocus={e => e.target.style.borderColor = 'var(--accent)'}
              onBlur={e => e.target.style.borderColor = 'var(--border-md)'}
            />
          </div>

          {/* Scope chips */}
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {['ALL', ...scopes].map(s => {
              const cfg = s === 'ALL' ? null : getCfg(s)
              const active = scopeFilter === s
              return (
                <button key={s}
                  onClick={() => setScopeFilter(s === scopeFilter && s !== 'ALL' ? 'ALL' : s)}
                  style={{
                    fontSize: 10, padding: '3px 9px', borderRadius: 99, cursor: 'pointer',
                    border: '1px solid',
                    background: active ? (cfg?.bg || 'rgba(99,102,241,0.15)') : 'transparent',
                    color: active ? (cfg?.color || '#6366f1') : 'var(--text-muted)',
                    borderColor: active ? (cfg?.border || 'rgba(99,102,241,0.35)') : 'transparent',
                    fontWeight: active ? 600 : 400, transition: 'all 0.12s',
                    fontFamily: 'system-ui,-apple-system,sans-serif',
                  }}>
                  {s}
                </button>
              )
            })}
          </div>
        </div>

        {/* Grid of rows — 2 columns on wide screens */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
          maxHeight: 380,
          overflow: 'hidden',
        }} className="scroll-hidden">
          {filtered.length === 0 ? (
            <div style={{ fontSize: 13, color: 'var(--text-muted)', textAlign: 'center', padding: 40, gridColumn: '1/-1' }}>
              No results
            </div>
          ) : filtered.map((n, i) => (
            <DepRow key={n.id || i} n={n} onHover={(dep) => setHoveredDep(dep || null)} />
          ))}
        </div>
      </div>
    </div>
  )
}
