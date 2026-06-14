import type { Book, BookInput, ReadingStatus } from '../types/book';

const BASE_URL = '/api/books';

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = 'ApiError';
  }
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(url, {
      headers: { 'Content-Type': 'application/json', ...options?.headers },
      ...options
    });
  } catch (e) {
    throw new ApiError(0, '网络连接失败，请检查后端服务是否启动');
  }

  if (!res.ok) {
    let msg = `请求失败（${res.status}）`;
    try {
      const data = (await res.json()) as { error?: string };
      if (data?.error) msg = data.error;
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, msg);
  }

  if (res.status === 204) return undefined as unknown as T;
  return (await res.json()) as T;
}

export interface QueryParams {
  status?: ReadingStatus | 'ALL';
  keyword?: string;
  sortBy?: 'createdAt' | 'status' | 'title';
}

export function fetchBooks(params: QueryParams = {}): Promise<Book[]> {
  const usp = new URLSearchParams();
  if (params.status && params.status !== 'ALL') usp.set('status', params.status);
  if (params.keyword && params.keyword.trim()) usp.set('keyword', params.keyword.trim());
  if (params.sortBy) usp.set('sortBy', params.sortBy);
  const qs = usp.toString();
  const url = qs ? `${BASE_URL}?${qs}` : BASE_URL;
  return request<Book[]>(url);
}

export function createBook(input: BookInput): Promise<Book> {
  return request<Book>(BASE_URL, {
    method: 'POST',
    body: JSON.stringify(input)
  });
}

export function updateBook(id: string, input: BookInput): Promise<Book> {
  return request<Book>(`${BASE_URL}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input)
  });
}

export function updateBookStatus(id: string, status: ReadingStatus): Promise<Book> {
  return request<Book>(`${BASE_URL}/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status })
  });
}

export function deleteBook(id: string): Promise<void> {
  return request<void>(`${BASE_URL}/${id}`, { method: 'DELETE' });
}
