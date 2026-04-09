import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getJobStatus, getReport } from '../api/client'
import OverviewTab from '../components/OverviewTab'
import ComplexityTab from '../components/ComplexityTab'
import DeadCodeTab from '../components/DeadCodeTab'
import ContributorsTab from '../components/ContributorsTab'
import DependenciesTab from '../components/DependenciesTab'

const STAGES = [
  { label: 'Clone',    status: 'CLONING',      icon: '⤓' },
  { label: 'Parse',    status: 'PARSING',       icon: '◈' },
  { label: 'Analyze',  status: 'ANALYZING',     icon: '◎' },
  { label: 'Git',      status: 'HOTSPOTS',      icon: '▣' },
  { label: 'Deps',     status: 'DEPENDENCIES',  icon: '⬡' },
  { label: 'Report',   status: 'REPORTING',     icon: '◆' },
  { label: 'Done',     status: 'DONE',          icon: '✓' },
]

const TABS = [
  { id: 'overview',     label: 'Overview',     icon: '◆' },
  { id: 'complexity',   label: 'Complexity',   icon: '◈' },
  { id: 'deadcode',     label: 'Dead Code',    icon: '◎' },
  { id: 'contributors', label: 'Contributors', icon: '◉' },
  { id: 'dependencies', label: 'Deps',         icon: '⬡' },
]

function HealthBadge({ score }) {
  const s = Math.round(score)
  const [bg, fg, label] =
    s >= 80 ? ['rgba(16,185,129,0.12)', '#10b981', 'Healthy'] :
    s >= 60 ? ['rgba(249,115,22,0.12)', '#f97316', 'Moderate'] :
              ['rgba(239,68,68,0.12)',   '#ef4444', 'Critical']
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8, padding: '6px 14px',
      borderRadius: 99, background: bg, border: `1px solid ${fg}44`,
    }}>
      <div style={{ width: 6, height: 6, borderRadius: '50%', background: fg, animation: 'pulse-glow 2s ease infinite' }} />
      <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: fg, fontWeight: 600 }}>
        {s} · {label}
      </span>
    </div>
  )
}

export default function Report() {
  const { jobId } = useParams()
  const [activeTab, setActiveTab] = useState('overview')

  const { data: jobStatus } = useQuery({
    queryKey: ['job-status', jobId],
    queryFn: () => getJobStatus(jobId),
    refetchInterval: (data) => {
      if (!data) return 2000
      return (data.status === 'DONE' || data.status === 'FAILED') ? false : 2000
    },
  })

  const isDone   = jobStatus?.status === 'DONE'
  const isFailed = jobStatus?.status === 'FAILED'

  const { data: report } = useQuery({
    queryKey: ['report', jobId],
    queryFn: () => getReport(jobId),
    enabled: isDone,
  })

  const progress    = jobStatus?.progress || 0
  const currentStage = jobStatus?.currentStage || 'Queued'
  const stageIndex  = STAGES.findIndex(s => s.status === jobStatus?.status)

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--bg-base)', position: 'relative' }}>

      {/* Header */}
      <header style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '14px 28px',
        borderBottom: '1px solid var(--border)', position: 'sticky', top: 0,
        background: 'rgba(3,4,10,0.85)', backdropFilter: 'blur(16px)', zIndex: 50,
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

        <span style={{ color: 'var(--border-light)', fontSize: 16 }}>›</span>
        <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {jobStatus?.repoName || jobId}
        </span>

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 10 }}>
          {isDone && report && <HealthBadge score={report.healthScore} />}
          {isDone && (
            <Link to="/" className="btn btn-ghost" style={{ fontSize: 11 }}>
              + New Analysis
            </Link>
          )}
        </div>
      </header>

      {/* Progress view */}
      {!isDone && !isFailed && (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 40 }}>
          <div className="animate-fade-up card" style={{ width: '100%', maxWidth: 640, padding: 36 }}>

            {/* Repo name */}
            <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'var(--text-muted)', marginBottom: 6 }}>ANALYZING</div>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 20, marginBottom: 28, color: 'var(--text-primary)' }}>
              {jobStatus?.repoName || '...'}
            </div>

            {/* Stage steps */}
            <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: 32, position: 'relative' }}>
              {STAGES.map((stage, i) => {
                const done   = stageIndex > i
                const active = stageIndex === i
                return (
                  <div key={stage.label} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, position: 'relative' }}>
                    {/* Connector line */}
                    {i < STAGES.length - 1 && (
                      <div style={{
                        position: 'absolute', top: 16, left: '50%', width: '100%', height: 1,
                        background: done ? 'var(--accent)' : 'var(--border)',
                        transition: 'background 0.4s',
                        zIndex: 0,
                      }} />
                    )}
                    {/* Circle */}
                    <div style={{
                      width: 32, height: 32, borderRadius: '50%', zIndex: 1,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontFamily: 'IBM Plex Mono', fontSize: 12, fontWeight: 700,
                      background: done ? 'var(--accent)' : active ? 'rgba(59,130,246,0.15)' : 'var(--bg-elevated)',
                      border: `2px solid ${done ? 'var(--accent)' : active ? 'var(--accent)' : 'var(--border-md)'}`,
                      color: done ? 'white' : active ? 'var(--accent)' : 'var(--text-muted)',
                      boxShadow: active ? '0 0 16px rgba(59,130,246,0.4)' : 'none',
                      transition: 'all 0.3s',
                    }}>
                      {done ? '✓' : stage.icon}
                    </div>
                    <span style={{
                      fontFamily: 'IBM Plex Mono', fontSize: 9, marginTop: 6,
                      color: active ? 'var(--accent)' : done ? 'var(--text-secondary)' : 'var(--text-muted)',
                      letterSpacing: '0.05em',
                    }}>
                      {stage.label.toUpperCase()}
                    </span>
                  </div>
                )
              })}
            </div>

            {/* Progress bar */}
            <div style={{ height: 4, borderRadius: 99, background: 'var(--bg-elevated)', overflow: 'hidden', marginBottom: 12 }}>
              <div className="shimmer-bar" style={{ height: '100%', width: `${progress}%`, borderRadius: 99, transition: 'width 0.6s ease' }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'IBM Plex Mono', fontSize: 11 }}>
              <span style={{ color: 'var(--text-muted)' }}>{currentStage}</span>
              <span style={{ color: 'var(--accent)' }}>{progress}%</span>
            </div>
          </div>
        </div>
      )}

      {/* Failed */}
      {isFailed && (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 40 }}>
          <div className="animate-fade-up card" style={{ maxWidth: 480, width: '100%', padding: 40, textAlign: 'center', borderColor: 'rgba(239,68,68,0.3)' }}>
            <div style={{ fontSize: 40, marginBottom: 16 }}>◎</div>
            <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 22, color: 'var(--red)', marginBottom: 10 }}>
              Analysis Failed
            </div>
            <p style={{ fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)', marginBottom: 24, lineHeight: 1.7 }}>
              {jobStatus?.errorMessage || 'An unknown error occurred'}
            </p>
            <Link to="/" className="btn btn-primary">← Try Another Repository</Link>
          </div>
        </div>
      )}

      {/* Dashboard */}
      {isDone && (
        <>
          {/* Tab bar */}
          <div style={{
            display: 'flex', alignItems: 'center', borderBottom: '1px solid var(--border)',
            padding: '0 28px', background: 'rgba(3,4,10,0.6)', backdropFilter: 'blur(8px)',
            position: 'sticky', top: 57, zIndex: 40,
          }}>
            {TABS.map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)} style={{
                display: 'flex', alignItems: 'center', gap: 7,
                padding: '13px 16px', cursor: 'pointer', background: 'none', border: 'none',
                fontFamily: 'IBM Plex Mono', fontSize: 12, fontWeight: activeTab === tab.id ? 600 : 400,
                color: activeTab === tab.id ? 'var(--text-primary)' : 'var(--text-muted)',
                position: 'relative', transition: 'color 0.15s', whiteSpace: 'nowrap',
              }}>
                <span style={{ color: activeTab === tab.id ? 'var(--accent)' : 'var(--text-muted)', fontSize: 11 }}>{tab.icon}</span>
                {tab.label}
                {activeTab === tab.id && (
                  <div style={{
                    position: 'absolute', bottom: 0, left: 0, right: 0, height: 2,
                    background: 'linear-gradient(90deg, var(--accent), var(--cyan))',
                    borderRadius: '2px 2px 0 0',
                  }} />
                )}
              </button>
            ))}
          </div>

          {/* Content */}
          <div style={{ flex: 1, padding: '24px 28px', overflow: 'auto' }} className="animate-fade-in">
            {activeTab === 'overview'     && <OverviewTab jobId={jobId} report={report} />}
            {activeTab === 'complexity'   && <ComplexityTab jobId={jobId} />}
            {activeTab === 'deadcode'     && <DeadCodeTab jobId={jobId} />}
            {activeTab === 'contributors' && <ContributorsTab jobId={jobId} report={report} />}
            {activeTab === 'dependencies' && <DependenciesTab jobId={jobId} />}
          </div>
        </>
      )}
    </div>
  )
}
