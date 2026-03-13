import { useQuery } from '@tanstack/react-query'
import { getDependencies } from '../api/client'
import { useRef, useEffect, useState } from 'react'

const SCOPE_COLORS = { root: '#6366f1', compile: '#3b82f6', runtime: '#10b981', test: '#f59e0b', provided: '#8b5cf6' }

function DepGraph({ nodes, edges }) {
  const [hovered, setHovered] = useState(null)
  const positions = {}

  const root = nodes.find(n => n.nodeType === 'ROOT') || nodes[0]
  if (!root) return null

  const cx = 320, cy = 240, r = 170
  positions[root.id] = { x: cx, y: cy }

  const others = nodes.filter(n => n.id !== root.id)
  others.forEach((n, i) => {
    const angle = (i / others.length) * 2 * Math.PI - Math.PI / 2
    positions[n.id] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
  })

  return (
    <svg viewBox="0 0 640 480" className="w-full h-full" style={{ background: 'transparent' }}>
      <defs>
        <marker id="arrowhead" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L8,3 L0,6 Z" fill="#374151" />
        </marker>
      </defs>

      {/* Edges */}
      {edges.map((e, i) => {
        const sp = positions[e.sourceId], tp = positions[e.targetId]
        if (!sp || !tp) return null
        const dx = tp.x - sp.x, dy = tp.y - sp.y
        const len = Math.sqrt(dx * dx + dy * dy)
        if (len === 0) return null
        const ex = tp.x - (dx / len) * 16, ey = tp.y - (dy / len) * 16
        const isActive = hovered === e.sourceId || hovered === e.targetId
        return (
          <line key={i} x1={sp.x} y1={sp.y} x2={ex} y2={ey}
            stroke={isActive ? '#6366f1' : '#374151'}
            strokeWidth={isActive ? 2 : 1.5}
            opacity={hovered ? (isActive ? 1 : 0.2) : 0.5}
            markerEnd="url(#arrowhead)"
            style={{ transition: 'opacity 0.2s, stroke 0.2s' }} />
        )
      })}

      {/* Nodes */}
      {nodes.map(n => {
        const p = positions[n.id]
        if (!p) return null
        const isRoot = n.nodeType === 'ROOT'
        const radius = isRoot ? 30 : 20
        const color = SCOPE_COLORS[n.scope] || '#6b7280'
        const isHov = hovered === n.id
        return (
          <g key={n.id} transform={`translate(${p.x},${p.y})`}
            onMouseEnter={() => setHovered(n.id)}
            onMouseLeave={() => setHovered(null)}
            style={{ cursor: 'pointer' }}>
            <circle r={radius + (isHov ? 3 : 0)} fill={color}
              opacity={hovered && !isHov ? 0.4 : 0.9}
              stroke={isRoot || isHov ? 'white' : 'transparent'}
              strokeWidth="2"
              style={{ transition: 'all 0.2s' }} />
            {/* Label above for non-root */}
            {!isRoot && (
              <text y={-radius - 4} textAnchor="middle" fontSize={8.5}
                fill={hovered && !isHov ? '#6b7280' : '#e5e7eb'}
                style={{ transition: 'fill 0.2s' }}>
                {(n.label || n.artifactId || '').length > 18
                  ? (n.label || n.artifactId || '').slice(0, 17) + '…'
                  : (n.label || n.artifactId || '')}
              </text>
            )}
            {isRoot && (
              <>
                <text textAnchor="middle" y="-4" fontSize={9} fill="white" fontWeight="700">
                  {(n.label || n.artifactId || '').slice(0, 12)}
                </text>
                <text textAnchor="middle" y="8" fontSize={7} fill="rgba(255,255,255,0.6)">ROOT</text>
              </>
            )}
          </g>
        )
      })}
    </svg>
  )
}

export default function DependenciesTab({ jobId }) {
  const { data, isLoading } = useQuery({
    queryKey: ['dependencies', jobId],
    queryFn: () => getDependencies(jobId),
  })

  if (isLoading) return <div className="py-20 text-center font-mono text-sm" style={{ color: 'var(--text-muted)' }}>Parsing dependency graph...</div>

  const nodes = data?.nodes || []
  const edges = data?.edges || []
  const deps = nodes.filter(n => n.nodeType !== 'ROOT')

  const byScopeCount = deps.reduce((acc, n) => {
    acc[n.scope] = (acc[n.scope] || 0) + 1
    return acc
  }, {})

  return (
    <div className="grid grid-cols-12 gap-4">
      {/* Graph */}
      <div className="col-span-7 card" style={{ height: 460 }}>
        <div className="text-xs font-mono uppercase tracking-widest mb-3" style={{ color: 'var(--text-muted)' }}>
          Dependency Graph
        </div>
        <div className="flex gap-4 mb-3">
          {Object.entries(SCOPE_COLORS).filter(([k]) => k !== 'root').map(([scope, color]) => (
            <div key={scope} className="flex items-center gap-1.5">
              <div className="w-2.5 h-2.5 rounded-full" style={{ background: color }} />
              <span className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>{scope}</span>
            </div>
          ))}
        </div>
        {nodes.length > 0 ? (
          <div style={{ height: 340 }}>
            <DepGraph nodes={nodes} edges={edges} />
          </div>
        ) : (
          <div className="flex items-center justify-center h-64 text-sm" style={{ color: 'var(--text-muted)' }}>
            No dependency data — check for pom.xml or build.gradle
          </div>
        )}
      </div>

      {/* Sidebar */}
      <div className="col-span-5 space-y-4">
        {/* Scope breakdown */}
        <div className="card">
          <div className="text-xs font-mono uppercase tracking-widest mb-3" style={{ color: 'var(--text-muted)' }}>
            By Scope
          </div>
          <div className="space-y-2">
            {Object.entries(byScopeCount).map(([scope, count]) => (
              <div key={scope} className="flex items-center gap-3">
                <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: SCOPE_COLORS[scope] || '#6b7280' }} />
                <span className="text-sm font-mono flex-1">{scope}</span>
                <span className="font-mono font-bold text-sm">{count}</span>
                <div className="w-24 h-1.5 rounded-full" style={{ background: 'var(--border)' }}>
                  <div className="h-1.5 rounded-full" style={{
                    width: `${(count / deps.length) * 100}%`,
                    background: SCOPE_COLORS[scope] || '#6b7280'
                  }} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Dependency List */}
        <div className="card" style={{ maxHeight: 300, overflowY: 'auto' }}>
          <div className="text-xs font-mono uppercase tracking-widest mb-3" style={{ color: 'var(--text-muted)' }}>
            All Dependencies ({deps.length})
          </div>
          <div className="space-y-1.5">
            {deps.map(n => (
              <div key={n.id} className="flex items-center gap-2 py-1"
                style={{ borderBottom: '1px solid var(--bg-elevated)' }}>
                <div className="w-2 h-2 rounded-full flex-shrink-0"
                  style={{ background: SCOPE_COLORS[n.scope] || '#6b7280' }} />
                <div className="flex-1 min-w-0">
                  <span className="text-xs font-mono" style={{ color: 'var(--text-secondary)' }}>
                    {n.groupId}.
                  </span>
                  <span className="text-xs font-mono font-bold" style={{ color: 'var(--text-primary)' }}>
                    {n.artifactId}
                  </span>
                </div>
                <span className="text-xs font-mono flex-shrink-0" style={{ color: 'var(--text-muted)' }}>
                  {n.version}
                </span>
                <span className="text-xs px-1.5 py-0.5 rounded flex-shrink-0"
                  style={{ background: (SCOPE_COLORS[n.scope] || '#6b7280') + '22', color: SCOPE_COLORS[n.scope] || '#6b7280' }}>
                  {n.scope}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
