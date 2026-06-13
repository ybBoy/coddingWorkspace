import React, { useMemo, useState } from 'react';
import type { Note, NoteType, Paragraph, Article } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  currentParagraph: Paragraph | null;
  selectedParagraph: Paragraph | null;
  setSelectedParagraphId: (id: string) => void;
  notes: Note[];
  userName: string;
  isModerator: boolean;
  mobileOpen: boolean;
  onMobileClose?: () => void;
}

const TYPE_LABEL: Record<NoteType, { label: string; icon: string; cls: string }> = {
  THOUGHT:    { label: '想法', icon: '💭', cls: 'note--thought' },
  QUESTION:   { label: '问题', icon: '❓', cls: 'note--question' },
  SUPPLEMENT: { label: '补充', icon: '📌', cls: 'note--supplement' }
};

const NotePanel: React.FC<Props> = ({
  article, currentParagraph, selectedParagraph, setSelectedParagraphId,
  notes, userName, isModerator, mobileOpen, onMobileClose
}) => {
  const [content, setContent] = useState('');
  const [type, setType] = useState<NoteType>('THOUGHT');
  const [submitting, setSubmitting] = useState(false);

  const targetParagraph = selectedParagraph || currentParagraph;

  const paraNotes = useMemo(() => {
    if (!targetParagraph) return [];
    return notes
      .filter(n => n.paragraphId === targetParagraph.id)
      .sort((a, b) => {
        if (a.highlighted !== b.highlighted) return a.highlighted ? -1 : 1;
        const lc = (b.likes?.length || 0) - (a.likes?.length || 0);
        if (lc !== 0) return lc;
        return a.createdAt - b.createdAt;
      });
  }, [notes, targetParagraph]);

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!targetParagraph || !content.trim() || !userName.trim()) return;
    setSubmitting(true);
    eventBus.emit('REQUEST_ADD_NOTE', {
      paragraphId: targetParagraph.id,
      content: content.trim(),
      type
    });
    setTimeout(() => {
      setContent('');
      setSubmitting(false);
    }, 200);
  };

  const canSubmit = targetParagraph && content.trim() && userName.trim() && !submitting;

  return (
    <aside className={['notes', mobileOpen ? 'notes--mobile-open' : ''].filter(Boolean).join(' ')}>
      <div className="notes__header">
        <div>
          <h2 className="notes__title">批注 · 共议</h2>
          {targetParagraph && (
            <p className="notes__subtitle">
              第 {targetParagraph.index + 1} 段 · {paraNotes.length} 条
            </p>
          )}
        </div>
        {onMobileClose && (
          <button className="icon-btn notes__close" onClick={onMobileClose} aria-label="关闭">
            ✕
          </button>
        )}
      </div>

      {targetParagraph && article ? (
        <>
          <div className="notes__selector">
            <label className="notes__selector-label">批注段落</label>
            <select
              className="notes__selector-select"
              value={targetParagraph.id}
              onChange={e => setSelectedParagraphId(e.target.value)}
            >
              {article.paragraphs.map(p => (
                <option key={p.id} value={p.id}>
                  第 {p.index + 1} 段
                  {p.id === article.currentParagraphId ? '（共读中）' : ''}
                  ：{p.content.slice(0, 24)}{p.content.length > 24 ? '…' : ''}
                </option>
              ))}
            </select>
          </div>

          <div className={['notes__current-para', targetParagraph.id !== article.currentParagraphId ? 'notes__current-para--noncurrent' : ''].join(' ')}>
            <span className="notes__current-label">
              {targetParagraph.id === article.currentParagraphId ? '当前共读' : '已选段落'}
            </span>
            <p className="notes__current-text">「{targetParagraph.content}」</p>
          </div>

          <form className="notes__form" onSubmit={handleSubmit}>
            <div className="notes__type-group" role="radiogroup" aria-label="批注类型">
              {(Object.keys(TYPE_LABEL) as NoteType[]).map(t => (
                <label
                  key={t}
                  className={['type-btn', type === t ? 'type-btn--active' : '', TYPE_LABEL[t].cls.replace('note--', 'type-btn--')].filter(Boolean).join(' ')}
                >
                  <input
                    type="radio"
                    name="noteType"
                    value={t}
                    checked={type === t}
                    onChange={() => setType(t)}
                  />
                  <span>{TYPE_LABEL[t].icon} {TYPE_LABEL[t].label}</span>
                </label>
              ))}
            </div>
            <textarea
              className="notes__textarea"
              placeholder={userName ? `给第 ${targetParagraph.index + 1} 段写下你的批注…` : '请先在上方输入昵称'}
              value={content}
              onChange={e => setContent(e.target.value)}
              rows={3}
              disabled={!userName}
            />
            <div className="notes__form-footer">
              <span className="notes__char-count">{content.length} / 500</span>
              <button
                className="btn btn--primary"
                type="submit"
                disabled={!canSubmit}
              >
                发布批注
              </button>
            </div>
          </form>

          <div className="notes__list">
            {paraNotes.length === 0 ? (
              <div className="notes__empty">
                <span className="notes__empty-icon">✨</span>
                <p>这段还没有批注，来发布第一条吧</p>
              </div>
            ) : (
              paraNotes.map(note => {
                const meta = TYPE_LABEL[note.type];
                const liked = (note.likes || []).includes(userName);
                return (
                  <article
                    key={note.id}
                    className={['note', meta.cls, note.highlighted ? 'note--highlighted' : ''].filter(Boolean).join(' ')}
                  >
                    {note.highlighted && (
                      <div className="note__focus-tag">⭐ 重点讨论</div>
                    )}
                    <header className="note__header">
                      <div className="note__author">
                        <span className="note__avatar">
                          {note.author ? note.author.slice(0, 1) : '?'}
                        </span>
                        <div>
                          <strong>{note.author || '匿名'}</strong>
                          <span className={['note__type-tag', `note__type-tag--${note.type.toLowerCase()}`].join(' ')}>
                            {meta.icon} {meta.label}
                          </span>
                        </div>
                      </div>
                      {isModerator && (
                        <button
                          className={['toggle-btn', note.highlighted ? 'toggle-btn--on' : ''].join(' ')}
                          onClick={() => eventBus.emit('REQUEST_HIGHLIGHT', note.id)}
                          title={note.highlighted ? '取消重点' : '标记为重点讨论'}
                        >
                          {note.highlighted ? '★ 重点' : '☆ 标重点'}
                        </button>
                      )}
                    </header>
                    <p className="note__content">{note.content}</p>
                    <footer className="note__footer">
                      <button
                        className={['like-btn', liked ? 'like-btn--liked' : ''].join(' ')}
                        onClick={() => eventBus.emit('REQUEST_LIKE', note.id)}
                        disabled={!userName}
                      >
                        <span className="like-btn__icon">{liked ? '❤️' : '🤍'}</span>
                        <span className="like-btn__count">{note.likes?.length || 0}</span>
                      </button>
                      <time className="note__time">
                        {new Date(note.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
                      </time>
                    </footer>
                  </article>
                );
              })
            )}
          </div>
        </>
      ) : (
        <div className="notes__empty">
          <span className="notes__empty-icon">📖</span>
          <p>等待共读开始…</p>
        </div>
      )}
    </aside>
  );
};

export default NotePanel;
