export interface Expense {
  id: string
  amount: string
  category: string
  payer: string
  remark: string
  time: string
}

export interface Budget {
  category: string
  amount: string
}

export interface CategoryStat {
  category: string
  spent: string
  budget: string
}

export interface Summary {
  total: string
}

export interface LedgerState {
  year: number
  month: number
  summary: Summary
  categoryStats: CategoryStat[]
  recentExpenses: Expense[]
  monthExpenses: Expense[]
  budgets: Budget[]
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'

export interface NewExpenseData {
  amount: string
  category: string
  payer: string
  remark: string
  time: string
}

export interface MonthInfo {
  year: number
  month: number
}

export type EventType =
  | 'expense:added'
  | 'expense:deleted'
  | 'budget:changed'
  | 'budget:removed'
  | 'month:changed'
  | 'state:updated'
  | 'connection:changed'

export interface EventMap {
  'expense:added': NewExpenseData
  'expense:deleted': string
  'budget:changed': Budget
  'budget:removed': string
  'month:changed': MonthInfo
  'state:updated': LedgerState
  'connection:changed': ConnectionStatus
}

export type EventHandler<T extends EventType> = (data: EventMap[T]) => void

export interface OutgoingMessage {
  type: 'ADD_EXPENSE' | 'DELETE_EXPENSE' | 'SET_BUDGET' | 'REMOVE_BUDGET' | 'GET_STATE'
  payload: any
}
