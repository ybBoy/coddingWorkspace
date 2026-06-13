import React, { useState, useEffect } from 'react'
import { LedgerConfig } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

interface ConfigPanelProps {
  currentConfig: LedgerConfig
}

const ConfigPanel: React.FC<ConfigPanelProps> = ({ currentConfig }) => {
  const [ledgerName, setLedgerName] = useState(currentConfig.ledgerName)
  const [categories, setCategories] = useState<string[]>(currentConfig.categories)
  const [payers, setPayers] = useState<string[]>(currentConfig.payers)
  const [newCategory, setNewCategory] = useState('')
  const [newPayer, setNewPayer] = useState('')

  useEffect(() => {
    setLedgerName(currentConfig.ledgerName)
    setCategories(currentConfig.categories)
    setPayers(currentConfig.payers)
  }, [currentConfig])

  const handleCancel = () => {
    eventBus.emit('config:close', undefined)
  }

  const handleSave = () => {
    if (!ledgerName.trim()) {
      alert('请输入账本名称')
      return
    }
    if (categories.length === 0) {
      alert('至少保留一个分类')
      return
    }
    if (payers.length === 0) {
      alert('至少保留一个付款人')
      return
    }
    eventBus.emit('config:updated', {
      ledgerName: ledgerName.trim(),
      categories,
      payers
    })
    eventBus.emit('config:close', undefined)
  }

  const addCategory = () => {
    const name = newCategory.trim()
    if (name && !categories.includes(name)) {
      setCategories([...categories, name])
      setNewCategory('')
    }
  }

  const removeCategory = (name: string) => {
    if (categories.length <= 1) {
      alert('至少保留一个分类')
      return
    }
    setCategories(categories.filter(c => c !== name))
  }

  const addPayer = () => {
    const name = newPayer.trim()
    if (name && !payers.includes(name)) {
      setPayers([...payers, name])
      setNewPayer('')
    }
  }

  const removePayer = (name: string) => {
    if (payers.length <= 1) {
      alert('至少保留一个付款人')
      return
    }
    setPayers(payers.filter(p => p !== name))
  }

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>⚙️ 账本配置</h3>
          <button
            type="button"
            className="btn-close"
            onClick={handleCancel}
            aria-label="关闭"
          >
            ×
          </button>
        </div>
        <div className="modal-body">
          <div className="form-group">
            <label>账本名称</label>
            <input
              type="text"
              value={ledgerName}
              onChange={(e) => setLedgerName(e.target.value)}
              placeholder="例如：我的家庭账本"
              maxLength={30}
            />
          </div>

          <div className="config-section">
            <label>分类列表</label>
            <div className="tag-list">
              {categories.map((c) => (
                <span key={c} className="tag">
                  {c}
                  <button
                    type="button"
                    className="tag-close"
                    onClick={() => removeCategory(c)}
                    aria-label={`删除${c}`}
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
            <div className="tag-input-row">
              <input
                type="text"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                placeholder="输入新分类名称"
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addCategory())}
              />
              <button type="button" className="btn-secondary" onClick={addCategory}>
                添加
              </button>
            </div>
          </div>

          <div className="config-section">
            <label>付款人列表</label>
            <div className="tag-list">
              {payers.map((p) => (
                <span key={p} className="tag">
                  {p}
                  <button
                    type="button"
                    className="tag-close"
                    onClick={() => removePayer(p)}
                    aria-label={`删除${p}`}
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
            <div className="tag-input-row">
              <input
                type="text"
                value={newPayer}
                onChange={(e) => setNewPayer(e.target.value)}
                placeholder="输入新付款人名称"
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addPayer())}
              />
              <button type="button" className="btn-secondary" onClick={addPayer}>
                添加
              </button>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={handleCancel}>
              取消
            </button>
            <button type="button" className="btn-primary" onClick={handleSave}>
              保存配置
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ConfigPanel
