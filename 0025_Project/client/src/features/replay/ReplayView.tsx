import React, { useMemo } from 'react';
import type { TimelineEvent, Article, Note } from '../../core/types';

interface Props {
  timeline: TimelineEvent[];
  article: Article | null;
  notes: Note[];
  currentIndex: number;
  setCurrentIndex: (n: number) => void;
  onExit: () => void;
  roomName: string;
}

const typeLabel: Record<string, string> = {
  JOIN: '👋 加入',
  LEAVE: '🚪 离开',
  NOTE_ADDED: '📝 批注',
  REPLY_ADDED: '💬 回复',
  LIKE: '👍 点赞',
  HIGHLIGHT: '⭐ 标重点',
  PARAGRAPH_SWITCH: '📖 切段',
  DISCUSSION_QUEUE_UPDATED: '📋 讨论队列',
  TYPING_START: '⌨️ 开始输入',
  TYPING_END: '⌨️ 停止输入',
  ARTICLE_UPDATED: '📄 文章更新',
};

const formatTime = (ts: number) => {
  const d = new Date(ts);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
};

const ReplayView: React.FC<Props> = ({ timeline, article, notes, currentIndex, setCurrentIndex, onExit, roomName }) => {
  const visibleEvents = useMemo(() => timeline.slice(0, currentIndex + 1), [timeline, currentIndex]);
  const currentEvent = timeline[currentIndex];

  const highlightedParagraphId = useMemo(() => {
    for (let i = visibleEvents.length - 1; i >= 0; i--) {
      if (visibleEvents[i].type === 'PARAGRAPH_SWITCH' && visibleEvents[i].data) {
        return (visibleEvents[i].data as Record<string, unknown>).paragraphId as string;
      }
    }
    return article?.currentParagraphId || null;
  }, [visibleEvents, article]);

  const visibleNotes = useMemo(() => {
    const ids = new Set<string>();
    for (const e of visibleEvents) {
      if (e.type === 'NOTE_ADDED' && e.data) {
        ids.add((e.data as Record<string, unknown>).noteId as string);
      }
    }
    return notes.filter(n => ids.has(n.id));
  }, [visibleEvents, notes]);

  return (
    <div className="replay">
      <header className="replay__header">
        <div className="replay__title">
          <span className="replay__tag">🎬 回放模式</span>
          <h2>{roomName}</h2>
        </div>
        <button className="btn btn--ghost" onClick={onExit}>✕ 退出回放</button>
      </header>

      <div className="replay__controls">
        <button
          className="btn btn--ghost"
          disabled={currentIndex <= 0}
          onClick={() => setCurrentIndex(Math.max(0, currentIndex - 1))}
        >⏮ 上一步</button>
        <button
          className="btn btn--ghost"
          disabled={currentIndex >= timeline.length - 1}
          onClick={() => setCurrentIndex(Math.min(timeline.length - 1, currentIndex + 1))}
        >下一步 ⏭</button>
        <input
          type="range"
          min={0}
          max={Math.max(0, timeline.length - 1)}
          value={currentIndex}
          onChange={e => setCurrentIndex(parseInt(e.target.value, 10))}
          className="replay__slider"
        />
        <span className="replay__progress">{currentIndex + 1} / {timeline.length}</span>
      </div>

      <main className="replay__main">
        <section className="replay__reader">
          <div className="replay__article">
            <h3 className="replay__articletitle">{article?.title}</h3>
            {article?.author && <p className="replay__articleauthor">— {article.author}</p>}
            <div className="replay__paragraphs">
              {article?.paragraphs.map(p => (
                <div
                  key={p.id}
                  className={`replay__paragraph ${highlightedParagraphId === p.id ? 'replay__paragraph--hl' : ''}`}
                >
                  <div className="replay__paraindex">第 {p.index + 1} 段</div>
                  <p>{p.content}</p>
                  {visibleNotes.filter(n => n.paragraphId === p.id).map(n => (
                    <div key={n.id} className={`replay__note replay__note--${n.type.toLowerCase()}`}>
                      <span className="replay__noteauthor">{n.author}</span>
                      <span className="replay__notetype">
                        {n.type === 'THOUGHT' ? '💭 想法' : n.type === 'QUESTION' ? '❓ 问题' : '📝 补充'}
                      </span>
                      <p>{n.content}</p>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        </section>

        <aside className="replay__timeline">
          <h4>📜 时间线</h4>
          <div className="replay__eventlist">
            {timeline.map((e, idx) => (
              <div
                key={idx}
                className={`replay__event ${idx === currentIndex ? 'replay__event--current' : ''} ${idx < currentIndex ? 'replay__event--past' : ''}`}
                onClick={() => setCurrentIndex(idx)}
              >
                <span className="replay__eventtime">{formatTime(e.timestamp)}</span>
                <span className="replay__eventtype">{typeLabel[e.type] || e.type}</span>
                <span className="replay__eventuser">{e.userName}</span>
              </div>
            ))}
          </div>
        </aside>
      </main>

      {currentEvent && (
        <div className="replay__currentevent">
          <span className="replay__currenttime">{formatTime(currentEvent.timestamp)}</span>
          <span className="replay__currenttype">{typeLabel[currentEvent.type]}</span>
          <span className="replay__currentuser"><b>{currentEvent.userName}</b></span>
          {currentEvent.data && (
            <span className="replay__currentdata">
              {typeof currentEvent.data === 'object'
                ? Object.entries(currentEvent.data as Record<string, unknown>)
                    .filter(([k]) => k !== 'noteId' && k !== 'paragraphId')
                    .map(([k, v]) => `${k}: ${typeof v === 'string' && v.length > 40 ? v.slice(0, 40) + '…' : v}`)
                    .join(' | ')
                : String(currentEvent.data)}
            </span>
          )}
        </div>
      )}
    </div>
  );
};

export default ReplayView;
