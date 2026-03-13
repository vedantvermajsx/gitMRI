import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listJobs, deleteJob } from '../api/client'

const statusColor = {
  DONE: { bg: '#14532d', text: '#86efac' },
  FAILED: { bg: '#7f1d1d', text: '#fca5a5' },
  PENDING: { bg: '#1e1b4b', text: '#a5b4fc' },
  CLONING: { bg: '#1e1b4b', text: '#a5b4fc' },
  PARSING: { bg: '#1e1b4b', text: '#a5b4fc' },
  ANALYZING: { bg: '#1e1b4b', text: '#a5b4fc' },
  HOTSPOTS: { bg: '#1e1b4b', text: '#a5b4fc' },
  DEPENDENCIES: { bg: '#1e1b4b', text: '#a5b4fc' },
  REPORTING: { bg: '#1e1b4b', text: '#a5b4fc' },
}

export default function History() {
  const qc = useQueryClient()
  const { data: jobs = [], isLoading } = useQuery({
    queryKey: ['jobs'],
    queryFn: listJobs,
  })

  const { mutate: remove } = useMutation({
    mutationFn: deleteJob,
    onSuccess: () => qc.invalidateQueries(['jobs']),
  })

  return (
    <div className="min-h-screen" style={{ background: 'var(--bg-base)' }}>
      <header className="flex items-center justify-between px-6 py-4 border-b" style={{ borderColor: 'var(--border)' }}>
        <Link to="/" className="font-mono font-black text-xl">
          REPO<span style={{ color: 'var(--accent)' }}>INTEL</span>
        </Link>
        <Link to="/" className="font-mono text-sm px-4 py-2 rounded-lg"
          style={{ background: 'var(--accent)', color: 'white' }}>
          + New Analysis
        </Link>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-10">
        <h1 className="font-mono font-black text-2xl mb-8">Past Analyses</h1>

        {isLoading ? (
          <div className="text-center py-20" style={{ color: 'var(--text-muted)' }}>Loading...</div>
        ) : jobs.length === 0 ? (
          <div className="text-center py-20">
            <div className="text-4xl mb-4">📂</div>
            <p style={{ color: 'var(--text-muted)' }}>No analyses yet.</p>
            <Link to="/" className="font-mono text-sm mt-4 inline-block" style={{ color: 'var(--accent)' }}>
              Start your first →
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {jobs.map(job => {
              const colors = statusColor[job.status] || statusColor.PENDING
              return (
                <div key={job.jobId} className="card flex items-center gap-4 hover:border-indigo-600 transition-colors">
                  <div className="flex-1 min-w-0">
                    <div className="font-mono font-bold text-sm mb-1">{job.repoName}</div>
                    <div className="text-xs truncate" style={{ color: 'var(--text-muted)' }}>{job.repoUrl}</div>
                  </div>
                  <span className="text-xs font-mono px-2 py-1 rounded-full flex-shrink-0"
                    style={{ background: colors.bg, color: colors.text }}>
                    {job.status}
                  </span>
                  <span className="text-xs flex-shrink-0" style={{ color: 'var(--text-muted)' }}>
                    {job.createdAt ? new Date(job.createdAt).toLocaleDateString() : ''}
                  </span>
                  {job.status === 'DONE' ? (
                    <Link to={`/report/${job.jobId}`}
                      className="font-mono text-xs px-3 py-1.5 rounded-lg flex-shrink-0"
                      style={{ background: 'var(--bg-elevated)', color: 'var(--accent-light)', border: '1px solid var(--border)' }}>
                      View →
                    </Link>
                  ) : job.status !== 'FAILED' ? (
                    <Link to={`/report/${job.jobId}`}
                      className="font-mono text-xs px-3 py-1.5 rounded-lg flex-shrink-0"
                      style={{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: '1px solid var(--border)' }}>
                      Watch
                    </Link>
                  ) : null}
                  <button onClick={() => remove(job.jobId)}
                    className="text-xs px-2 py-1 rounded flex-shrink-0 transition-colors"
                    style={{ color: 'var(--text-muted)' }}>
                    ✕
                  </button>
                </div>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}
