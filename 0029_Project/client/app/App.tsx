import React, { useState, useEffect } from 'react'
import { LedgerState, ConnectionStatus, MonthInfo, NewExpenseData, Budget } from '../shared/types'
import { eventBus } from '../shared/EventBus'
import { socketClient } from '../shared/socketClient'
import ExpenseForm from '../features/expense/ExpenseForm'
import SummaryPanel from '../features/summary/SummaryPanel'
import BudgetEditor from '../features/budget/BudgetEditor'
import ExpenseTimeline from '../features/history/ExpenseTimeline'

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
    budgets: []
  })
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting')
  const [selectedYear, setSelectedYear] = useState(initialYear)
  const [selectedMonth, setSelectedMonth] = useState(initialMonth)

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

  useEffect(() => {
    const unsubState = eventBus.on('state:updated', (newState) => {
      setState(newState)
    })

    const unsubConnection = eventBus.on('connection:changed', (status) => {
      setConnectionStatus(status)
    })

    const unsubExpenseAdded = eventBus.on('expense:added', (data: NewExpenseData) => {
      socketClient.send({
        type: 'ADD_EXPENSE',
        payload: data
      })
    })

    const unsubExpenseDeleted = eventBus.on('expense:deleted', (id: string) => {
      socketClient.send({
        type: 'DELETE_EXPENSE',
        payload: { id }
      })
    })

    const unsubBudgetChanged = eventBus.on('budget:changed', (budget: Budget) => {
      socketClient.send({
        type: 'SET_BUDGET',
        payload: budget
      })
    })

    const unsubBudgetRemoved = eventBus.on('budget:removed', (category: string) => {
      socketClient.send({
        type: 'REMOVE_BUDGET',
        payload: { category }
      })
    })

    const unsubMonthChanged = eventBus.on('month:changed', (monthInfo: MonthInfo) => {
      fetchStateForMonth(monthInfo.year, monthInfo.month)
    })

    socketClient.connect()

    return () => {
      unsubState()
      unsubConnection()
      unsubExpenseAdded()
      unsubExpenseDeleted()
      unsubBudgetChanged()
      unsubBudgetRemoved()
      unsubMonthChanged()
      socketClient.disconnect()
    }
  }, [])

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

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1 className="app-title">🏠 家庭账本</h1>
          <span className="connection-status">
            <span className="status-dot" style={{ backgroundColor: getStatusColor() }} />
            {getStatusText()}
          </span>
        </div>
        <div className="header-right">
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
          <ExpenseForm />
          <BudgetEditor budgets={state.budgets} />
        </aside>

        <section className="content">
          <SummaryPanel
            total={state.summary.total}
            categoryStats={state.categoryStats}
            currentMonth={formatMonthDisplay(selectedYear, selectedMonth)}
          />
          <ExpenseTimeline
            recentExpenses={state.recentExpenses}
            monthExpenses={state.monthExpenses}
            currentMonth={formatMonthDisplay(selectedYear, selectedMonth)}
          />
        </section>
      </main>
    </div>
  )
}

export default App
