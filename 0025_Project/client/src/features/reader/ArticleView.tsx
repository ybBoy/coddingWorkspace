import React, { useEffect, useMemo, useRef, useState } from 'react';
import type { Article, Paragraph, Note, NoteType } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  notes: Note[];
  noteCounts: Record<string, number>;
  currentParagraphId: string | null;
  highlightParagraphId: string | null;
  selectedParagraphId: string | null;
  setSelectedParagraphId: (id: string) => void;
  isModerator: boolean;
  userName: string;
}

const TYPE_META: Record<NoteType, { icon: string; cls: string }> = {
  THOUGHT:    { icon: '💭', cls: 'inline-note--thought' },
  QUESTION:   { icon: '❓', cls: 'inline-note--question' },
  SUPPLEMENT: { icon: '📌', cls: 'inline-note--supplement' }
};

const MAX_INLINE_PREVIEW = 2;

const ArticleView: React.FC<Props> = ({
  article, notes, noteCounts,
  currentParagraphId, highlightParagraphId,
  selectedParagraphId, setSelectedParagraphId,
  isModerator, userName
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const paragraphRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const [expandedMap, setExpandedMap] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (currentParagraphId && paragraphRefs.current[currentParagraphId]) {
      const el = paragraphRefs.current[currentParagraphId]!;
      setTimeout(() => {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 150);
    }
  }, [currentParagraphId]);

  const toggleExpand = (pid: string) => {
    setExpandedMap(prev => ({ ...prev, [pid]: !prev[pid] }));
  };

  const handleAddNote = (p: Paragraph, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedParagraphId(p.id);
    eventBus.emit('SELECT_PARAGRAPH_FOR_NOTE', p.id);
  };

  const handleParagraphClick = (p: Paragraph) => {
    if (isModerator) {
      eventBus.emit('REQUEST_SWITCH_PARAGRAPH', p.id);
    } else {
      setSelectedParagraphId(p.id);
    }
  };

  const handleInlineLike = (noteId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!userName) return;
    eventBus.emit('REQUEST_LIKE', noteId);
  };

  if (!article) {
    return (
      <div className="reader-empty">
        <div className="reader-empty__spinner" />
        <p>正在加载文章…</p>
      </div>
    );
  }

  const currentIndex = article.paragraphs.findIndex(p => p.id === currentParagraphId);

  const getParagraphNotes = (pid: string) =>
    useMemoEmpty() || notes
      .filter(n => n.paragraphId === pid)
      .sort((a, b) => {
        if (a.highlighted !== b.highlighted) return a.highlighted ? -1 : 1;
        const lc = (b.likes?.length || 0) - (a.likes?.length || 0);
        if (lc !== 0) return lc;
        return a.createdAt - b.createdAt;
      });

  return (
    <div className="reader" ref={containerRef}>
      <header className="reader__header">
        <h1 className="reader__title">{article.title}</h1>
        {article.author && <p className="reader__author">—— {article.author}</p>}
        <div className="reader__progress">
          <div
            className="reader__progress-bar"
            style={{ width: `${((currentIndex + 1) / article.paragraphs.length) * 100}%` }}
          />
          <span className="reader__progress-text">
            第 {currentIndex + 1} / {article.paragraphs.length} 段
          </span>
        </div>
        <p className="reader__hint">
          {isModerator
            ? '💡 主持人：点击段落可切换当前共读位置；点击💬可批注'
            : '💡 点击任意段落查看批注，点击💬发布新批注'}
        </p>
      </header>

      <div className="reader__body">
        {article.paragraphs.map((p) => {
          const isCurrent = p.id === currentParagraphId;
          const isHighlight = p.id === highlightParagraphId;
          const isSelected = p.id === selectedParagraphId;
          const count = noteCounts[p.id] || 0;
          const paraNotes = getParagraphNotes(p.id);
          const hasHighlightNote = paraNotes.some(n => n.highlighted);
          const expanded = expandedMap[p.id] || false;
          const visibleNotes = expanded ? paraNotes : paraNotes.slice(0, MAX_INLINE_PREVIEW);
          const hiddenCount = paraNotes.length - visibleNotes.length;

          return (
            <div
              key={p.id}
              ref={el => { paragraphRefs.current[p.id] = el; }}
              className={[
                'paragraph',
                isCurrent ? 'paragraph--current' : '',
                isHighlight ? 'paragraph--flash' : '',
                isSelected ? 'paragraph--selected' : '',
                isModerator ? 'paragraph--clickable' : 'paragraph--selectable'
              ].filter(Boolean).join(' ')}
              onClick={() => handleParagraphClick(p)}
              data-paragraph-id={p.id}
            >
              <span className="paragraph__marker">{p.index + 1}</span>

              <div className="paragraph__main">
                <p className="paragraph__content">{p.content}</p>
                <div className="paragraph__meta">
                  {count > 0 && (
                    <span className="badge badge--count" title={`${count} 条批注`}>
                      💬 {count}
                    </span>
                  )}
                  {hasHighlightNote && (
                    <span className="badge badge--focus" title="含重点讨论">
                      ⭐ 重点
                    </span>
                  )}
                  {isCurrent && (
                    <span className="badge badge--current">
                      📖 共读中
                    </span>
                  )}
                  <button
                    className="paragraph__add-btn"
                    onClick={(e) => handleAddNote(p, e)}
                    title={`给第 ${p.index + 1} 段加批注`}
                  >
                    <span className="paragraph__add-icon">＋</span>
                    加批注
                  </button>
                </div>
              </div>

              {paraNotes.length > 0 && (
                <div className="paragraph__notes" onClick={e => e.stopPropagation()}>
                  <div className="paragraph__notes-header">
                    <span className="paragraph__notes-title">本段批注</span>
                    {paraNotes.length > MAX_INLINE_PREVIEW && (
                      <button
                        className="paragraph__notes-expand"
                        onClick={() => toggleExpand(p.id)}
                      >
                        {expanded ? '收起' : `展开全部 ${paraNotes.length} 条`}
                      </button>
                    )}
                  </div>
                  <div className="paragraph__notes-list">
                    {visibleNotes.map(note => {
                      const meta = TYPE_META[note.type];
                      const liked = (note.likes || []).includes(userName);
                      return (
                        <div
                          key={note.id}
                          className={[
                            'inline-note',
                            meta.cls,
                            note.highlighted ? 'inline-note--highlighted' : ''
                          ].filter(Boolean).join(' ')}
                        >
                          {note.highlighted && (
                            <span className="inline-note__focus">⭐ 重点</span>
                          )}
                          <div className="inline-note__body">
                            <div className="inline-note__head">
                              <strong className="inline-note__author">{note.author || '匿名'}</strong>
                              <span className="inline-note__type">
                                {meta.icon}
                              </span>
                            </div>
                            <p className="inline-note__content">{note.content}</p>
                          </div>
                          <button
                            className={['inline-note__like', liked ? 'inline-note__like--on' : ''].join(' ')}
                            onClick={(e) => handleInlineLike(note.id, e)}
                            disabled={!userName}
                            title={liked ? '取消点赞' : '点赞'}
                          >
                            {liked ? '❤️' : '🤍'}
                            <span>{note.likes?.length || 0}</span>
                          </button>
                        </div>
                      );
                    })}
                    {hiddenCount > 0 && !expanded && (
                      <button
                        className="paragraph__notes-more"
                        onClick={() => toggleExpand(p.id)}
                      >
                        还有 {hiddenCount} 条批注，点击展开 →
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

function useMemoEmpty(): null { return null; }

export default ArticleView;
