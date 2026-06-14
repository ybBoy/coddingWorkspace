import { useEffect, useMemo, useRef, useState } from 'react';
import BookForm from '../components/BookForm';
import BookList from '../components/BookList';
import StatusFilter, { type FilterValue } from '../components/StatusFilter';
import {
  ApiError,
  createBook,
  deleteBook,
  fetchBooks,
  updateBook,
  updateBookStatus
} from '../api/bookApi';
import type { Book, BookInput, ReadingStatus } from '../types/book';

type SortBy = 'createdAt' | 'status' | 'title';

interface Toast {
  id: number;
  type: 'success' | 'error' | 'info';
  message: string;
}

export default function BookPage() {
  const [allBooks, setAllBooks] = useState<Book[]>([]);
  const [filter, setFilter] = useState<FilterValue>('ALL');
  const [keyword, setKeyword] = useState('');
  const [sortBy, setSortBy] = useState<SortBy>('createdAt');
  const [groupByStatus, setGroupByStatus] = useState(false);

  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [editError, setEditError] = useState<string | null>(null);

  const [toasts, setToasts] = useState<Toast[]>([]);
  const toastIdRef = useRef(0);

  const pushToast = (type: Toast['type'], message: string) => {
    const id = ++toastIdRef.current;
    setToasts((prev) => [...prev, { id, type, message }]);
    window.setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  };

  const loadBooks = async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const list = await fetchBooks({ status: filter, keyword, sortBy });
      setAllBooks(list);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : (err as Error).message;
      pushToast('error', '加载书籍失败：' + msg);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    loadBooks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadBooks(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, keyword, sortBy]);

  const handleCreate = async (input: BookInput) => {
    setCreateError(null);
    try {
      await createBook(input);
      pushToast('success', `已添加《${input.title}》`);
      await loadBooks(true);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : (err as Error).message;
      setCreateError(msg);
      pushToast('error', '添加失败：' + msg);
      throw err;
    }
  };

  const handleEditSubmit = async (id: string, input: BookInput) => {
    setEditError(null);
    try {
      await updateBook(id, input);
      pushToast('success', `已更新《${input.title}》`);
      setEditingId(null);
      await loadBooks(true);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : (err as Error).message;
      setEditError(msg);
      pushToast('error', '保存失败：' + msg);
      throw err;
    }
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditError(null);
  };

  const handleStatusChange = async (id: string, status: ReadingStatus) => {
    try {
      const updated = await updateBookStatus(id, status);
      pushToast('info', `已将《${updated.title}》改为【${
        { TO_READ: '想读', READING: '在读', READ: '已读' }[status]
      }】`);
      await loadBooks(true);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : (err as Error).message;
      pushToast('error', '更新状态失败：' + msg);
    }
  };

  const handleDelete = async (id: string) => {
    const target = allBooks.find((b) => b.id === id);
    const name = target ? `《${target.title}》` : '这本书';
    if (!window.confirm(`确定要删除${name}吗？`)) return;
    try {
      if (editingId === id) handleCancelEdit();
      await deleteBook(id);
      pushToast('success', `已删除${name}`);
      await loadBooks(true);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : (err as Error).message;
      pushToast('error', '删除失败：' + msg);
    }
  };

  const stats = useMemo(() => {
    const total = allBooks.length;
    const readCount = allBooks.filter((b) => b.status === 'READ').length;
    return { total, readCount, filteredCount: allBooks.length };
  }, [allBooks]);

  return (
    <div className="app">
      <ToastStack toasts={toasts} />

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
          <BookForm onSubmit={handleCreate} formError={createError} />
        </aside>
        <section className="app-right">
          <div className="toolbar">
            <div className="toolbar-row">
              <div className="search-box">
                <span className="search-icon">🔍</span>
                <input
                  type="text"
                  value={keyword}
                  placeholder="搜索书名或作者..."
                  onChange={(e) => setKeyword(e.target.value)}
                />
                {keyword && (
                  <button
                    className="search-clear"
                    type="button"
                    onClick={() => setKeyword('')}
                    aria-label="清除搜索"
                  >
                    ×
                  </button>
                )}
              </div>
              <div className="sort-box">
                <label>排序：</label>
                <select value={sortBy} onChange={(e) => setSortBy(e.target.value as SortBy)}>
                  <option value="createdAt">添加时间（最新）</option>
                  <option value="status">按阅读状态</option>
                  <option value="title">按书名（A-Z）</option>
                </select>
              </div>
            </div>
            <div className="toolbar-row toolbar-row-sub">
              <StatusFilter value={filter} onChange={setFilter} />
              <label className="toggle-group">
                <input
                  type="checkbox"
                  checked={groupByStatus}
                  onChange={(e) => setGroupByStatus(e.target.checked)}
                />
                <span>分组展示</span>
              </label>
            </div>
          </div>

          {loading ? (
            <div className="loading">加载中...</div>
          ) : (
            <BookList
              books={allBooks}
              groupByStatus={groupByStatus}
              editingId={editingId}
              formError={editError}
              onStartEdit={(id) => {
                setEditError(null);
                setEditingId(id);
              }}
              onCancelEdit={handleCancelEdit}
              onSubmitEdit={handleEditSubmit}
              onStatusChange={handleStatusChange}
              onDelete={handleDelete}
            />
          )}
        </section>
      </main>
    </div>
  );
}

function ToastStack({ toasts }: { toasts: Toast[] }) {
  return (
    <div className="toast-stack">
      {toasts.map((t) => (
        <div key={t.id} className={`toast toast-${t.type}`}>
          {t.type === 'success' && <span className="toast-icon">✅</span>}
          {t.type === 'error' && <span className="toast-icon">❌</span>}
          {t.type === 'info' && <span className="toast-icon">ℹ️</span>}
          <span>{t.message}</span>
        </div>
      ))}
    </div>
  );
}
