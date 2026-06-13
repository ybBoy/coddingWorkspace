import { useState } from 'react';
import type { BookInput, ReadingStatus } from '../types/book';
import { STATUS_LABELS } from '../types/book';

interface BookFormProps {
  onSubmit: (input: BookInput) => Promise<void> | void;
}

export default function BookForm({ onSubmit }: BookFormProps) {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [status, setStatus] = useState<ReadingStatus>('TO_READ');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !author.trim()) return;
    setSubmitting(true);
    try {
      await onSubmit({ title: title.trim(), author: author.trim(), status, remark: remark.trim() });
      setTitle('');
      setAuthor('');
      setStatus('TO_READ');
      setRemark('');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="book-form" onSubmit={handleSubmit}>
      <h2>添加书籍</h2>
      <div className="form-group">
        <label>书名</label>
        <input
          type="text" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="请输入书名" required />
      </div>
      <div className="form-group">
        <label>作者</label>
        <input
          type="text" value={author} onChange={(e) => setAuthor(e.target.value)} placeholder="请输入作者" required />
      </div>
      <div className="form-group">
        <label>阅读状态</label>
        <select value={status} onChange={(e) => setStatus(e.target.value as ReadingStatus)}>
          {(Object.keys(STATUS_LABELS) as ReadingStatus[]).map((s) => (
            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
          ))}
        </select>
      </div>
      <div className="form-group">
        <label>备注</label>
        <textarea
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          placeholder="一句简短备注"
          rows={3}
        />
      </div>
      <button type="submit" disabled={submitting}>
        {submitting ? '添加中...' : '添加书籍'}
      </button>
    </form>
  );
}
