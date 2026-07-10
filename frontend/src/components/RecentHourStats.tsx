import { useState, useEffect } from 'react'
import './RecentHourStats.css'

interface RecentHourStatsResponse {
  timestamp: string
  timeRange: string
  categorySnCount: { [key: string]: number }
  totalSnCount: number
}

function RecentHourStats() {
  const [stats, setStats] = useState<RecentHourStatsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [expanded, setExpanded] = useState(false)

  const fetchRecentHourStats = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/stats/recent-hour')
      const data: RecentHourStatsResponse = await response.json()
      setStats(data)
    } catch (error) {
      console.error('获取最近一小时统计数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRecentHourStats()

    const interval = setInterval(() => {
      fetchRecentHourStats()
    }, 60000)

    return () => clearInterval(interval)
  }, [])

  const sortedEntries = stats
    ? Object.entries(stats.categorySnCount).sort((a, b) => b[1] - a[1])
    : []

  const visibleEntries = expanded ? sortedEntries : sortedEntries.slice(0, 8)
  const hasMore = sortedEntries.length > 8

  return (
    <div className="recent-hour-stats">
      <div className="section-header">
        <h2>最近一小时Product SN统计</h2>
        <button
          onClick={fetchRecentHourStats}
          disabled={loading}
          className="refresh-btn"
        >
          {loading ? '刷新中...' : '立即刷新'}
        </button>
      </div>

      {stats && (
        <>
          <div className="stats-info">
            <span className="time-range">
              统计时间范围: <strong>{stats.timeRange}</strong>
            </span>
            <span className="update-time">
              更新时间: {stats.timestamp}
            </span>
            <span className="total-count">
              总SN数量: <strong>{stats.totalSnCount}</strong>
            </span>
          </div>

          {sortedEntries.length > 0 && (
            <div className="data-table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>分类名称</th>
                    <th>SN数量</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleEntries.map(([category, count]) => (
                    <tr key={category}>
                      <td>{category}</td>
                      <td><strong>{count.toLocaleString()}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {hasMore && (
                <div className="expand-bar" onClick={() => setExpanded(!expanded)}>
                  {expanded ? '收起' : `展开更多（共 ${sortedEntries.length} 条）`}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default RecentHourStats