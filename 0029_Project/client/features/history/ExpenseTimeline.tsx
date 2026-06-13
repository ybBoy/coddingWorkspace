import React from 'react'
import { Expense } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

interface ExpenseTimelineProps {
  recentExpenses: Expense[]
  monthExpenses: Expense[]
  currentMonth: string
}

const ExpenseTimeline: React.FC<ExpenseTimelineProps> = ({ recentExpenses, currentMonth }) => {
  const formatTime = (timeStr: string): string => {
    const date = new Date(timeStr)
    const month = date.getMonth() + 1
    const day = date.getDate()
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${month}月${day}日 ${hours}:${minutes}`
  }

  const handleDelete = (id: string, remark: string) => {
    if (confirm(`确定要删除「${remark || '无备注'}」这笔记录吗？`)) {
      eventBus.emit('expense:deleted', id)
    }
  }

  const handleEdit = (expense: Expense) => {
    eventBus.emit('editor:open', expense)
  }

  const getCategoryColor = (category: string): string => {
    const colors: Record<string, string> = {
      '餐饮': '#e74c3c',
      '购物': '#9b59b6',
      '交通': '#3498db',
      '娱乐': '#1abc9c',
      '医疗': '#e67e22',
      '教育': '#34495e',
      '住房': '#27ae60',
      '通讯': '#f39c12',
      '其他': '#7f8c8d'
    }
    return colors[category] || '#7f8c8d'
  }

  const groupedByDate = recentExpenses.reduce((groups, expense) => {
    const dateKey = expense.time.slice(0, 10)
    if (!groups[dateKey]) {
      groups[dateKey] = []
    }
    groups[dateKey].push(expense)
    return groups
  }, {} as Record<string, Expense[]>)

  const formatDateKey = (dateKey: string): string => {
    const date = new Date(dateKey)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)

    if (dateKey === today.toISOString().slice(0, 10)) return '今天'
    if (dateKey === yesterday.toISOString().slice(0, 10)) return '昨天'
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }

  return (
    <div className="card">
      <h3 className="card-title">
        最近记录
        <span className="card-subtitle">（最多20条 · {currentMonth}）</span>
      </h3>
      <div className="timeline">
        {recentExpenses.length === 0 ? (
          <div className="empty-state">暂无记录，开始记第一笔吧~</div>
        ) : (
          Object.entries(groupedByDate).map(([dateKey, dayExpenses]) => {
            const dayTotal = dayExpenses.reduce(
              (sum, e) => sum + parseFloat(e.amount),
              0
            )
            return (
              <div key={dateKey} className="timeline-day">
                <div className="timeline-date">
                  <span>{formatDateKey(dateKey)}</span>
                  <span className="timeline-day-total">支出 ¥{dayTotal.toFixed(2)}</span>
                </div>
                <div className="timeline-items">
                  {dayExpenses.map(expense => (
                    <div key={expense.id} className="timeline-item">
                      <div
                        className="timeline-dot"
                        style={{ backgroundColor: getCategoryColor(expense.category) }}
                      />
                      <div className="timeline-content">
                        <div className="timeline-header">
                          <span className="timeline-category">{expense.category}</span>
                          <span className="timeline-amount">¥{parseFloat(expense.amount).toFixed(2)}</span>
                        </div>
                        <div className="timeline-meta">
                          <span className="timeline-payer">{expense.payer}</span>
                          <span className="timeline-time">{formatTime(expense.time)}</span>
                          {expense.remark && (
                            <span className="timeline-remark">· {expense.remark}</span>
                          )}
                        </div>
                      </div>
                      <div className="timeline-actions">
                        <button
                          type="button"
                          className="btn-edit"
                          onClick={() => handleEdit(expense)}
                          title="编辑"
                        >
                          ✎
                        </button>
                        <button
                          type="button"
                          className="btn-delete"
                          onClick={() => handleDelete(expense.id, expense.remark)}
                          title="删除"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}

export default ExpenseTimeline
