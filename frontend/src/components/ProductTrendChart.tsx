import { useEffect, useRef, useState } from 'react'
import * as echarts from 'echarts'
import './ProductTrendChart.css'

interface TrendPoint {
  time: string
  count: number
}

interface ProductTrendResponse {
  product: string
  trendData: TrendPoint[]
}

const TIME_RANGES = [
  { label: '近1小时', value: 1 },
  { label: '近6小时', value: 6 },
  { label: '近24小时', value: 24 },
  { label: '近7天', value: 168 },
] as const

function ProductTrendChart({ product }: { product: string }) {
  const chartRef = useRef<HTMLDivElement>(null)
  const chartInstance = useRef<echarts.ECharts | null>(null)
  const [loading, setLoading] = useState(false)
  const [trendData, setTrendData] = useState<ProductTrendResponse | null>(null)
  const [hours, setHours] = useState(24)

  const fetchTrendData = async () => {
    if (!product) return
    setLoading(true)
    try {
      const response = await fetch(
        `/api/stats/trend?product=${encodeURIComponent(product)}&hours=${hours}`
      )
      const data: ProductTrendResponse = await response.json()
      setTrendData(data)
    } catch (error) {
      console.error('获取趋势数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchTrendData()
  }, [product, hours])

  // 初始化 echarts
  useEffect(() => {
    if (!chartRef.current) return

    chartInstance.current = echarts.init(chartRef.current)

    return () => {
      chartInstance.current?.dispose()
      chartInstance.current = null
    }
  }, [product])

  // 数据变化时更新图表
  useEffect(() => {
    if (!trendData || !chartInstance.current) return
    if (trendData.trendData.length === 0) return

    const counts = trendData.trendData.map(point => point.count)
    const minCount = Math.min(...counts)
    const maxCount = Math.max(...counts)

    const option = {
      title: {
        text: `${product} - SN数量趋势`,
        left: 'center',
        textStyle: { fontSize: 16 }
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const point = params[0]
          const fullTime = trendData.trendData[point.dataIndex].time
          return `${fullTime}<br/>SN数量: ${point.value}`
        }
      },
      toolbox: {
        feature: {
          dataZoom: { yAxisIndex: 'none' },
          restore: {},
          saveAsImage: {}
        }
      },
      xAxis: {
        type: 'category',
        data: trendData.trendData.map(p => p.time),
        axisLabel: {
          rotate: 30,
          fontSize: 11
        }
      },
      yAxis: {
        type: 'value',
        name: 'SN数量',
        min: Math.max(0, Math.floor(minCount * 0.95)),
        max: Math.ceil(maxCount * 1.05)
      },
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100 }
      ],
      series: [{
        type: 'line',
        data: counts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#1890ff' },
        itemStyle: { color: '#1890ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.05)' }
          ])
        }
      }],
      grid: {
        left: '10%',
        right: '5%',
        bottom: '15%',
        top: '15%'
      }
    }

    chartInstance.current.setOption(option, true)
  }, [trendData])

  useEffect(() => {
    const handleResize = () => chartInstance.current?.resize()
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return (
    <div className="trend-chart-section">
      <div className="trend-header">
        <h3>{product} 历史趋势</h3>
        <div className="time-range-selector">
          {TIME_RANGES.map(range => (
            <button
              key={range.value}
              className={`range-btn ${hours === range.value ? 'active' : ''}`}
              onClick={() => setHours(range.value)}
            >
              {range.label}
            </button>
          ))}
        </div>
      </div>
      <div className="chart-wrapper">
        {/* chart div 始终存在，不受 loading 影响 */}
        <div ref={chartRef} className="chart-container" />

        {/* loading 用遮罩层覆盖，不删除 chart div */}
        {loading && (
          <div className="chart-loading-overlay">
            <span>加载中...</span>
          </div>
        )}

        {/* 无数据遮罩 */}
        {!loading && trendData && trendData.trendData.length === 0 && (
          <div className="chart-loading-overlay">
            <span>该时间段暂无数据</span>
          </div>
        )}
      </div>
    </div>
  )
}

export default ProductTrendChart
