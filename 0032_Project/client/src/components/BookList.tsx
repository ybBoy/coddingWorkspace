import type { Book, BookInput, ReadingStatus } from '../types/book';
import { STATUS_LABELS } from '../types/book';
import BookForm from './BookForm';

interface BookListProps {
  books: Book[];
  groupByStatus?: boolean;
  editingId: string | null;
  formError: string | null;
  onStartEdit: (id: string) => void;
  onCancelEdit: () => void;
  onSubmitEdit: (id: string, input: BookInput) => Promise<void>;
  onStatusChange: (id: string, status: ReadingStatus) => void;
  onDelete: (id: string) => void;
}

const STATUS_CLASS: Record<ReadingStatus, string> = {
  TO_READ: 'status-to-read',
  READING: 'status-reading',
  READ: 'status-read'
};

const STATUS_ORDER: ReadingStatus[] = ['READING', 'TO_READ', 'READ'];

function groupBooks(books: Book[]): Record<ReadingStatus, Book[]> {
  const result: Record<ReadingStatus, Book[]> = {
    READING: [],
    TO_READ: [],
    READ: []
  };
  for (const b of books) {
    result[b.status].push(b);
  }
  return result;
}

interface CardProps {
  book: Book;
  editing: boolean;
  formError: string | null;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onSubmitEdit: (input: BookInput) => Promise<void>;
  onStatusChange: (s: ReadingStatus) => void;
  onDelete: () => void;
}

function BookCard({
  book,
  editing,
  formError,
  onStartEdit,
  onCancelEdit,
  onSubmitEdit,
  onStatusChange,
  onDelete
}: CardProps) {
  if (editing) {
    return (
      <div className="book-card book-card-editing">
        <BookForm
          initialValue={{ ...book }}
          titleLabel="编辑书籍"
          onSubmit={onSubmitEdit}
          onCancel={onCancelEdit}
          formError={formError}
        />
      </div>
    );
  }
  return (
    <div className="book-card">
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
          onChange={(e) => onStatusChange(e.target.value as ReadingStatus)}
        >
          {(Object.keys(STATUS_LABELS) as ReadingStatus[]).map((s) => (
            <option key={s} value={s}>
              改为{STATUS_LABELS[s]}
            </option>
          ))}
        </select>
        <button className="edit-btn" onClick={onStartEdit}>
          编辑
        </button>
        <button className="delete-btn" onClick={onDelete}>
          删除
        </button>
      </div>
    </div>
  );
}

export default function BookList(props: BookListProps) {
  const {
    books,
    groupByStatus,
    editingId,
    formError,
    onStartEdit,
    onCancelEdit,
    onSubmitEdit,
    onStatusChange,
    onDelete
  } = props;

  if (books.length === 0) {
    return <div className="empty-tip">暂无匹配的书籍，换个条件试试～</div>;
  }

  if (groupByStatus) {
    const groups = groupBooks(books);
    return (
      <div className="book-list-grouped">
        {STATUS_ORDER.map((status) => (
          <section key={status} className={`book-group group-${status}`}>
            <div className="group-header">
              <span className={`status-tag ${STATUS_CLASS[status]}`}>{STATUS_LABELS[status]}</span>
              <span className="group-count">{groups[status].length}</span>
            </div>
            {groups[status].length === 0 ? (
              <div className="empty-tip small">— 暂无 —</div>
            ) : (
              <div className="book-list">
                {groups[status].map((book) => (
                  <BookCard
                    key={book.id}
                    book={book}
                    editing={editingId === book.id}
                    formError={editingId === book.id ? formError : null}
                    onStartEdit={() => onStartEdit(book.id)}
                    onCancelEdit={onCancelEdit}
                    onSubmitEdit={(input) => onSubmitEdit(book.id, input)}
                    onStatusChange={(s) => onStatusChange(book.id, s)}
                    onDelete={() => onDelete(book.id)}
                  />
                ))}
              </div>
            )}
          </section>
        ))}
      </div>
    );
  }

  return (
    <div className="book-list">
      {books.map((book) => (
        <BookCard
          key={book.id}
          book={book}
          editing={editingId === book.id}
          formError={editingId === book.id ? formError : null}
          onStartEdit={() => onStartEdit(book.id)}
          onCancelEdit={onCancelEdit}
          onSubmitEdit={(input) => onSubmitEdit(book.id, input)}
          onStatusChange={(s) => onStatusChange(book.id, s)}
          onDelete={() => onDelete(book.id)}
        />
      ))}
    </div>
  );
}
