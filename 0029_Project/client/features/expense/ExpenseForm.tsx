import React, { useState, useEffect } from 'react'
import { eventBus } from '../../shared/EventBus'

interface FormData {
  amount: string
  category: string
  payer: string
  remark: string
}

interface ExpenseFormProps {
  categories: string[]
  payers: string[]
  defaultPayer?: string
}

const ExpenseForm: React.FC<ExpenseFormProps> = ({ categories, payers, defaultPayer }) => {
  const [formData, setFormData] = useState<FormData>({
    amount: '',
    category: categories[0] || '',
    payer: defaultPayer || payers[0] || '',
    remark: ''
  })

  useEffect(() => {
    setFormData(prev => ({
      ...prev,
      category: categories.includes(prev.category) ? prev.category : (categories[0] || ''),
      payer: defaultPayer && payers.includes(defaultPayer) ? defaultPayer : (payers.includes(prev.payer) ? prev.payer : (payers[0] || ''))
    }))
  }, [categories, payers, defaultPayer])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!formData.amount || parseFloat(formData.amount) <= 0) {
      alert('请输入有效金额')
      return
    }

    eventBus.emit('expense:added', {
      amount: formData.amount,
      category: formData.category,
      payer: formData.payer,
      remark: formData.remark,
      time: new Date().toISOString().slice(0, 19)
    })

    setFormData({
      amount: '',
      category: categories[0] || '',
      payer: defaultPayer && payers.includes(defaultPayer) ? defaultPayer : (payers[0] || ''),
      remark: ''
    })
  }

  return (
    <div className="card">
      <h3 className="card-title">记一笔</h3>
      <form onSubmit={handleSubmit} className="form">
        <div className="form-row">
          <label className="form-label">金额</label>
          <input
            type="number"
            name="amount"
            value={formData.amount}
            onChange={handleChange}
            placeholder="0.00"
            step="0.01"
            min="0"
            className="form-input"
            required
          />
        </div>

        <div className="form-row">
          <label className="form-label">分类</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
            className="form-select"
          >
            {categories.map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label className="form-label">付款人</label>
          <select
            name="payer"
            value={formData.payer}
            onChange={handleChange}
            className="form-select"
          >
            {payers.map(payer => (
              <option key={payer} value={payer}>{payer}</option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label className="form-label">备注</label>
          <textarea
            name="remark"
            value={formData.remark}
            onChange={handleChange}
            placeholder="买了什么..."
            rows={2}
            className="form-textarea"
          />
        </div>

        <button type="submit" className="btn btn-primary btn-block">
          保存
        </button>
      </form>
    </div>
  )
}

export default ExpenseForm
