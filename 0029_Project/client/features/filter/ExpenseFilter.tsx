import React, { useState } from 'react'
import { Expense } from '../../shared/types'

interface ExpenseFilterProps {
  expenses: Expense[]
  categories: string[]
  payers: string[]
  onFilter: (filtered: Expense[]) => void
}

const ExpenseFilter: React.FC<ExpenseFilterProps> = ({ expenses, categories, payers, onFilter }) => {
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [payer, setPayer] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [expanded, setExpanded] = useState(false)

  const applyFilter = () => {
    let result = [...expenses]
    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase()
      result = result.filter(e => e.remark.toLowerCase().includes(kw) || e.category.toLowerCase().includes(kw))
    }
    if (category) {
      result = result.filter(e => e.category === category)
    }
    if (payer) {
      result = result.filter(e => e.payer === payer)
    }
    if (minAmount && parseFloat(minAmount) > 0) {
      result = result.filter(e => parseFloat(e.amount) >= parseFloat(minAmount))
    }
    if (maxAmount && parseFloat(maxAmount) > 0) {
      result = result.filter(e => parseFloat(e.amount) <= parseFloat(maxAmount))
    }
    onFilter(result)
  }

  const clearFilter = () => {
    setKeyword('')
    setCategory('')
    setPayer('')
    setMinAmount('')
    setMaxAmount('')
    onFilter(expenses)
  }

  const hasFilter = keyword || category || payer || minAmount || maxAmount

  return (
    <div className="filter-bar">
      <div className="filter-row">
        <input
          type="text"
          value={keyword}
          onChange={e => { setKeyword(e.target.value); setTimeout(applyFilter, 0) }}
          placeholder="🔍 搜索备注/分类..."
          className="filter-input"
        />
        <button
          type="button"
          className={`filter-toggle ${expanded ? 'active' : ''}`}
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? '收起' : '筛选'}
        </button>
        {hasFilter && (
          <button type="button" className="filter-clear" onClick={clearFilter}>
            清除
          </button>
        )}
      </div>
      {expanded && (
        <div className="filter-advanced">
          <select value={category} onChange={e => setCategory(e.target.value)} className="filter-select">
            <option value="">全部分类</option>
            {categories.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
          <select value={payer} onChange={e => setPayer(e.target.value)} className="filter-select">
            <option value="">全部付款人</option>
            {payers.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
          <div className="filter-amount-range">
            <input
              type="number"
              value={minAmount}
              onChange={e => setMinAmount(e.target.value)}
              placeholder="最低金额"
              min="0"
              step="0.01"
              className="filter-amount-input"
            />
            <span className="filter-amount-sep">~</span>
            <input
              type="number"
              value={maxAmount}
              onChange={e => setMaxAmount(e.target.value)}
              placeholder="最高金额"
              min="0"
              step="0.01"
              className="filter-amount-input"
            />
          </div>
          <button type="button" className="btn btn-small btn-primary" onClick={applyFilter}>
            应用筛选
          </button>
        </div>
      )}
    </div>
  )
}

export default ExpenseFilter
