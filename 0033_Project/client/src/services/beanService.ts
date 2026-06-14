import {
  CoffeeBean,
  StockRecord,
  AddBeanRequest,
  EditBeanRequest,
  StockOperationRequest,
  StatisticsResponse,
  WarningSummary,
  SortField,
  SortDir,
} from '../types';

const API_BASE = '/api/beans';

export const beanService = {
  async getAllBeans(
    roastLevel?: string,
    search?: string,
    sortBy?: SortField,
    sortDir?: SortDir
  ): Promise<CoffeeBean[]> {
    const params = new URLSearchParams();
    if (roastLevel) params.set('roastLevel', roastLevel);
    if (search) params.set('search', search);
    if (sortBy) params.set('sortBy', sortBy);
    if (sortDir) params.set('sortDir', sortDir);
    const query = params.toString();
    const url = query ? `${API_BASE}?${query}` : API_BASE;
    const res = await fetch(url);
    if (!res.ok) throw new Error('获取咖啡豆列表失败');
    return res.json();
  },

  async getBeanById(id: string): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}`);
    if (!res.ok) throw new Error('获取咖啡豆信息失败');
    return res.json();
  },

  async addBean(bean: AddBeanRequest): Promise<CoffeeBean> {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(bean),
    });
    if (!res.ok) throw new Error('新增咖啡豆失败');
    return res.json();
  },

  async updateBean(id: string, req: EditBeanRequest): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error('编辑咖啡豆失败');
    return res.json();
  },

  async restockBean(id: string, req: StockOperationRequest): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}/restock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || '补货失败');
    }
    return res.json();
  },

  async consumeBean(id: string, req: StockOperationRequest): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}/consume`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || '消耗记录失败');
    }
    return res.json();
  },

  async deleteBean(id: string): Promise<void> {
    const res = await fetch(`${API_BASE}/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error('删除咖啡豆失败');
  },

  async getRecentRecords(id: string, limit: number = 5): Promise<StockRecord[]> {
    const res = await fetch(`${API_BASE}/${id}/records?limit=${limit}`);
    if (!res.ok) throw new Error('获取库存记录失败');
    return res.json();
  },

  async getLowStockCount(): Promise<number> {
    const res = await fetch(`${API_BASE}/low-stock/count`);
    if (!res.ok) throw new Error('获取低库存数量失败');
    const data = await res.json();
    return data.count;
  },

  async getWarningSummary(): Promise<WarningSummary> {
    const res = await fetch(`${API_BASE}/warnings/summary`);
    if (!res.ok) throw new Error('获取预警汇总失败');
    return res.json();
  },

  async getStatistics(): Promise<StatisticsResponse> {
    const res = await fetch(`${API_BASE}/statistics`);
    if (!res.ok) throw new Error('获取统计数据失败');
    return res.json();
  },

  async importBeans(beans: CoffeeBean[], replace: boolean = false): Promise<CoffeeBean[]> {
    const res = await fetch(`${API_BASE}/import?replace=${replace}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(beans),
    });
    if (!res.ok) throw new Error('导入失败');
    return res.json();
  },

  exportJsonUrl(): string {
    return `${API_BASE}/export/json`;
  },

  exportCsvUrl(): string {
    return `${API_BASE}/export/csv`;
  },

  async exportJson(): Promise<CoffeeBean[]> {
    const res = await fetch(`${API_BASE}/export/json`);
    if (!res.ok) throw new Error('导出 JSON 失败');
    return res.json();
  },
};
