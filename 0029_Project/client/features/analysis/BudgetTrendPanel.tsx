import React, { useState, useEffect } from 'react'
import { BudgetTrendMonth } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

const BudgetTrendPanel: React.FC = () => {
  const [trend, setTrend] = useState<BudgetTrendMonth[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('')

  useEffect(() => {
    const unsub = eventBus.on('trend:result', (data: BudgetTrendMonth[]) => {
      setTrend(data)
    })
    return () => { unsub() }
  }, [])

  const allCategories = trend.length > 0
    ? [...new Set(trend.flatMap(m => m.categories.map(c => c.category)))]
    : []

  useEffect(() => {
    if (allCategories.length > 0 && !selectedCategory) {
      setSelectedCategory(allCategories[0])
    }
  }, [allCategories.length])

  const filteredTrend = trend.map(m => ({
    ...m,
    categories: m.categories.filter(c => c.category === selectedCategory)
  })).filter(m => m.categories.length > 0)

  const maxRatio = Math.max(
    100,
    ...filteredTrend.flatMap(m => m.categories.map(c => c.ratio))
  )

  const overCount = (cat: string): number => {
    return trend.filter(m => m.categories.some(c => c.category === cat && c.ratio >= 100)).length
  }

  if (trend.length === 0) {
    return (
      <div className="modal-overlay" onClick={() => eventBus.emit('trend:close', undefined)}>
        <div className="modal modal-large" onClick={e => e.stopPropagation()}>
          <div className="modal-header">
            <h3>📈 预算历史趋势</h3>
            <button type="button" className="btn-close" onClick={() => eventBus.emit('trend:close', undefined)}>×</button>
          </div>
          <div className="modal-body">
            <div className="empty-state">加载中...</div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="modal-overlay" onClick={() => eventBus.emit('trend:close', undefined)}>
      <div className="modal modal-large" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>📈 预算历史趋势</h3>
          <button type="button" className="btn-close" onClick={() => eventBus.emit('trend:close', undefined)}>×</button>
        </div>
        <div className="modal-body">
          <div className="trend-category-tabs">
            {allCategories.map(cat => {
              const oc = overCount(cat)
              return (
                <button
                  key={cat}
                  type="button"
                  className={`trend-tab ${cat === selectedCategory ? 'active' : ''} ${oc > 0 ? 'has-over' : ''}`}
                  onClick={() => setSelectedCategory(cat)}
                >
                  {cat}
                  {oc > 0 && <span className="trend-over-badge">{oc}次超支</span>}
                </button>
              )
            })}
          </div>

          {selectedCategory && (
            <div className="trend-chart">
              <div className="trend-bars">
                {filteredTrend.map(m => {
                  const catData = m.categories[0]
                  if (!catData) return null
                  const barWidth = Math.min((catData.ratio / maxRatio) * 100, 100)
                  const isOver = catData.ratio >= 100
                  const isWarning = catData.ratio >= 80 && catData.ratio < 100
                  return (
                    <div key={`${m.year}-${m.month}`} className="trend-bar-group">
                      <div className="trend-bar-label">{m.month}月</div>
                      <div className="trend-bar-track">
                        <div
                          className={`trend-bar-fill ${isOver ? 'over' : isWarning ? 'warning' : ''}`}
                          style={{ width: `${barWidth}%` }}
                        />
                        <div
                          className="trend-bar-budget-line"
                          style={{ left: `${Math.min((100 / maxRatio) * 100, 100)}%` }}
                        />
                      </div>
                      <div className={`trend-bar-value ${isOver ? 'over' : isWarning ? 'warning' : ''}`}>
                        {catData.ratio.toFixed(0)}%
                      </div>
                      <div className="trend-bar-detail">
                        ¥{parseFloat(catData.spent).toFixed(0)} / ¥{parseFloat(catData.budget).toFixed(0)}
                      </div>
                    </div>
                  )
                })}
              </div>
              <div className="trend-legend">
                <span className="trend-legend-item">
                  <span className="trend-legend-dot normal" /> 正常
                </span>
                <span className="trend-legend-item">
                  <span className="trend-legend-dot warning" /> 接近预算
                </span>
                <span className="trend-legend-item">
                  <span className="trend-legend-dot over" /> 超支
                </span>
                <span className="trend-legend-item">
                  <span className="trend-legend-line" /> 预算线 (100%)
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default BudgetTrendPanel
