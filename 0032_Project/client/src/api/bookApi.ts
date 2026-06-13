import type { Book, BookInput, ReadingStatus } from '../types/book';

const BASE_URL = '/api/books';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers
    },
    ...options
  });
  if (!res.ok) {
    throw new Error(`请求失败: ${res.status}`);
  }
  return res.json();
}

export function fetchBooks(status?: ReadingStatus): Promise<Book[]> {
  const url = status ? `${BASE_URL}?status=${status}` : BASE_URL;
  return request<Book[]>(url);
}

export function createBook(input: BookInput): Promise<Book> {
  return request<Book>(BASE_URL, {
    method: 'POST',
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
  return request<void>(`${BASE_URL}/${id}`, {
    method: 'DELETE'
  });
}
