import React, { useEffect, useRef } from 'react';
import type { Article, Paragraph, Note } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  notes: Note[];
  noteCounts: Record<string, number>;
  currentParagraphId: string | null;
  highlightParagraphId: string | null;
  isModerator: boolean;
}

const ArticleView: React.FC<Props> = ({
  article, notes, noteCounts, currentParagraphId, highlightParagraphId, isModerator
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const paragraphRefs = useRef<Record<string, HTMLDivElement | null>>({});

  useEffect(() => {
    if (currentParagraphId && paragraphRefs.current[currentParagraphId]) {
      const el = paragraphRefs.current[currentParagraphId]!;
      setTimeout(() => {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 150);
    }
  }, [currentParagraphId]);

  if (!article) {
    return (
      <div className="reader-empty">
        <div className="reader-empty__spinner" />
        <p>正在加载文章…</p>
      </div>
    );
  }

  const handleParagraphClick = (p: Paragraph) => {
    if (isModerator) {
      eventBus.emit('REQUEST_SWITCH_PARAGRAPH', p.id);
    }
  };

  const currentIndex = article.paragraphs.findIndex(p => p.id === currentParagraphId);

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
      </header>

      <div className="reader__body">
        {article.paragraphs.map((p) => {
          const isCurrent = p.id === currentParagraphId;
          const isHighlight = p.id === highlightParagraphId;
          const count = noteCounts[p.id] || 0;
          const paraNotes = notes.filter(n => n.paragraphId === p.id);
          const hasHighlightNote = paraNotes.some(n => n.highlighted);
          return (
            <div
              key={p.id}
              ref={el => { paragraphRefs.current[p.id] = el; }}
              className={[
                'paragraph',
                isCurrent ? 'paragraph--current' : '',
                isHighlight ? 'paragraph--flash' : '',
                isModerator ? 'paragraph--clickable' : ''
              ].filter(Boolean).join(' ')}
              onClick={() => handleParagraphClick(p)}
              data-paragraph-id={p.id}
            >
              <span className="paragraph__marker">{p.index + 1}</span>
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
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ArticleView;
