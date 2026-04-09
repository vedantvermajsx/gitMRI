import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listJobs, deleteJob } from '../api/client'

const STATUS_CFG = {
  DONE:         { color: '#10b981', bg: 'rgba(16,185,129,0.1)',  label: 'Done'     },
  FAILED:       { color: '#ef4444', bg: 'rgba(239,68,68,0.1)',   label: 'Failed'   },
  PENDING:      { color: '#6b7280', bg: 'rgba(107,114,128,0.1)', label: 'Pending'  },
  CLONING:      { color: '#3b82f6', bg: 'rgba(59,130,246,0.1)',  label: 'Cloning'  },
  PARSING:      { color: '#8b5cf6', bg: 'rgba(139,92,246,0.1)',  label: 'Parsing'  },
  ANALYZING:    { color: '#f59e0b', bg: 'rgba(245,158,11,0.1)',  label: 'Analyzing'},
  HOTSPOTS:     { color: '#f97316', bg: 'rgba(249,115,22,0.1)',  label: 'Git'      },
  DEPENDENCIES: { color: '#06b6d4', bg: 'rgba(6,182,212,0.1)',   label: 'Deps'     },
  REPORTING:    { color: '#84cc16', bg: 'rgba(132,204,22,0.1)',  label: 'Reporting'},
}

function StatusBadge({ status }) {
  const cfg = STATUS_CFG[status] || STATUS_CFG.PENDING
  const isActive = !['DONE','FAILED'].includes(status)
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      fontFamily: 'IBM Plex Mono', fontSize: 10, fontWeight: 600,
      padding: '3px 10px', borderRadius: 99,
      background: cfg.bg, color: cfg.color,
    }}>
      <span style={{ width: 5, height: 5, borderRadius: '50%', background: cfg.color, animation: isActive ? 'pulse-glow 2s ease infinite' : 'none' }} />
      {cfg.label}
    </span>
  )
}

export default function History() {
  const { data: jobs = [], isLoading, refetch } = useQuery({
    queryKey: ['jobs'],
    queryFn: listJobs,
    refetchInterval: 5000,
  })

  const handleDelete = async (jobId) => {
    await deleteJob(jobId)
    refetch()
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-base)' }}>
      {/* Nav */}
      <nav style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 32px', borderBottom: '1px solid var(--border)',
        background: 'rgba(3,4,10,0.85)', backdropFilter: 'blur(16px)',
        position: 'sticky', top: 0, zIndex: 50,
      }}>
        <Link to="/" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{
            width: 24, height: 24, borderRadius: 6, background: 'var(--accent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 12, fontWeight: 800, color: 'white',
          }}>R</div>
          <span style={{ fontFamily: 'IBM Plex Mono', fontWeight: 700, fontSize: 13, color: 'var(--text-secondary)' }}>
            repo<span style={{ color: 'var(--accent)' }}>intel</span>
          </span>
        </Link>
        <Link to="/" className="btn btn-primary" style={{ fontSize: 11 }}>
          + New Analysis
        </Link>
      </nav>

      <div style={{ maxWidth: 900, margin: '0 auto', padding: '40px 24px' }}>
        <div style={{ marginBottom: 32 }}>
          <h1 style={{ fontFamily: 'Syne', fontWeight: 800, fontSize: 32, marginBottom: 6 }}>Analysis History</h1>
          <p style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)' }}>
            {jobs.length} past {jobs.length === 1 ? 'analysis' : 'analyses'}
          </p>
        </div>

        {isLoading ? (
          <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', textAlign: 'center', padding: 60 }}>
            Loading...
          </div>
        ) : jobs.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: 60 }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>◎</div>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, marginBottom: 8 }}>No analyses yet</div>
            <p style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'var(--text-muted)', marginBottom: 20 }}>
              Analyze your first Java repository to see results here
            </p>
            <Link to="/" className="btn btn-primary">Start Analysis →</Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {jobs.map(job => {
              const isDone = job.status === 'DONE'
              const isActive = !['DONE','FAILED'].includes(job.status)
              return (
                <div key={job.jobId} className="card" style={{
                  padding: '16px 20px',
                  display: 'flex', alignItems: 'center', gap: 16,
                  transition: 'border-color 0.15s',
                }}
                  onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--border-md)'}
                  onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
                >
                  {/* Status dot */}
                  <StatusBadge status={job.status} />

                  {/* Repo info */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {job.repoName || job.jobId}
                    </div>
                    <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', marginTop: 3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {job.repoUrl}
                    </div>
                  </div>

                  {/* Progress (active) */}
                  {isActive && (
                    <div style={{ width: 120 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--text-muted)' }}>{job.currentStage}</span>
                        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 9, color: 'var(--accent)' }}>{job.progress}%</span>
                      </div>
                      <div style={{ height: 2, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
                        <div className="shimmer-bar" style={{ width: `${job.progress}%`, height: '100%', borderRadius: 99 }} />
                      </div>
                    </div>
                  )}

                  {/* Time */}
                  <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)', flexShrink: 0 }}>
                    {job.createdAt ? new Date(job.createdAt).toLocaleDateString() : '—'}
                  </span>

                  {/* Actions */}
                  <div style={{ display: 'flex', gap: 6 }}>
                    {isDone && (
                      <Link to={`/report/${job.jobId}`} className="btn btn-primary" style={{ fontSize: 10, padding: '5px 14px' }}>
                        View Report
                      </Link>
                    )}
                    {isActive && (
                      <Link to={`/report/${job.jobId}`} className="btn btn-ghost" style={{ fontSize: 10, padding: '5px 14px' }}>
                        Watch →
                      </Link>
                    )}
                    <button onClick={() => handleDelete(job.jobId)} className="btn btn-ghost"
                      style={{ fontSize: 10, padding: '5px 10px', color: 'var(--text-muted)' }}>
                      ✕
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
