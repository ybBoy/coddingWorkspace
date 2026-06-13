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

export interface PayerStat {
  payer: string
  amount: string
}

export interface WarningCategory {
  category: string
  ratio: number
  level: 'warning' | 'over'
}

export interface LedgerConfig {
  ledgerName: string
  categories: string[]
  payers: string[]
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
  payerStats: PayerStat[]
  warningCategories: WarningCategory[]
  config: LedgerConfig
  templates: RecurringTemplate[]
}

export interface RecurringTemplate {
  id: string
  name: string
  amount: string
  category: string
  payer: string
  remark: string
}

export interface ComparisonResult {
  currentTotal: string
  previousTotal: string
  totalDiff: string
  categoryChanges: Record<string, { current: string; previous: string; diff: string }>
  payerChanges: Record<string, { current: string; previous: string; diff: string }>
}

export interface BudgetTrendMonth {
  year: number
  month: number
  categories: BudgetTrendCategory[]
}

export interface BudgetTrendCategory {
  category: string
  budget: string
  spent: string
  ratio: number
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'

export interface NewExpenseData {
  amount: string
  category: string
  payer: string
  remark: string
  time: string
}

export interface EditExpenseData {
  id: string
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

export interface ExportRequest {
  year: number
  month: number
  format: 'csv' | 'json'
}

export interface ExportResult {
  year: number
  month: number
  format: 'csv' | 'json'
  content?: string
  expenses?: Expense[]
  categoryStats?: Record<string, string>
  payerStats?: Record<string, string>
}

export type EventType =
  | 'expense:added'
  | 'expense:deleted'
  | 'expense:edited'
  | 'budget:changed'
  | 'budget:removed'
  | 'month:changed'
  | 'state:updated'
  | 'connection:changed'
  | 'export:request'
  | 'export:result'
  | 'config:updated'
  | 'editor:open'
  | 'editor:close'
  | 'config:open'
  | 'config:close'
  | 'export:open'
  | 'export:close'
  | 'toast:show'
  | 'template:added'
  | 'template:deleted'
  | 'templates:apply'
  | 'comparison:open'
  | 'comparison:close'
  | 'comparison:result'
  | 'trend:open'
  | 'trend:close'
  | 'trend:result'

export interface ToastData {
  message: string
  type: 'success' | 'warning' | 'danger' | 'info'
}

export interface EventMap {
  'expense:added': NewExpenseData
  'expense:deleted': string
  'expense:edited': EditExpenseData
  'budget:changed': Budget
  'budget:removed': string
  'month:changed': MonthInfo
  'state:updated': LedgerState
  'connection:changed': ConnectionStatus
  'export:request': ExportRequest
  'export:result': ExportResult
  'config:updated': LedgerConfig
  'editor:open': Expense
  'editor:close': void
  'config:open': void
  'config:close': void
  'export:open': void
  'export:close': void
  'toast:show': ToastData
  'template:added': { name: string; amount: string; category: string; payer: string; remark: string }
  'template:deleted': string
  'templates:apply': MonthInfo
  'comparison:open': void
  'comparison:close': void
  'comparison:result': ComparisonResult
  'trend:open': void
  'trend:close': void
  'trend:result': BudgetTrendMonth[]
}

export type EventHandler<T extends EventType> = (data: EventMap[T]) => void

export interface OutgoingMessage {
  type: 'ADD_EXPENSE' | 'DELETE_EXPENSE' | 'EDIT_EXPENSE' | 'SET_BUDGET' | 'REMOVE_BUDGET' | 'GET_STATE' | 'EXPORT_MONTH' | 'UPDATE_CONFIG' | 'ADD_TEMPLATE' | 'DELETE_TEMPLATE' | 'APPLY_TEMPLATES' | 'GET_COMPARISON' | 'GET_BUDGET_TREND'
  payload: any
}
