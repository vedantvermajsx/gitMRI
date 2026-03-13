import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { startAnalysis } from '../api/client'

const EXAMPLE_REPOS = [
  'https://github.com/spring-projects/spring-petclinic',
  'https://github.com/iluwatar/java-design-patterns',
  'https://github.com/TheAlgorithms/Java',
]

const FEATURES = [
  { icon: '⚡', label: 'Cyclomatic Complexity', desc: 'Per-method CC scores and risk levels' },
  { icon: '💀', label: 'Dead Code Detection', desc: 'Unreachable classes and methods' },
  { icon: '🔥', label: 'Hotspot Analysis', desc: 'Churn × complexity heatmap' },
  { icon: '👥', label: 'Bus Factor', desc: 'Knowledge concentration risk' },
  { icon: '🕸', label: 'Dependency Graph', desc: 'External library visualization' },
  { icon: '📊', label: 'Health Score', desc: 'Composite repository quality index' },
]

export default function Home() {
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleAnalyze = async () => {
    if (!url.trim()) { setError('Please enter a repository URL'); return }
    if (!url.startsWith('http')) { setError('Please enter a valid HTTPS URL'); return }
    setError('')
    setLoading(true)
    try {
      const { jobId } = await startAnalysis(url.trim())
      navigate(`/report/${jobId}`)
    } catch (e) {
      setError(e.response?.data?.error || 'Failed to start analysis')
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--bg-base)' }}>
      {/* Nav */}
      <nav className="flex items-center justify-between px-8 py-5 border-b" style={{ borderColor: 'var(--border)' }}>
        <span className="font-mono font-black text-xl tracking-tight">
          REPO<span style={{ color: 'var(--accent)' }}>INTEL</span>
        </span>
        <Link to="/history" className="text-sm font-mono" style={{ color: 'var(--text-muted)' }}>
          Past Analyses →
        </Link>
      </nav>

      {/* Hero */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-20">
        <div className="max-w-2xl w-full text-center animate-fade-in">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono mb-8"
            style={{ background: '#1a1040', border: '1px solid #3730a3', color: '#a5b4fc' }}>
            <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-pulse" />
            Java · Spring Boot · JGit · JavaParser
          </div>

          <h1 className="font-mono font-black text-6xl leading-none mb-4" style={{ letterSpacing: '-2px' }}>
            Repository
            <br />
            <span style={{ color: 'var(--accent)' }}>Intelligence</span>
          </h1>

          <p className="text-lg mb-12" style={{ color: 'var(--text-secondary)' }}>
            Deep static analysis of Java codebases. Complexity metrics, dead code detection,
            git hotspots, bus factor and dependency graphs — in one dashboard.
          </p>

          {/* Input */}
          <div className="flex gap-2 mb-3">
            <input
              value={url}
              onChange={e => { setUrl(e.target.value); setError('') }}
              onKeyDown={e => e.key === 'Enter' && handleAnalyze()}
              placeholder="https://github.com/org/repository"
              className="flex-1 px-4 py-3 rounded-xl font-mono text-sm outline-none transition-all"
              style={{
                background: 'var(--bg-card)',
                border: `1px solid ${error ? 'var(--danger)' : 'var(--border-light)'}`,
                color: 'var(--text-primary)',
              }}
            />
            <button
              onClick={handleAnalyze}
              disabled={loading}
              className="px-6 py-3 rounded-xl font-mono font-bold text-sm uppercase tracking-widest transition-all"
              style={{
                background: loading ? '#374151' : 'var(--accent)',
                color: 'white',
                cursor: loading ? 'not-allowed' : 'pointer',
                minWidth: 120,
              }}
            >
              {loading ? '⏳ Starting...' : 'Analyze →'}
            </button>
          </div>

          {error && (
            <p className="text-sm mb-3 font-mono" style={{ color: 'var(--danger)' }}>{error}</p>
          )}

          {/* Example repos */}
          <div className="flex flex-wrap justify-center gap-2 mt-4">
            <span className="text-xs" style={{ color: 'var(--text-muted)' }}>Try:</span>
            {EXAMPLE_REPOS.map(repo => {
              const name = repo.split('/').slice(-1)[0]
              return (
                <button key={repo} onClick={() => setUrl(repo)}
                  className="text-xs px-3 py-1 rounded-full font-mono transition-colors"
                  style={{ background: 'var(--bg-elevated)', color: 'var(--accent-light)', border: '1px solid var(--border)' }}>
                  {name}
                </button>
              )
            })}
          </div>
        </div>

        {/* Features grid */}
        <div className="max-w-3xl w-full mt-24 grid grid-cols-3 gap-4">
          {FEATURES.map(f => (
            <div key={f.label} className="card hover:border-indigo-600 transition-colors">
              <div className="text-2xl mb-2">{f.icon}</div>
              <div className="font-mono font-bold text-sm mb-1">{f.label}</div>
              <div className="text-xs" style={{ color: 'var(--text-muted)' }}>{f.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
