import React, { useMemo, useState } from 'react';
import type { Note, NoteType, Paragraph, Article, Reply } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  currentParagraph: Paragraph | null;
  selectedParagraph: Paragraph | null;
  setSelectedParagraphId: (id: string) => void;
  notes: Note[];
  repliesByNote: Record<string, Reply[]>;
  discussionQueue: string[];
  userName: string;
  isModerator: boolean;
  isOwner: boolean;
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
  notes, repliesByNote, discussionQueue,
  userName, isModerator, isOwner, mobileOpen, onMobileClose
}) => {
  const [content, setContent] = useState('');
  const [type, setType] = useState<NoteType>('THOUGHT');
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState<'notes' | 'queue' | 'import'>('notes');
  const [replyForNote, setReplyForNote] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [importTitle, setImportTitle] = useState('');
  const [importAuthor, setImportAuthor] = useState('');
  const [importText, setImportText] = useState('');
  const [dragOverId, setDragOverId] = useState<string | null>(null);
  const [onlyQuestions, setOnlyQuestions] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const targetParagraph = selectedParagraph || currentParagraph;

  const paraNotes = useMemo(() => {
    if (!targetParagraph) return [];
    return notes
      .filter(n => n.paragraphId === targetParagraph.id)
      .filter(n => !onlyQuestions || n.type === 'QUESTION')
      .sort((a, b) => {
        if (a.highlighted !== b.highlighted) return a.highlighted ? -1 : 1;
        const lc = (b.likes?.length || 0) - (a.likes?.length || 0);
        if (lc !== 0) return lc;
        return a.createdAt - b.createdAt;
      });
  }, [notes, targetParagraph, onlyQuestions]);

  const queueNotes = useMemo(() => {
    return discussionQueue
      .map(id => notes.find(n => n.id === id))
      .filter((n): n is Note => !!n);
  }, [discussionQueue, notes]);

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!targetParagraph || !content.trim() || !userName.trim()) return;
    setSubmitting(true);
    eventBus.emit('REQUEST_ADD_NOTE', {
      paragraphId: targetParagraph.id,
      content: content.trim(),
      type
    });
    eventBus.emit('REQUEST_PRESENCE', { typing: false });
    setTimeout(() => {
      setContent('');
      setSubmitting(false);
    }, 200);
  };

  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    if (e.target.value.length === 1) {
      eventBus.emit('REQUEST_PRESENCE', { typing: true });
    }
  };

  const handleSubmitReply = (noteId: string) => {
    if (!replyText.trim() || !userName) return;
    eventBus.emit('REQUEST_ADD_REPLY', { noteId, content: replyText.trim() });
    setReplyText('');
    setReplyForNote(null);
  };

  const handleImport = () => {
    if (!importText.trim()) {
      alert('请粘贴文章内容');
      return;
    }
    eventBus.emit('REQUEST_IMPORT_ARTICLE', {
      title: importTitle.trim() || undefined,
      author: importAuthor.trim() || undefined,
      text: importText
    });
    setImportTitle('');
    setImportAuthor('');
    setImportText('');
    setActiveTab('notes');
  };

  const handleAddToQueue = (noteId: string) => {
    eventBus.emit('REQUEST_ADD_TO_QUEUE', noteId);
  };

  const handleRemoveFromQueue = (noteId: string) => {
    eventBus.emit('REQUEST_REMOVE_FROM_QUEUE', noteId);
  };

  const handleDragStart = (e: React.DragEvent, noteId: string) => {
    e.dataTransfer.setData('text/plain', noteId);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, noteId: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverId(noteId);
  };

  const handleDragLeave = () => {
    setDragOverId(null);
  };

  const handleDrop = (e: React.DragEvent, targetId: string) => {
    e.preventDefault();
    const draggedId = e.dataTransfer.getData('text/plain');
    setDragOverId(null);
    if (!draggedId || draggedId === targetId) return;
    const newOrder = [...discussionQueue];
    const from = newOrder.indexOf(draggedId);
    const to = newOrder.indexOf(targetId);
    if (from < 0 || to < 0) return;
    newOrder.splice(from, 1);
    newOrder.splice(to, 0, draggedId);
    eventBus.emit('REQUEST_REORDER_QUEUE', newOrder);
  };

  const handleCopyContent = async (note: Note) => {
    const text = `[${TYPE_LABEL[note.type].label}] ${note.author}：${note.content}`;
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(note.id);
      setTimeout(() => setCopiedId(null), 1500);
    } catch (e) {
      console.warn('复制失败', e);
    }
  };

  const handleClearNotes = () => {
    if (!targetParagraph) return;
    const count = paraNotes.length;
    if (count === 0) return;
    if (!confirm(`确定要清空第 ${targetParagraph.index + 1} 段的 ${count} 条批注吗？（同时会清空关联的回复和队列）`)) return;
    eventBus.emit('REQUEST_CLEAR_NOTES_PARAGRAPH', { paragraphId: targetParagraph.id });
  };

  const canSubmit = targetParagraph && content.trim() && userName.trim() && !submitting;

  return (
    <aside className={['notes', mobileOpen ? 'notes--mobile-open' : ''].filter(Boolean).join(' ')}>
      <div className="notes__header">
        <div className="notes__tabs">
          <button
            className={['notes__tab', activeTab === 'notes' ? 'notes__tab--active' : ''].join(' ')}
            onClick={() => setActiveTab('notes')}
          >💬 批注</button>
          <button
            className={['notes__tab', activeTab === 'queue' ? 'notes__tab--active' : ''].join(' ')}
            onClick={() => setActiveTab('queue')}
          >📋 讨论队列 {discussionQueue.length > 0 && `(${discussionQueue.length})`}</button>
          {(isModerator || isOwner) && (
            <button
              className={['notes__tab', activeTab === 'import' ? 'notes__tab--active' : ''].join(' ')}
              onClick={() => setActiveTab('import')}
            >📄 导入</button>
          )}
        </div>
        {onMobileClose && (
          <button className="icon-btn notes__close" onClick={onMobileClose} aria-label="关闭">
            ✕
          </button>
        )}
      </div>

      {activeTab === 'notes' && targetParagraph && article ? (
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
              onChange={handleContentChange}
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

          <div className="notes__filters">
            <label className="filter-checkbox">
              <input
                type="checkbox"
                checked={onlyQuestions}
                onChange={e => setOnlyQuestions(e.target.checked)}
              />
              <span>❓ 只看问题</span>
            </label>
            {(isModerator || isOwner) && paraNotes.length > 0 && (
              <button className="btn btn--ghost btn--xs btn--warn" onClick={handleClearNotes}>
                🗑 清空本段批注
              </button>
            )}
          </div>

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
                const noteReplies = repliesByNote[note.id] || [];
                const inQueue = discussionQueue.includes(note.id);
                return (
                  <article
                    key={note.id}
                    className={['note', meta.cls, note.highlighted ? 'note--highlighted' : '', inQueue ? 'note--inqueue' : ''].filter(Boolean).join(' ')}
                  >
                    {note.highlighted && (
                      <div className="note__focus-tag">⭐ 重点讨论</div>
                    )}
                    {inQueue && (
                      <div className="note__queue-tag">📋 队列 #{discussionQueue.indexOf(note.id) + 1}</div>
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
                      <div className="note__actions">
                        <button
                          className={['btn', 'btn--ghost', 'btn--xs', copiedId === note.id ? 'btn--primary' : ''].filter(Boolean).join(' ')}
                          onClick={() => handleCopyContent(note)}
                          title="复制批注内容"
                        >
                          {copiedId === note.id ? '✓ 已复制' : '📋 复制'}
                        </button>
                        {isModerator && (
                          <>
                            {!inQueue ? (
                              <button
                                className="btn btn--ghost btn--xs"
                                onClick={() => handleAddToQueue(note.id)}
                                title="加入讨论队列"
                              >📋 入队</button>
                            ) : (
                              <button
                                className="btn btn--ghost btn--xs btn--warn"
                                onClick={() => handleRemoveFromQueue(note.id)}
                                title="移出讨论队列"
                              >移出队列</button>
                            )}
                            <button
                              className={['toggle-btn', note.highlighted ? 'toggle-btn--on' : ''].join(' ')}
                              onClick={() => eventBus.emit('REQUEST_HIGHLIGHT', note.id)}
                              title={note.highlighted ? '取消重点' : '标记为重点讨论'}
                            >
                              {note.highlighted ? '★' : '☆'}
                            </button>
                          </>
                        )}
                      </div>
                    </header>
                    <p className="note__content">{note.content}</p>

                    {noteReplies.length > 0 && (
                      <div className="note__replies">
                        {noteReplies.map(r => {
                          const rliked = (r.likes || []).includes(userName);
                          return (
                            <div key={r.id} className="note-reply">
                              <strong className="note-reply__author">{r.author}</strong>
                              <span className="note-reply__text">{r.content}</span>
                              <button
                                className={['note-reply__like', rliked ? 'note-reply__like--on' : ''].join(' ')}
                                onClick={() => eventBus.emit('REQUEST_LIKE_REPLY', r.id)}
                                disabled={!userName}
                              >
                                {rliked ? '❤️' : '🤍'} {r.likes?.length || 0}
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {replyForNote === note.id ? (
                      <div className="note__replyform">
                        <input
                          type="text"
                          placeholder="回复这条批注…"
                          value={replyText}
                          onChange={e => setReplyText(e.target.value)}
                          autoFocus
                          onKeyDown={e => { if (e.key === 'Enter') handleSubmitReply(note.id); }}
                        />
                        <button className="btn btn--primary btn--sm" onClick={() => handleSubmitReply(note.id)} disabled={!replyText.trim()}>
                          回复
                        </button>
                        <button className="btn btn--ghost btn--sm" onClick={() => { setReplyForNote(null); setReplyText(''); }}>
                          取消
                        </button>
                      </div>
                    ) : (
                      <button
                        className="note__replybtn"
                        onClick={() => { setReplyForNote(note.id); setReplyText(''); }}
                        disabled={!userName}
                      >
                        💬 回复 {noteReplies.length > 0 ? `(${noteReplies.length})` : ''}
                      </button>
                    )}

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
      ) : activeTab === 'queue' ? (
        <div className="queue">
          <div className="queue__header">
            <h3>📋 讨论队列</h3>
            <p className="queue__hint">{isModerator ? '拖拽可调整顺序' : '主持人可管理队列'}</p>
          </div>
          {queueNotes.length === 0 ? (
            <div className="notes__empty">
              <span className="notes__empty-icon">📋</span>
              <p>暂无重点批注加入队列</p>
              {isModerator && <p className="queue__tip">在批注卡片点击「📋 入队」添加</p>}
            </div>
          ) : (
            <div className="queue__list">
              {queueNotes.map((note, idx) => {
                const meta = TYPE_LABEL[note.type];
                return (
                  <div
                    key={note.id}
                    className={['queue-item', dragOverId === note.id ? 'queue-item--over' : '', isModerator ? 'queue-item--draggable' : ''].join(' ')}
                    draggable={isModerator}
                    onDragStart={e => handleDragStart(e, note.id)}
                    onDragOver={e => handleDragOver(e, note.id)}
                    onDragLeave={handleDragLeave}
                    onDrop={e => handleDrop(e, note.id)}
                  >
                    <span className="queue-item__index">{idx + 1}</span>
                    <div className="queue-item__body">
                      <div className="queue-item__head">
                        <strong>{note.author}</strong>
                        <span className="queue-item__type">{meta.icon}</span>
                      </div>
                      <p className="queue-item__content">{note.content.length > 80 ? note.content.slice(0, 80) + '…' : note.content}</p>
                    </div>
                    {isModerator && (
                      <button
                        className="queue-item__remove"
                        onClick={() => handleRemoveFromQueue(note.id)}
                        title="移出队列"
                      >✕</button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : activeTab === 'import' && (isModerator || isOwner) ? (
        <div className="import">
          <div className="import__header">
            <h3>📄 导入短文</h3>
            <p className="import__hint">粘贴文字，空行分段</p>
          </div>
          <div className="import__form">
            <label>
              <span>标题（可选）</span>
              <input
                type="text"
                value={importTitle}
                onChange={e => setImportTitle(e.target.value)}
                placeholder="文章标题"
              />
            </label>
            <label>
              <span>作者（可选）</span>
              <input
                type="text"
                value={importAuthor}
                onChange={e => setImportAuthor(e.target.value)}
                placeholder="作者名"
              />
            </label>
            <label>
              <span>正文（空行分段）</span>
              <textarea
                value={importText}
                onChange={e => setImportText(e.target.value)}
                placeholder="粘贴文章内容，段落之间用空行分隔…"
                rows={10}
              />
            </label>
            <div className="import__actions">
              <button className="btn btn--ghost" onClick={() => { setImportTitle(''); setImportAuthor(''); setImportText(''); }}>
                清空
              </button>
              <button className="btn btn--primary" onClick={handleImport} disabled={!importText.trim()}>
                导入并替换
              </button>
            </div>
          </div>
        </div>
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
