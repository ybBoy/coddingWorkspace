import React, { useState } from 'react'
import { RecurringTemplate } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

interface TemplatePanelProps {
  templates: RecurringTemplate[]
  categories: string[]
  payers: string[]
  currentYear: number
  currentMonth: number
}

const TemplatePanel: React.FC<TemplatePanelProps> = ({ templates, categories, payers, currentYear, currentMonth }) => {
  const [showAdd, setShowAdd] = useState(false)
  const [name, setName] = useState('')
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState(categories[0] || '')
  const [payer, setPayer] = useState(payers[0] || '')
  const [remark, setRemark] = useState('')

  const handleAdd = () => {
    if (!name.trim() || !amount || parseFloat(amount) <= 0) {
      eventBus.emit('toast:show', { message: '请填写模板名称和金额', type: 'warning' })
      return
    }
    eventBus.emit('template:added', { name: name.trim(), amount, category, payer, remark })
    setName('')
    setAmount('')
    setRemark('')
    setShowAdd(false)
  }

  const handleDelete = (id: string) => {
    eventBus.emit('template:deleted', id)
  }

  const handleApplyAll = () => {
    if (templates.length === 0) {
      eventBus.emit('toast:show', { message: '没有可用模板', type: 'warning' })
      return
    }
    eventBus.emit('templates:apply', { year: currentYear, month: currentMonth })
  }

  return (
    <div className="card">
      <h3 className="card-title">
        🔄 重复账单模板
        <span className="card-subtitle">（{templates.length} 个）</span>
      </h3>

      {templates.length > 0 && (
        <div className="template-list">
          {templates.map(t => (
            <div key={t.id} className="template-item">
              <div className="template-info">
                <span className="template-name">{t.name}</span>
                <span className="template-meta">
                  {t.category} · {t.payer} · ¥{parseFloat(t.amount).toFixed(2)}
                </span>
                {t.remark && <span className="template-remark">{t.remark}</span>}
              </div>
              <button
                type="button"
                className="btn-delete-sm"
                onClick={() => handleDelete(t.id)}
                title="删除模板"
              >
                ×
              </button>
            </div>
          ))}
          <button
            type="button"
            className="btn btn-primary btn-block"
            onClick={handleApplyAll}
            style={{ marginTop: '12px' }}
          >
            一键生成本月账单
          </button>
        </div>
      )}

      {showAdd ? (
        <div className="template-form">
          <div className="form-row">
            <label className="form-label">模板名称</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="如：房租、网费"
              className="form-input"
            />
          </div>
          <div className="form-row">
            <label className="form-label">金额</label>
            <input
              type="number"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              placeholder="0.00"
              step="0.01"
              min="0"
              className="form-input"
            />
          </div>
          <div className="form-row">
            <label className="form-label">分类</label>
            <select value={category} onChange={e => setCategory(e.target.value)} className="form-select">
              {categories.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="form-row">
            <label className="form-label">付款人</label>
            <select value={payer} onChange={e => setPayer(e.target.value)} className="form-select">
              {payers.map(p => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div className="form-row">
            <label className="form-label">备注</label>
            <input
              type="text"
              value={remark}
              onChange={e => setRemark(e.target.value)}
              placeholder="可选"
              className="form-input"
            />
          </div>
          <div className="template-form-actions">
            <button type="button" className="btn btn-primary" onClick={handleAdd}>添加</button>
            <button type="button" className="btn btn-secondary" onClick={() => setShowAdd(false)}>取消</button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="btn btn-secondary btn-block"
          onClick={() => setShowAdd(true)}
        >
          + 新增模板
        </button>
      )}
    </div>
  )
}

export default TemplatePanel
