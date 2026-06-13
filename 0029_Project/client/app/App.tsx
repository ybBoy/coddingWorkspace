import React, { useState, useEffect } from 'react'
import { LedgerState, ConnectionStatus } from '../shared/types'
import { eventBus } from '../shared/EventBus'
import { socketClient } from '../shared/socketClient'
import ExpenseForm from '../features/expense/ExpenseForm'
import SummaryPanel from '../features/summary/SummaryPanel'
import BudgetEditor from '../features/budget/BudgetEditor'
import ExpenseTimeline from '../features/history/ExpenseTimeline'

const App: React.FC = () => {
  const [state, setState] = useState<LedgerState>({
    summary: { total: '0' },
    categoryStats: [],
    expenses: [],
    budgets: []
  })
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting')
  const [currentMonth] = useState<string>(() => {
    const now = new Date()
    return `${now.getFullYear()}年${now.getMonth() + 1}月`
  })

  useEffect(() => {
    const unsubState = eventBus.on('state:updated', (newState) => {
      setState(newState)
    })

    const unsubConnection = eventBus.on('connection:changed', (status) => {
      setConnectionStatus(status)
    })

    socketClient.connect()

    return () => {
      unsubState()
      unsubConnection()
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
          <span className="current-month">{currentMonth}</span>
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
          />
          <ExpenseTimeline expenses={state.expenses} />
        </section>
      </main>
    </div>
  )
}

export default App
