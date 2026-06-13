import { CoffeeBean, StockRecord, AddBeanRequest } from '../types';

const API_BASE = '/api/beans';

export const beanService = {
  async getAllBeans(roastLevel?: string): Promise<CoffeeBean[]> {
    const url = roastLevel
      ? `${API_BASE}?roastLevel=${encodeURIComponent(roastLevel)}`
      : API_BASE;
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

  async restockBean(id: string, amount: number): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}/restock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount }),
    });
    if (!res.ok) throw new Error('补货失败');
    return res.json();
  },

  async consumeBean(id: string, amount: number): Promise<CoffeeBean> {
    const res = await fetch(`${API_BASE}/${id}/consume`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount }),
    });
    if (!res.ok) throw new Error('消耗记录失败');
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
};
