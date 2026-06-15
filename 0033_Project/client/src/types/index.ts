export interface CoffeeBean {
  id: string;
  name: string;
  origin: string;
  roastLevel: string;
  stockGrams: number;
  minStockLevel: number;
  createdAt: string;
  lastModified?: string;
  stockRecords: StockRecord[];
}

export interface StockRecord {
  id: string;
  beanId: string;
  type: 'INIT' | 'RESTOCK' | 'CONSUME' | 'EDIT';
  quantity: number;
  beforeStock: number;
  afterStock: number;
  operator: string;
  remark: string;
  timestamp: string;
}

export interface AddBeanRequest {
  name: string;
  origin: string;
  roastLevel: string;
  stockGrams: number;
  minStockLevel: number;
}

export interface EditBeanRequest {
  name?: string;
  origin?: string;
  roastLevel?: string;
  minStockLevel?: number;
  operator?: string;
}

export interface StockOperationRequest {
  amount: number;
  operator?: string;
  remark?: string;
}

export interface WeeklyConsumeBean {
  beanId: string;
  beanName: string;
  totalConsumed: number;
}

export interface StatisticsResponse {
  totalStockGrams: number;
  totalBeanKinds: number;
  approachingCount: number;
  lowStockCount: number;
  emptyCount: number;
  lowStockBeans: CoffeeBean[];
  weeklyTopConsumed: WeeklyConsumeBean[];
}

export interface WarningSummary {
  total: number;
  approachingCount: number;
  approaching: CoffeeBean[];
  lowStockCount: number;
  lowStock: CoffeeBean[];
  emptyCount: number;
  empty: CoffeeBean[];
}

export type WarningLevel = 'NONE' | 'APPROACHING' | 'LOW' | 'EMPTY';

export type SortField = 'name' | 'origin' | 'stock' | 'minStock' | 'lastModified';
export type SortDir = 'asc' | 'desc';
