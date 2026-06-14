import { useEffect, useState } from 'react';
import type { BookInput, ReadingStatus } from '../types/book';
import { STATUS_LABELS } from '../types/book';

interface BookFormProps {
  initialValue?: BookInput & { id?: string };
  submitLabel?: string;
  titleLabel?: string;
  onSubmit: (input: BookInput) => Promise<void> | void;
  onCancel?: () => void;
  formError?: string | null;
}

export default function BookForm({
  initialValue,
  submitLabel,
  titleLabel = '添加书籍',
  onSubmit,
  onCancel,
  formError
}: BookFormProps) {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [status, setStatus] = useState<ReadingStatus>('TO_READ');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    if (initialValue) {
      setTitle(initialValue.title || '');
      setAuthor(initialValue.author || '');
      setStatus(initialValue.status || 'TO_READ');
      setRemark(initialValue.remark || '');
    }
  }, [initialValue]);

  const validate = (): string | null => {
    if (!title.trim()) return '书名不能为空';
    if (!author.trim()) return '作者不能为空';
    if (!['TO_READ', 'READING', 'READ'].includes(status)) return '阅读状态非法';
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const err = validate();
    if (err) {
      setLocalError(err);
      return;
    }
    setLocalError(null);
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        author: author.trim(),
        status,
        remark: remark.trim()
      });
      if (!initialValue) {
        setTitle('');
        setAuthor('');
        setStatus('TO_READ');
        setRemark('');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="book-form" onSubmit={handleSubmit}>
      <h2>{titleLabel}</h2>
      <div className="form-group">
        <label>书名</label>
        <input
          type="text"
          value={title}
          maxLength={200}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="请输入书名"
          required
        />
      </div>
      <div className="form-group">
        <label>作者</label>
        <input
          type="text"
          value={author}
          maxLength={100}
          onChange={(e) => setAuthor(e.target.value)}
          placeholder="请输入作者"
          required
        />
      </div>
      <div className="form-group">
        <label>阅读状态</label>
        <select value={status} onChange={(e) => setStatus(e.target.value as ReadingStatus)}>
          {(Object.keys(STATUS_LABELS) as ReadingStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </div>
      <div className="form-group">
        <label>备注</label>
        <textarea
          value={remark}
          maxLength={500}
          onChange={(e) => setRemark(e.target.value)}
          placeholder="一句简短备注（选填）"
          rows={3}
        />
      </div>
      {(localError || formError) && (
        <div className="form-error">{localError || formError}</div>
      )}
      <div className="form-actions">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? '保存中...' : submitLabel || (initialValue ? '保存修改' : '添加书籍')}
        </button>
        {onCancel && (
          <button type="button" className="btn-secondary" onClick={onCancel} disabled={submitting}>
            取消
          </button>
        )}
      </div>
    </form>
  );
}
