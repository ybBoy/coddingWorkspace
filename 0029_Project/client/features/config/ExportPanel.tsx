import React, { useState, useEffect } from 'react'
import { ExportRequest, ExportResult, MonthInfo } from '../../shared/types'
import { eventBus } from '../../shared/EventBus'

interface ExportPanelProps {
  defaultMonth: MonthInfo
}

const ExportPanel: React.FC<ExportPanelProps> = ({ defaultMonth }) => {
  const [year, setYear] = useState(defaultMonth.year)
  const [month, setMonth] = useState(defaultMonth.month)
  const [format, setFormat] = useState<'csv' | 'json'>('csv')
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    const unsub = eventBus.on('export:result', handleExportResult)
    return () => unsub()
  }, [format, year, month])

  const handleExport = () => {
    setExporting(true)
    const req: ExportRequest = { year, month, format }
    eventBus.emit('export:request', req)
  }

  const handleExportResult = (result: ExportResult) => {
    setExporting(false)
    try {
      let content: string
      let filename: string
      let mime: string

      if (result.format === 'csv') {
        content = '\uFEFF' + (result.content || '')
        filename = `账本-${result.year}年${result.month}月.csv`
        mime = 'text/csv;charset=utf-8'
      } else {
        content = JSON.stringify({
          expenses: result.expenses || [],
          categoryStats: result.categoryStats || {},
          payerStats: result.payerStats || {}
        }, null, 2)
        filename = `账本-${result.year}年${result.month}月.json`
        mime = 'application/json;charset=utf-8'
      }

      const blob = new Blob([content], { type: mime })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)

      eventBus.emit('toast:show', {
        message: '导出成功，文件已下载',
        type: 'success'
      })
    } catch (err) {
      console.error(err)
      eventBus.emit('toast:show', {
        message: '导出失败，请重试',
        type: 'danger'
      })
    }
  }

  const handleCancel = () => {
    eventBus.emit('export:close', undefined)
  }

  const years = Array.from({ length: 5 }, (_, i) => defaultMonth.year - 2 + i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>📁 导出月份数据</h3>
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
          <div className="form-row">
            <div className="form-group">
              <label>年份</label>
              <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
                {years.map((y) => (
                  <option key={y} value={y}>{y}年</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>月份</label>
              <select value={month} onChange={(e) => setMonth(parseInt(e.target.value))}>
                {months.map((m) => (
                  <option key={m} value={m}>{m}月</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>格式</label>
            <div className="radio-group">
              <label className="radio-item">
                <input
                  type="radio"
                  name="format"
                  value="csv"
                  checked={format === 'csv'}
                  onChange={() => setFormat('csv')}
                />
                <span>CSV（Excel 可打开，含账单明细+分类汇总+成员汇总）</span>
              </label>
              <label className="radio-item">
                <input
                  type="radio"
                  name="format"
                  value="json"
                  checked={format === 'json'}
                  onChange={() => setFormat('json')}
                />
                <span>JSON（结构化备份，适合二次开发）</span>
              </label>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={handleCancel}>
              取消
            </button>
            <button
              type="button"
              className="btn-primary"
              onClick={handleExport}
              disabled={exporting}
            >
              {exporting ? '正在导出...' : '导出文件'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ExportPanel
