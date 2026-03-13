import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getJobStatus, getReport } from '../api/client'
import OverviewTab from '../components/OverviewTab'
import ComplexityTab from '../components/ComplexityTab'
import DeadCodeTab from '../components/DeadCodeTab'
import ContributorsTab from '../components/ContributorsTab'
import DependenciesTab from '../components/DependenciesTab'

const STAGES = [
  { label: 'Clone', status: 'CLONING', pct: 15 },
  { label: 'Parse', status: 'PARSING', pct: 30 },
  { label: 'Analyze', status: 'ANALYZING', pct: 50 },
  { label: 'Git', status: 'HOTSPOTS', pct: 65 },
  { label: 'Deps', status: 'DEPENDENCIES', pct: 80 },
  { label: 'Report', status: 'REPORTING', pct: 92 },
  { label: 'Done', status: 'DONE', pct: 100 },
]

const TABS = [
  { id: 'overview', label: 'Overview', icon: '📊' },
  { id: 'complexity', label: 'Complexity', icon: '⚡' },
  { id: 'deadcode', label: 'Dead Code', icon: '💀' },
  { id: 'contributors', label: 'Contributors', icon: '👥' },
  { id: 'dependencies', label: 'Dependencies', icon: '🕸' },
]

export default function Report() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('overview')

  // Poll job status until done
  const { data: jobStatus } = useQuery({
    queryKey: ['job-status', jobId],
    queryFn: () => getJobStatus(jobId),
    refetchInterval: (data) => {
      if (!data) return 2000
      const s = data.status
      return (s === 'DONE' || s === 'FAILED') ? false : 2000
    },
  })

  const isDone = jobStatus?.status === 'DONE'
  const isFailed = jobStatus?.status === 'FAILED'
  const isAnalyzing = !isDone && !isFailed

  // Fetch report once done
  const { data: report } = useQuery({
    queryKey: ['report', jobId],
    queryFn: () => getReport(jobId),
    enabled: isDone,
  })

  const progress = jobStatus?.progress || 0
  const currentStage = jobStatus?.currentStage || 'Queued'
  const stageIndex = STAGES.findIndex(s => s.status === jobStatus?.status)

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--bg-base)' }}>
      {/* Header */}
      <header className="flex items-center gap-4 px-6 py-4 border-b" style={{ borderColor: 'var(--border)' }}>
        <Link to="/" className="font-mono font-black text-lg tracking-tight">
          REPO<span style={{ color: 'var(--accent)' }}>INTEL</span>
        </Link>
        <span style={{ color: 'var(--text-muted)' }}>›</span>
        <span className="font-mono text-sm" style={{ color: 'var(--text-secondary)' }}>
          {jobStatus?.repoName || jobId}
        </span>
        {isDone && report && (
          <span className="ml-auto font-mono font-bold text-sm px-3 py-1 rounded-full"
            style={{
              background: report.healthScore >= 80 ? '#14532d' : report.healthScore >= 60 ? '#431407' : '#7f1d1d',
              color: report.healthScore >= 80 ? '#86efac' : report.healthScore >= 60 ? '#fed7aa' : '#fca5a5',
            }}>
            Health: {report.healthScore}
          </span>
        )}
      </header>

      {/* Progress bar (while analyzing) */}
      {isAnalyzing && (
        <div className="px-6 py-6">
          <div className="max-w-2xl mx-auto">
            <div className="flex items-center justify-between mb-3">
              <span className="font-mono text-sm" style={{ color: 'var(--text-secondary)' }}>
                {currentStage}
              </span>
              <span className="font-mono text-sm" style={{ color: 'var(--accent-light)' }}>
                {progress}%
              </span>
            </div>
            <div className="w-full h-2 rounded-full mb-6" style={{ background: 'var(--bg-elevated)' }}>
              <div className="h-2 rounded-full shimmer transition-all duration-700"
                style={{ width: `${progress}%` }} />
            </div>
            {/* Stage indicators */}
            <div className="flex items-center gap-0">
              {STAGES.map((stage, i) => {
                const done = stageIndex > i
                const active = stageIndex === i
                return (
                  <div key={stage.label} className="flex items-center flex-1 last:flex-none">
                    <div className="flex flex-col items-center">
                      <div className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-mono font-bold"
                        style={{
                          background: done ? 'var(--accent)' : active ? 'var(--bg-elevated)' : 'var(--bg-elevated)',
                          border: `2px solid ${done ? 'var(--accent)' : active ? 'var(--accent)' : 'var(--border)'}`,
                          color: done ? 'white' : active ? 'var(--accent)' : 'var(--text-muted)',
                        }}>
                        {done ? '✓' : i + 1}
                      </div>
                      <span className="text-xs mt-1 font-mono"
                        style={{ color: active ? 'var(--accent-light)' : 'var(--text-muted)' }}>
                        {stage.label}
                      </span>
                    </div>
                    {i < STAGES.length - 1 && (
                      <div className="flex-1 h-0.5 mx-1 mb-4"
                        style={{ background: done ? 'var(--accent)' : 'var(--border)' }} />
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* Failed state */}
      {isFailed && (
        <div className="px-6 py-12 text-center">
          <div className="text-4xl mb-4">❌</div>
          <h2 className="font-mono font-bold text-xl mb-2" style={{ color: 'var(--danger)' }}>
            Analysis Failed
          </h2>
          <p className="text-sm mb-6" style={{ color: 'var(--text-muted)' }}>
            {jobStatus?.errorMessage || 'An unknown error occurred'}
          </p>
          <Link to="/" className="font-mono text-sm px-4 py-2 rounded-lg"
            style={{ background: 'var(--accent)', color: 'white' }}>
            Try Another Repository
          </Link>
        </div>
      )}

      {/* Dashboard (after done) */}
      {isDone && (
        <>
          {/* Tabs */}
          <div className="flex gap-0 border-b px-6" style={{ borderColor: 'var(--border)' }}>
            {TABS.map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                className="flex items-center gap-2 px-4 py-3 text-sm font-mono relative transition-colors"
                style={{ color: activeTab === tab.id ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                <span>{tab.icon}</span>
                <span>{tab.label}</span>
                {activeTab === tab.id && (
                  <div className="absolute bottom-0 left-0 right-0 h-0.5"
                    style={{ background: 'var(--accent)' }} />
                )}
              </button>
            ))}
          </div>

          {/* Tab content */}
          <div className="flex-1 p-6 overflow-auto">
            <div className="animate-fade-in">
              {activeTab === 'overview' && <OverviewTab jobId={jobId} report={report} />}
              {activeTab === 'complexity' && <ComplexityTab jobId={jobId} />}
              {activeTab === 'deadcode' && <DeadCodeTab jobId={jobId} />}
              {activeTab === 'contributors' && <ContributorsTab jobId={jobId} report={report} />}
              {activeTab === 'dependencies' && <DependenciesTab jobId={jobId} />}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
