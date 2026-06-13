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
  summary: Summary
  categoryStats: CategoryStat[]
  expenses: Expense[]
  budgets: Budget[]
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'

export type EventType =
  | 'expense:added'
  | 'expense:deleted'
  | 'budget:changed'
  | 'month:changed'
  | 'state:updated'
  | 'connection:changed'

export interface EventMap {
  'expense:added': Expense
  'expense:deleted': string
  'budget:changed': Budget
  'month:changed': string
  'state:updated': LedgerState
  'connection:changed': ConnectionStatus
}

export type EventHandler<T extends EventType> = (data: EventMap[T]) => void

export interface OutgoingMessage {
  type: 'ADD_EXPENSE' | 'DELETE_EXPENSE' | 'SET_BUDGET' | 'GET_STATE'
  payload: any
}
