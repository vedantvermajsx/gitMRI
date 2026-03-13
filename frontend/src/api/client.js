import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

export const startAnalysis = (repoUrl) =>
  api.post('/analyze', { repoUrl }).then(r => r.data)

export const getJobStatus = (jobId) =>
  api.get(`/jobs/${jobId}/status`).then(r => r.data)

export const listJobs = () =>
  api.get('/jobs').then(r => r.data)

export const deleteJob = (jobId) =>
  api.delete(`/jobs/${jobId}`)

export const getReport = (jobId) =>
  api.get(`/reports/${jobId}`).then(r => r.data)

export const getComplexity = (jobId) =>
  api.get(`/reports/${jobId}/complexity`).then(r => r.data)

export const getDeadCode = (jobId, risk) =>
  api.get(`/reports/${jobId}/deadcode`, { params: risk ? { risk } : {} }).then(r => r.data)

export const getHotspots = (jobId) =>
  api.get(`/reports/${jobId}/hotspots`).then(r => r.data)

export const getContributors = (jobId) =>
  api.get(`/reports/${jobId}/contributors`).then(r => r.data)

export const getDependencies = (jobId) =>
  api.get(`/reports/${jobId}/dependencies`).then(r => r.data)

export default api
