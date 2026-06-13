import React, { useState } from 'react'
import { Budget } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'
import { CATEGORIES } from '../../shared/constants'

interface BudgetEditorProps {
  budgets: Budget[]
}

const BudgetEditor: React.FC<BudgetEditorProps> = ({ budgets }) => {
  const budgetMap = budgets.reduce((acc, b) => {
    acc[b.category] = b.amount
    return acc
  }, {} as Record<string, string>)

  const [localBudgets, setLocalBudgets] = useState<Record<string, string>>(budgetMap)

  React.useEffect(() => {
    setLocalBudgets(
      budgets.reduce((acc, b) => {
        acc[b.category] = b.amount
        return acc
      }, {} as Record<string, string>)
    )
  }, [budgets])

  const handleAmountChange = (category: string, value: string) => {
    setLocalBudgets(prev => ({ ...prev, [category]: value }))
  }

  const handleSave = (category: string) => {
    const amount = localBudgets[category]
    if (!amount || parseFloat(amount) < 0) {
      alert('请输入有效金额')
      return
    }
    eventBus.emit('budget:changed', { category, amount })
  }

  const handleRemove = (category: string) => {
    if (confirm(`确定要移除「${category}」的预算吗？`)) {
      eventBus.emit('budget:removed', category)
      setLocalBudgets(prev => {
        const next = { ...prev }
        delete next[category]
        return next
      })
    }
  }

  return (
    <div className="card">
      <h3 className="card-title">预算设置</h3>
      <div className="budget-list">
        {CATEGORIES.map(category => {
          const currentAmount = localBudgets[category] || ''
          const savedAmount = budgetMap[category] || '0'
          const hasChanges = currentAmount !== savedAmount

          return (
            <div key={category} className="budget-item">
              <span className="budget-category">{category}</span>
              <div className="budget-input-group">
                <span className="budget-prefix">¥</span>
                <input
                  type="number"
                  value={currentAmount}
                  onChange={(e) => handleAmountChange(category, e.target.value)}
                  placeholder="0"
                  step="100"
                  min="0"
                  className="budget-input"
                />
              </div>
              <div className="budget-actions">
                <button
                  type="button"
                  className="btn btn-small btn-primary"
                  onClick={() => handleSave(category)}
                  disabled={!hasChanges || parseFloat(currentAmount) < 0}
                >
                  保存
                </button>
                {parseFloat(savedAmount) > 0 && (
                  <button
                    type="button"
                    className="btn btn-small btn-danger"
                    onClick={() => handleRemove(category)}
                  >
                    移除
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default BudgetEditor
