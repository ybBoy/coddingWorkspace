import React, { useState, useEffect } from 'react'
import { ComparisonResult } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

const ComparisonPanel: React.FC = () => {
  const [comparison, setComparison] = useState<ComparisonResult | null>(null)

  useEffect(() => {
    const unsub = eventBus.on('comparison:result', (data: ComparisonResult) => {
      setComparison(data)
    })
    return () => { unsub() }
  }, [])

  const formatAmount = (val: string): string => {
    return parseFloat(val).toFixed(2)
  }

  const formatDiff = (diff: string): { text: string; className: string } => {
    const num = parseFloat(diff)
    if (num > 0) return { text: `+¥${num.toFixed(2)}`, className: 'diff-up' }
    if (num < 0) return { text: `-¥${Math.abs(num).toFixed(2)}`, className: 'diff-down' }
    return { text: '¥0.00', className: 'diff-same' }
  }

  if (!comparison) {
    return (
      <div className="modal-overlay" onClick={() => eventBus.emit('comparison:close', undefined)}>
        <div className="modal modal-large" onClick={e => e.stopPropagation()}>
          <div className="modal-header">
            <h3>📊 月度对比</h3>
            <button type="button" className="btn-close" onClick={() => eventBus.emit('comparison:close', undefined)}>×</button>
          </div>
          <div className="modal-body">
            <div className="empty-state">加载中...</div>
          </div>
        </div>
      </div>
    )
  }

  const totalDiff = formatDiff(comparison.totalDiff)

  return (
    <div className="modal-overlay" onClick={() => eventBus.emit('comparison:close', undefined)}>
      <div className="modal modal-large" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>📊 月度对比</h3>
          <button type="button" className="btn-close" onClick={() => eventBus.emit('comparison:close', undefined)}>×</button>
        </div>
        <div className="modal-body">
          <div className="comparison-summary">
            <div className="comparison-total-row">
              <div className="comparison-col">
                <div className="comparison-label">本月</div>
                <div className="comparison-value">¥{formatAmount(comparison.currentTotal)}</div>
              </div>
              <div className="comparison-col comparison-center">
                <div className={`comparison-diff ${totalDiff.className}`}>{totalDiff.text}</div>
              </div>
              <div className="comparison-col">
                <div className="comparison-label">上月</div>
                <div className="comparison-value">¥{formatAmount(comparison.previousTotal)}</div>
              </div>
            </div>
          </div>

          <h4 className="section-subtitle">📁 分类变化</h4>
          <div className="comparison-list">
            {Object.entries(comparison.categoryChanges).map(([cat, vals]) => {
              const diff = formatDiff(vals.diff)
              return (
                <div key={cat} className="comparison-item">
                  <span className="comparison-name">{cat}</span>
                  <span className="comparison-vals">
                    ¥{formatAmount(vals.previous)} → ¥{formatAmount(vals.current)}
                  </span>
                  <span className={`comparison-diff-val ${diff.className}`}>{diff.text}</span>
                </div>
              )
            })}
          </div>

          <h4 className="section-subtitle">👤 成员变化</h4>
          <div className="comparison-list">
            {Object.entries(comparison.payerChanges).map(([payer, vals]) => {
              const diff = formatDiff(vals.diff)
              return (
                <div key={payer} className="comparison-item">
                  <span className="comparison-name">{payer}</span>
                  <span className="comparison-vals">
                    ¥{formatAmount(vals.previous)} → ¥{formatAmount(vals.current)}
                  </span>
                  <span className={`comparison-diff-val ${diff.className}`}>{diff.text}</span>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ComparisonPanel
