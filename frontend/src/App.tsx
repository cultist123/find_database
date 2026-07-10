import { useState, useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import './App.css'
import RecentHourStats from './components/RecentHourStats'
import ProductTrendChart from './components/ProductTrendChart'

interface CategoryStatsResponse {
  timestamp: string
  data: { [key: string]: number }
  totalRecords: number
}

function App() {
  const [stats, setStats] = useState<CategoryStatsResponse | null>(null)
  const [connectionStatus, setConnectionStatus] = useState<'连接中' | '已连接' | '已断开'>('连接中')
  const stompClient = useRef<Client | null>(null)
  const [selectedProduct, setSelectedProduct] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'recent' | 'product'>('recent')
  const [productExpanded, setProductExpanded] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    const connectWebSocket = () => {
      const client = new Client({
        webSocketFactory: () => new SockJS('/ws/stats'),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket已连接')
          setConnectionStatus('已连接')
          client.subscribe('/topic/stats', (message) => {
            const data: CategoryStatsResponse = JSON.parse(message.body)
            console.log('收到实时数据:', data)
            setStats(data)
          })
        },
        onDisconnect: () => {
          console.log('WebSocket已断开')
          setConnectionStatus('已断开')
        },
        onStompError: (frame) => {
          console.error('STOMP错误:', frame)
          setConnectionStatus('已断开')
        }
      })
      client.activate()
      stompClient.current = client
    }
    connectWebSocket()
    return () => {
      if (stompClient.current) {
        stompClient.current.deactivate()
      }
    }
  }, [])

  useEffect(() => {
    fetchStats()
  }, [])

  const fetchStats = async () => {
    try {
      const response = await fetch('/api/stats/category')
      const data = await response.json()
      setStats(data)
    } catch (error) {
      console.error('获取统计数据失败:', error)
    }
  }

  const refreshStats = async () => {
    try {
      const response = await fetch('/api/stats/refresh', {
        method: 'POST'
      })
      const data = await response.json()
      setStats(data)
    } catch (error) {
      console.error('刷新统计数据失败:', error)
    }
  }

  const getConnectionColor = () => {
    switch (connectionStatus) {
      case '已连接': return '#4caf50'
      case '连接中': return '#ff9800'
      case '已断开': return '#f44336'
    }
  }

  // 按SN数量排序所有product
  const sortedData = stats
    ? Object.entries(stats.data).sort((a, b) => b[1] - a[1])
    : []

  // 根据搜索词过滤
  const filteredData = sortedData.filter(([category]) =>
    category.toLowerCase().includes(searchQuery.toLowerCase().trim())
  )

  const visibleData = productExpanded ? filteredData : filteredData.slice(0, 8)
  const hasMoreProducts = filteredData.length > 8

  //点击某行切换选中的product
  const handleRowClick = (category: string) => {
    setSelectedProduct(category)
  }

  // 数据加载后默认选中第一个product
  useEffect(() => {
    if (sortedData.length > 0 && !selectedProduct) {
      setSelectedProduct(sortedData[0][0])
    }
  }, [stats])

  return (
    <div className="app">
      <header className="header">
        <h1>IOT链接设备数据统计系统</h1>
        <div className="status-bar">
          <span className="connection-status" style={{ color: getConnectionColor() }}>
            ● WebSocket {connectionStatus}
          </span>
          {stats && (
            <>
              <span className="total-records">
                总SN数量: <strong>{stats.totalRecords.toLocaleString()}</strong>
              </span>
              <span className="last-update">
                更新时间: {stats.timestamp}
              </span>
            </>
          )}
          <button onClick={refreshStats} className="refresh-btn">
            立即刷新
          </button>
        </div>
      </header>

      <main className="main">
        <div className="tab-bar">
          <button
            className={`tab-btn ${activeTab === 'recent' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('recent')}
          >
            最近一小时Product SN统计
          </button>
          <button
            className={`tab-btn ${activeTab === 'product' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('product')}
          >
            Product SN统计数据展示
          </button>
        </div>

        {activeTab === 'recent' && <RecentHourStats />}

        {activeTab === 'product' && (
          <div className="daily-stats-section">
            <h2>Product SN统计数据展示</h2>

            <div className="search-bar">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                placeholder="搜索 Product 名称..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="search-input"
              />
              {searchQuery && (
                <button
                  className="search-clear"
                  onClick={() => setSearchQuery('')}
                >
                  ✕
                </button>
              )}
            </div>

            {filteredData.length > 0 && (
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Product SN分类</th>
                      <th>SN数量</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleData.map(([category, count]) => (
                      <tr
                        key={category}
                        onClick={() => handleRowClick(category)}
                        className={`clickable-row ${selectedProduct === category ? 'selected-row' : ''}`}
                      >
                        <td>{category}</td>
                        <td><strong>{count.toLocaleString()}</strong></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {hasMoreProducts && (
                  <div className="expand-bar" onClick={() => setProductExpanded(!productExpanded)}>
                    {productExpanded ? '收起' : `展开更多（共 ${filteredData.length} 条）`}
                  </div>
                )}
              </div>
            )}

            {filteredData.length === 0 && searchQuery && (
              <div className="no-results">
                未找到匹配的 Product
              </div>
            )}

            {/* 趋势图常驻在页面底部 */}
            {selectedProduct && (
              <div className="trend-section">
                <ProductTrendChart key={selectedProduct} product={selectedProduct} />
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}

export default App