export interface CoffeeBean {
  id: string;
  name: string;
  origin: string;
  roastLevel: string;
  stockGrams: number;
  minStockLevel: number;
  createdAt: string;
  stockRecords: StockRecord[];
}

export interface StockRecord {
  id: string;
  beanId: string;
  type: 'INIT' | 'RESTOCK' | 'CONSUME';
  quantity: number;
  remainingStock: number;
  timestamp: string;
}

export interface AddBeanRequest {
  name: string;
  origin: string;
  roastLevel: string;
  stockGrams: number;
  minStockLevel: number;
}
