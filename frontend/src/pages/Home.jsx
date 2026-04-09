import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { startAnalysis } from '../api/client'

const EXAMPLE_REPOS = [
  { name: 'spring-petclinic', url: 'https://github.com/spring-projects/spring-petclinic' },
  { name: 'java-design-patterns', url: 'https://github.com/iluwatar/java-design-patterns' },
  { name: 'TheAlgorithms/Java', url: 'https://github.com/TheAlgorithms/Java' },
]

const FEATURES = [
  { icon: '◈', label: 'Cyclomatic Complexity', desc: 'Per-method CC scores with risk classification', color: 'var(--accent)' },
  { icon: '◎', label: 'Dead Code Detection', desc: 'Unreachable classes and methods via call-graph', color: 'var(--red)' },
  { icon: '▣', label: 'Hotspot Analysis', desc: 'Churn × complexity matrix from git history', color: 'var(--orange)' },
  { icon: '◉', label: 'Bus Factor', desc: 'Knowledge concentration risk index', color: 'var(--yellow)' },
  { icon: '⬡', label: 'Dependency Graph', desc: 'Maven & Gradle library visualization', color: 'var(--purple)' },
  { icon: '◆', label: 'Health Score', desc: 'Composite quality index across all dimensions', color: 'var(--green)' },
]

export default function Home() {
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleAnalyze = async () => {
    if (!url.trim()) { setError('Enter a repository URL'); return }
    if (!url.startsWith('http')) { setError('Must be a valid HTTPS URL'); return }
    setError(''); setLoading(true)
    try {
      const { jobId } = await startAnalysis(url.trim())
      navigate(`/report/${jobId}`)
    } catch (e) {
      setError(e.response?.data?.error || 'Failed to start analysis')
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-base)', position: 'relative', overflowX: 'hidden' }}>

      {/* Ambient background */}
      <div style={{
        position: 'fixed', top: 0, left: '50%', transform: 'translateX(-50%)',
        width: 800, height: 800, borderRadius: '50%', pointerEvents: 'none', zIndex: 0,
        background: 'radial-gradient(ellipse at 50% 0%, rgba(59,130,246,0.06) 0%, transparent 70%)',
      }} />
      <div style={{
        position: 'fixed', bottom: 0, left: 0, width: 500, height: 500, pointerEvents: 'none', zIndex: 0,
        background: 'radial-gradient(ellipse at 0% 100%, rgba(139,92,246,0.04) 0%, transparent 70%)',
      }} />

      {/* Nav */}
      <nav style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '20px 40px', borderBottom: '1px solid var(--border)',
        position: 'relative', zIndex: 10,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 28, height: 28, borderRadius: 7, background: 'var(--accent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 14, fontWeight: 800, color: 'white',
          }}>R</div>
          <span style={{ fontFamily: 'IBM Plex Mono', fontWeight: 700, fontSize: 15, letterSpacing: '-0.02em' }}>
            repo<span style={{ color: 'var(--accent)' }}>intel</span>
          </span>
        </div>
        <Link to="/history" style={{
          fontFamily: 'IBM Plex Mono', fontSize: 12, color: 'var(--text-muted)',
          textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 6,
          padding: '6px 12px', borderRadius: 8, border: '1px solid var(--border)',
          transition: 'all 0.15s',
        }}
          onMouseEnter={e => { e.target.style.color = 'var(--text-primary)'; e.target.style.borderColor = 'var(--border-light)' }}
          onMouseLeave={e => { e.target.style.color = 'var(--text-muted)'; e.target.style.borderColor = 'var(--border)' }}
        >
          History ↗
        </Link>
      </nav>

      {/* Hero */}
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', padding: '100px 24px 80px', position: 'relative', zIndex: 1,
      }}>
        {/* Status pill */}
        <div className="animate-fade-up" style={{
          display: 'inline-flex', alignItems: 'center', gap: 8,
          padding: '5px 14px', borderRadius: 99, marginBottom: 40,
          border: '1px solid rgba(59,130,246,0.3)',
          background: 'rgba(59,130,246,0.07)',
          fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'rgba(147,197,253,0.9)',
        }}>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--accent)', display: 'block', animation: 'pulse-glow 2s ease infinite' }} />
          Java · Spring Boot · JGit · JavaParser
        </div>

        {/* Headline */}
        <h1 className="animate-fade-up" style={{
          fontFamily: 'Syne', fontWeight: 800, fontSize: 'clamp(48px, 8vw, 80px)',
          lineHeight: 0.92, letterSpacing: '-0.04em', textAlign: 'center', marginBottom: 24,
          animationDelay: '0.05s',
        }}>
          <span style={{ color: 'var(--text-primary)' }}>Repository</span>
          <br />
          <span style={{
            backgroundImage: 'linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%)',
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>Intelligence</span>
        </h1>

        <p className="animate-fade-up" style={{
          fontFamily: 'Syne', fontSize: 17, lineHeight: 1.6, color: 'var(--text-secondary)',
          textAlign: 'center', maxWidth: 520, marginBottom: 48,
          animationDelay: '0.10s',
        }}>
          Deep static analysis of Java codebases. Surface complexity, dead code, hotspots, and bus factor — shipped in one dashboard.
        </p>

        {/* Input card */}
        <div className="animate-fade-up card" style={{
          width: '100%', maxWidth: 580, padding: 20,
          border: '1px solid var(--border-md)',
          animationDelay: '0.15s',
        }}>
          <div style={{ fontSize: 11, fontFamily: 'IBM Plex Mono', color: 'var(--text-muted)', marginBottom: 10 }}>
            GITHUB URL
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="input"
              value={url}
              onChange={e => { setUrl(e.target.value); setError('') }}
              onKeyDown={e => e.key === 'Enter' && handleAnalyze()}
              placeholder="https://github.com/org/repository"
              style={{ borderColor: error ? 'var(--red)' : undefined }}
            />
            <button
              onClick={handleAnalyze}
              disabled={loading}
              className="btn btn-primary"
              style={{ minWidth: 110, fontSize: 12 }}
            >
              {loading ? (
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 12, height: 12, border: '2px solid rgba(255,255,255,0.3)', borderTopColor: 'white', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                  Starting
                </span>
              ) : 'Analyze →'}
            </button>
          </div>

          {error && (
            <p style={{ fontFamily: 'IBM Plex Mono', fontSize: 11, color: 'var(--red)', marginTop: 8 }}>
              ⚠ {error}
            </p>
          )}

          {/* Example repos */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
            <span style={{ fontFamily: 'IBM Plex Mono', fontSize: 10, color: 'var(--text-muted)' }}>EXAMPLES:</span>
            {EXAMPLE_REPOS.map(r => (
              <button key={r.url} onClick={() => setUrl(r.url)}
                style={{
                  fontFamily: 'IBM Plex Mono', fontSize: 10, padding: '3px 10px', borderRadius: 6,
                  background: 'var(--bg-elevated)', color: 'var(--accent)', cursor: 'pointer',
                  border: '1px solid var(--border-md)', transition: 'all 0.12s',
                }}
                onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border-md)'}
              >
                {r.name}
              </button>
            ))}
          </div>
        </div>

        {/* Features */}
        <div className="animate-fade-up stagger" style={{
          display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12,
          maxWidth: 780, width: '100%', marginTop: 60,
          animationDelay: '0.20s',
        }}>
          {FEATURES.map((f, i) => (
            <div key={f.label} className="animate-fade-up card" style={{
              display: 'flex', flexDirection: 'column', gap: 8, cursor: 'default',
              animationDelay: `${0.2 + i * 0.06}s`,
              transition: 'border-color 0.2s, transform 0.2s',
            }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = f.color + '55'; e.currentTarget.style.transform = 'translateY(-2px)' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.transform = 'translateY(0)' }}
            >
              <div style={{ fontSize: 22, color: f.color, fontWeight: 400 }}>{f.icon}</div>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 13 }}>{f.label}</div>
              <div style={{ fontFamily: 'IBM Plex Mono', fontSize: 10.5, color: 'var(--text-muted)', lineHeight: 1.6 }}>{f.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
