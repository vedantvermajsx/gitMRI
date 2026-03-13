import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import Report from './pages/Report'
import History from './pages/History'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/report/:jobId" element={<Report />} />
      <Route path="/history" element={<History />} />
    </Routes>
  )
}
