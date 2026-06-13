import React from 'react'
import { CategoryStat } from '../../shared/types'
import { BUDGET_WARNING_THRESHOLD, BUDGET_OVER_THRESHOLD } from '../../shared/constants'

interface SummaryPanelProps {
  total: string
  categoryStats: CategoryStat[]
}

const SummaryPanel: React.FC<SummaryPanelProps> = ({ total, categoryStats }) => {
  const getProgressColor = (spent: number, budget: number): string => {
    if (budget <= 0) return 'var(--primary)'
    const ratio = spent / budget
    if (ratio >= BUDGET_OVER_THRESHOLD) return 'var(--danger)'
    if (ratio >= BUDGET_WARNING_THRESHOLD) return 'var(--warning)'
    return 'var(--primary)'
  }

  const getProgressWidth = (spent: number, budget: number): number => {
    if (budget <= 0) return 100
    const ratio = spent / budget
    return Math.min(ratio * 100, 100)
  }

  const getStatusText = (spent: number, budget: number): string => {
    if (budget <= 0) return ''
    const remaining = budget - spent
    const ratio = spent / budget
    if (ratio >= BUDGET_OVER_THRESHOLD) return `超预算 ¥${Math.abs(remaining).toFixed(2)}`
    if (ratio >= BUDGET_WARNING_THRESHOLD) return `剩余 ¥${remaining.toFixed(2)}（接近预算）`
    return `剩余 ¥${remaining.toFixed(2)}`
  }

  const totalNum = parseFloat(total) || 0

  return (
    <div className="card">
      <h3 className="card-title">本月统计</h3>

      <div className="summary-total">
        <div className="summary-total-label">总支出</div>
        <div className="summary-total-value">¥{totalNum.toFixed(2)}</div>
      </div>

      <div className="category-list">
        {categoryStats.length === 0 ? (
          <div className="empty-state">暂无支出记录</div>
        ) : (
          categoryStats.map((stat) => {
            const spent = parseFloat(stat.spent) || 0
            const budget = parseFloat(stat.budget) || 0
            const width = getProgressWidth(spent, budget)
            const color = getProgressColor(spent, budget)
            const status = getStatusText(spent, budget)
            const ratio = budget > 0 ? (spent / budget) * 100 : 0

            return (
              <div key={stat.category} className="category-item">
                <div className="category-header">
                  <span className="category-name">{stat.category}</span>
                  <span className="category-amount">¥{spent.toFixed(2)}</span>
                </div>
                {budget > 0 && (
                  <>
                    <div className="progress-bar">
                      <div
                        className="progress-fill"
                        style={{ width: `${width}%`, backgroundColor: color }}
                      />
                    </div>
                    <div className="category-footer">
                      <span className="category-budget">预算 ¥{budget.toFixed(2)}</span>
                      <span
                        className="category-status"
                        style={{ color }}
                      >
                        {status} ({ratio.toFixed(0)}%)
                      </span>
                    </div>
                  </>
                )}
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}

export default SummaryPanel
