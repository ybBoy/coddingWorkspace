import type { Book, ReadingStatus } from '../types/book';
import { STATUS_LABELS } from '../types/book';

interface BookListProps {
  books: Book[];
  onStatusChange: (id: string, status: ReadingStatus) => void;
  onDelete: (id: string) => void;
}

const STATUS_CLASS: Record<ReadingStatus, string> = {
  TO_READ: 'status-to-read',
  READING: 'status-reading',
  READ: 'status-read'
};

export default function BookList({ books, onStatusChange, onDelete }: BookListProps) {
  if (books.length === 0) {
    return <div className="empty-tip">暂无书籍，快去添加一本吧～</div>;
  }

  return (
    <div className="book-list">
      {books.map((book) => (
        <div key={book.id} className="book-card">
          <div className="book-info">
            <h3 className="book-title">{book.title}</h3>
            <p className="book-author">作者：{book.author}</p>
            {book.remark && <p className="book-remark">"{book.remark}"</p>}
          </div>
          <div className="book-actions">
            <span className={`status-tag ${STATUS_CLASS[book.status]}`}>
              {STATUS_LABELS[book.status]}
            </span>
            <select
              className="status-select"
              value={book.status}
              onChange={(e) => onStatusChange(book.id, e.target.value as ReadingStatus)}
            >
              {(Object.keys(STATUS_LABELS) as ReadingStatus[]).map((s) => (
                <option key={s} value={s}>
                  改为{STATUS_LABELS[s]}
                </option>
              ))}
            </select>
            <button className="delete-btn" onClick={() => onDelete(book.id)}>
              删除
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
