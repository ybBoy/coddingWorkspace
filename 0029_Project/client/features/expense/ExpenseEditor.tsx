import React, { useState, useEffect } from 'react'
import { Expense, LedgerConfig } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

interface ExpenseEditorProps {
  expense: Expense
  config: LedgerConfig
}

const ExpenseEditor: React.FC<ExpenseEditorProps> = ({ expense, config }) => {
  const [amount, setAmount] = useState(expense.amount)
  const [category, setCategory] = useState(expense.category)
  const [payer, setPayer] = useState(expense.payer)
  const [remark, setRemark] = useState(expense.remark)
  const [time, setTime] = useState(expense.time)

  useEffect(() => {
    setAmount(expense.amount)
    setCategory(expense.category)
    setPayer(expense.payer)
    setRemark(expense.remark)
    setTime(expense.time)
  }, [expense])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!amount || parseFloat(amount) <= 0) {
      alert('请输入有效金额')
      return
    }
    eventBus.emit('expense:edited', {
      id: expense.id,
      amount,
      category,
      payer,
      remark,
      time
    })
    eventBus.emit('editor:close', undefined)
  }

  const handleCancel = () => {
    eventBus.emit('editor:close', undefined)
  }

  const formatLocalInputTime = (isoStr: string): string => {
    const d = new Date(isoStr)
    const p = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
  }

  const toIsoTime = (localStr: string): string => {
    if (!localStr) return expense.time
    const d = new Date(localStr)
    const p = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`
  }

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>编辑支出记录</h3>
          <button
            type="button"
            className="btn-close"
            onClick={handleCancel}
            aria-label="关闭"
          >
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-row">
            <div className="form-group">
              <label>金额</label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>时间</label>
              <input
                type="datetime-local"
                value={formatLocalInputTime(time)}
                onChange={(e) => setTime(toIsoTime(e.target.value))}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>分类</label>
              <select value={category} onChange={(e) => setCategory(e.target.value)}>
                {config.categories.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>付款人</label>
              <select value={payer} onChange={(e) => setPayer(e.target.value)}>
                {config.payers.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>备注</label>
            <input
              type="text"
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="例如：买了什么"
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={handleCancel}>
              取消
            </button>
            <button type="submit" className="btn-primary">
              保存修改
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ExpenseEditor
