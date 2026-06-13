import { useEffect, useMemo, useState } from 'react';
import BookForm from '../components/BookForm';
import BookList from '../components/BookList';
import StatusFilter, { type FilterValue } from '../components/StatusFilter';
import { createBook, deleteBook, fetchBooks, updateBookStatus } from '../api/bookApi';
import type { Book, BookInput, ReadingStatus } from '../types/book';

export default function BookPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [filter, setFilter] = useState<FilterValue>('ALL');
  const [loading, setLoading] = useState(true);

  const loadBooks = async () => {
    setLoading(true);
    try {
      const list = await fetchBooks();
      setBooks(list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBooks();
  }, []);

  const handleCreate = async (input: BookInput) => {
    await createBook(input);
    await loadBooks();
  };

  const handleStatusChange = async (id: string, status: ReadingStatus) => {
    await updateBookStatus(id, status);
    await loadBooks();
  };

  const handleDelete = async (id: string) => {
    if (!confirm('确定要删除这本书吗？')) return;
    await deleteBook(id);
    await loadBooks();
  };

  const stats = useMemo(() => {
    const total = books.length;
    const readCount = books.filter((b) => b.status === 'READ').length;
    const filteredCount =
      filter === 'ALL' ? total : books.filter((b) => b.status === filter).length;
    return { total, readCount, filteredCount };
  }, [books, filter]);

  const filteredBooks = useMemo(() => {
    if (filter === 'ALL') return books;
    return books.filter((b) => b.status === filter);
  }, [books, filter]);

  return (
    <div className="app">
      <header className="app-header">
        <h1>📚 个人读书清单</h1>
        <div className="stats">
          <span className="stat-item">总书籍：<strong>{stats.total}</strong></span>
          <span className="stat-item">已读：<strong>{stats.readCount}</strong></span>
          <span className="stat-item">当前筛选：<strong>{stats.filteredCount}</strong></span>
        </div>
      </header>

      <main className="app-main">
        <aside className="app-left">
          <BookForm onSubmit={handleCreate} />
        </aside>
        <section className="app-right">
          <StatusFilter value={filter} onChange={setFilter} />
          {loading ? <div className="loading">加载中...</div> : (
            <BookList books={filteredBooks} onStatusChange={handleStatusChange} onDelete={handleDelete} />
          )}
        </section>
      </main>
    </div>
  );
}
