import React, { useState, useEffect, useRef } from 'react'
import {
  LedgerState, ConnectionStatus, MonthInfo, NewExpenseData, Budget,
  EditExpenseData, WarningCategory, ToastData, Expense, ExportRequest, LedgerConfig
} from '../shared/types'
import { eventBus } from '../shared/EventBus'
import { socketClient } from '../shared/socketClient'
import ExpenseForm from '../features/expense/ExpenseForm'
import ExpenseEditor from '../features/expense/ExpenseEditor'
import SummaryPanel from '../features/summary/SummaryPanel'
import BudgetEditor from '../features/budget/BudgetEditor'
import ExpenseTimeline from '../features/history/ExpenseTimeline'
import ExportPanel from '../features/config/ExportPanel'
import ConfigPanel from '../features/config/ConfigPanel'

const DEFAULT_CONFIG: LedgerConfig = {
  ledgerName: '🏠 家庭账本',
  categories: ['餐饮', '购物', '交通', '娱乐', '医疗', '教育', '住房', '通讯', '其他'],
  payers: ['爸爸', '妈妈', '孩子', '共同']
}

const now = new Date()
const initialYear = now.getFullYear()
const initialMonth = now.getMonth() + 1

const App: React.FC = () => {
  const [state, setState] = useState<LedgerState>({
    year: initialYear,
    month: initialMonth,
    summary: { total: '0' },
    categoryStats: [],
    recentExpenses: [],
    monthExpenses: [],
    budgets: [],
    payerStats: [],
    warningCategories: [],
    config: DEFAULT_CONFIG
  })
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting')
  const [selectedYear, setSelectedYear] = useState(initialYear)
  const [selectedMonth, setSelectedMonth] = useState(initialMonth)
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null)
  const [showExport, setShowExport] = useState(false)
  const [showConfig, setShowConfig] = useState(false)
  const [toasts, setToasts] = useState<{ id: number; data: ToastData }[]>([])
  const toastIdRef = useRef(0)

  const config: LedgerConfig = state.config || DEFAULT_CONFIG

  const formatMonthDisplay = (year: number, month: number): string => {
    return `${year}年${month}月`
  }

  const fetchStateForMonth = (year: number, month: number) => {
    socketClient.send({
      type: 'GET_STATE',
      payload: { year, month }
    })
  }

  const handlePrevMonth = () => {
    let newYear = selectedYear
    let newMonth = selectedMonth - 1
    if (newMonth < 1) {
      newMonth = 12
      newYear = selectedYear - 1
    }
    setSelectedYear(newYear)
    setSelectedMonth(newMonth)
    eventBus.emit('month:changed', { year: newYear, month: newMonth })
  }

  const handleNextMonth = () => {
    let newYear = selectedYear
    let newMonth = selectedMonth + 1
    if (newMonth > 12) {
      newMonth = 1
      newYear = selectedYear + 1
    }
    setSelectedYear(newYear)
    setSelectedMonth(newMonth)
    eventBus.emit('month:changed', { year: newYear, month: newMonth })
  }

  const addToast = (data: ToastData) => {
    const id = ++toastIdRef.current
    setToasts(prev => [...prev, { id, data }])
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id))
    }, 3000)
  }

  useEffect(() => {
    const prevWarnings: WarningCategory[] = []

    const unsubState = eventBus.on('state:updated', (newState) => {
      const newWarnings = newState.warningCategories || []
      newWarnings.forEach(w => {
        const existed = prevWarnings.some(pw => pw.category === w.category && pw.level === w.level)
        if (!existed) {
          if (w.level === 'over') {
            addToast({ message: `⚠️「${w.category}」已超预算 ${w.ratio.toFixed(0)}%`, type: 'danger' })
          } else if (w.level === 'warning') {
            addToast({ message: `🟠「${w.category}」接近预算 ${w.ratio.toFixed(0)}%`, type: 'warning' })
          }
        }
      })
      prevWarnings.length = 0
      prevWarnings.push(...newWarnings)
      setState(newState)
    })

    const unsubConnection = eventBus.on('connection:changed', (status) => {
      setConnectionStatus(status)
    })

    const unsubExpenseAdded = eventBus.on('expense:added', (data: NewExpenseData) => {
      socketClient.send({
        type: 'ADD_EXPENSE',
        payload: { ...data, year: selectedYear, month: selectedMonth }
      })
    })

    const unsubExpenseEdited = eventBus.on('expense:edited', (data: EditExpenseData) => {
      socketClient.send({
        type: 'EDIT_EXPENSE',
        payload: { ...data, year: selectedYear, month: selectedMonth }
      })
      addToast({ message: '已保存修改', type: 'success' })
    })

    const unsubExpenseDeleted = eventBus.on('expense:deleted', (id: string) => {
      socketClient.send({
        type: 'DELETE_EXPENSE',
        payload: { id, year: selectedYear, month: selectedMonth }
      })
    })

    const unsubBudgetChanged = eventBus.on('budget:changed', (budget: Budget) => {
      socketClient.send({
        type: 'SET_BUDGET',
        payload: { ...budget, year: selectedYear, month: selectedMonth }
      })
    })

    const unsubBudgetRemoved = eventBus.on('budget:removed', (category: string) => {
      socketClient.send({
        type: 'REMOVE_BUDGET',
        payload: { category, year: selectedYear, month: selectedMonth }
      })
    })

    const unsubMonthChanged = eventBus.on('month:changed', (monthInfo: MonthInfo) => {
      fetchStateForMonth(monthInfo.year, monthInfo.month)
    })

    const unsubEditorOpen = eventBus.on('editor:open', (expense: Expense) => {
      setEditingExpense(expense)
    })

    const unsubEditorClose = eventBus.on('editor:close', () => {
      setEditingExpense(null)
    })

    const unsubExportOpen = eventBus.on('export:open', () => {
      setShowExport(true)
    })

    const unsubExportClose = eventBus.on('export:close', () => {
      setShowExport(false)
    })

    const unsubExportRequest = eventBus.on('export:request', (req: ExportRequest) => {
      socketClient.send({
        type: 'EXPORT_MONTH',
        payload: req
      })
    })

    const unsubConfigUpdated = eventBus.on('config:updated', (cfg: LedgerConfig) => {
      socketClient.send({
        type: 'UPDATE_CONFIG',
        payload: { ...cfg, year: selectedYear, month: selectedMonth }
      })
      addToast({ message: '配置已保存', type: 'success' })
    })

    const unsubConfigOpen = eventBus.on('config:open', () => {
      setShowConfig(true)
    })

    const unsubConfigClose = eventBus.on('config:close', () => {
      setShowConfig(false)
    })

    const unsubToast = eventBus.on('toast:show', (data: ToastData) => {
      addToast(data)
    })

    socketClient.connect()

    return () => {
      unsubState()
      unsubConnection()
      unsubExpenseAdded()
      unsubExpenseEdited()
      unsubExpenseDeleted()
      unsubBudgetChanged()
      unsubBudgetRemoved()
      unsubMonthChanged()
      unsubEditorOpen()
      unsubEditorClose()
      unsubExportOpen()
      unsubExportClose()
      unsubExportRequest()
      unsubConfigUpdated()
      unsubConfigOpen()
      unsubConfigClose()
      unsubToast()
      socketClient.disconnect()
    }
  }, [selectedYear, selectedMonth])

  const getStatusColor = (): string => {
    switch (connectionStatus) {
      case 'connected': return 'var(--success)'
      case 'connecting': return 'var(--warning)'
      case 'disconnected': return 'var(--danger)'
      default: return 'var(--muted)'
    }
  }

  const getStatusText = (): string => {
    switch (connectionStatus) {
      case 'connected': return '已连接'
      case 'connecting': return '连接中...'
      case 'disconnected': return '已断开'
      default: return '未知'
    }
  }

  const years = Array.from({ length: 5 }, (_, i) => initialYear - 2 + i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)

  const handleYearChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const year = parseInt(e.target.value)
    setSelectedYear(year)
    eventBus.emit('month:changed', { year, month: selectedMonth })
  }

  const handleMonthChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const month = parseInt(e.target.value)
    setSelectedMonth(month)
    eventBus.emit('month:changed', { year: selectedYear, month })
  }

  const warningOver = state.warningCategories.filter(w => w.level === 'over').length
  const warningNear = state.warningCategories.filter(w => w.level === 'warning').length

  const pendingCount = socketClient.getPendingCount()

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1 className="app-title">{config.ledgerName}</h1>
          <span className="connection-status">
            <span className="status-dot" style={{ backgroundColor: getStatusColor() }} />
            {getStatusText()}
            {pendingCount > 0 && (
              <span className="pending-badge">{pendingCount} 条待同步</span>
            )}
          </span>
        </div>
        <div className="header-center">
          {(warningOver > 0 || warningNear > 0) && (
            <div className="warnings-banner">
              {warningOver > 0 && (
                <span className="warning-chip danger">
                  <strong>{warningOver}</strong> 个超预算
                </span>
              )}
              {warningNear > 0 && (
                <span className="warning-chip warning">
                  <strong>{warningNear}</strong> 个接近预算
                </span>
              )}
            </div>
          )}
        </div>
        <div className="header-right">
          <div className="header-actions">
            <button
              type="button"
              className="btn-icon"
              onClick={() => eventBus.emit('export:open', undefined)}
              title="导出月份数据"
            >
              📁
            </button>
            <button
              type="button"
              className="btn-icon"
              onClick={() => eventBus.emit('config:open', undefined)}
              title="账本配置"
            >
              ⚙️
            </button>
          </div>
          <div className="month-selector">
            <button
              type="button"
              className="btn-month"
              onClick={handlePrevMonth}
              aria-label="上个月"
            >
              ‹
            </button>
            <select
              className="month-select"
              value={selectedYear}
              onChange={handleYearChange}
            >
              {years.map(y => (
                <option key={y} value={y}>{y}年</option>
              ))}
            </select>
            <select
              className="month-select"
              value={selectedMonth}
              onChange={handleMonthChange}
            >
              {months.map(m => (
                <option key={m} value={m}>{m}月</option>
              ))}
            </select>
            <button
              type="button"
              className="btn-month"
              onClick={handleNextMonth}
              aria-label="下个月"
            >
              ›
            </button>
          </div>
        </div>
      </header>

      <main className="app-main">
        <aside className="sidebar">
          <ExpenseForm categories={config.categories} payers={config.payers} />
          <BudgetEditor budgets={state.budgets} categories={config.categories} />
        </aside>

        <section className="content">
          <SummaryPanel
            total={state.summary.total}
            categoryStats={state.categoryStats}
            payerStats={state.payerStats}
            currentMonth={formatMonthDisplay(selectedYear, selectedMonth)}
          />
          <ExpenseTimeline
            monthExpenses={state.monthExpenses}
            currentMonth={formatMonthDisplay(selectedYear, selectedMonth)}
          />
        </section>
      </main>

      {editingExpense && (
        <ExpenseEditor expense={editingExpense} config={config} />
      )}
      {showExport && (
        <ExportPanel defaultMonth={{ year: selectedYear, month: selectedMonth }} />
      )}
      {showConfig && (
        <ConfigPanel currentConfig={config} />
      )}

      <div className="toast-container">
        {toasts.map(({ id, data }) => (
          <div key={id} className={`toast toast-${data.type}`}>
            {data.message}
          </div>
        ))}
      </div>
    </div>
  )
}

export default App
