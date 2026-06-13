import React, { useMemo } from 'react';
import type { TimelineEvent, Article, Note, Reply } from '../../core/types';

interface Props {
  timeline: TimelineEvent[];
  article: Article | null;
  currentIndex: number;
  setCurrentIndex: (n: number) => void;
  onExit: () => void;
  roomName: string;
}

interface ReplayState {
  notes: Note[];
  replies: Reply[];
  discussionQueue: string[];
  highlightedParagraphId: string | null;
  usersRenamed: Record<string, string>;
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
  USER_RENAMED: '✏️ 改名',
};

const formatTime = (ts: number) => {
  const d = new Date(ts);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
};

const buildReplayState = (timeline: TimelineEvent[], upToIdx: number): ReplayState => {
  const state: ReplayState = {
    notes: [],
    replies: [],
    discussionQueue: [],
    highlightedParagraphId: null,
    usersRenamed: {},
  };

  const limit = Math.min(upToIdx, timeline.length - 1);
  for (let i = 0; i <= limit; i++) {
    const e = timeline[i];
    if (!e || !e.data) continue;
    const d = e.data as Record<string, unknown>;

    switch (e.type) {
      case 'USER_RENAMED': {
        const oldName = d.oldName as string;
        const newName = d.newName as string;
        if (oldName && newName) {
          state.usersRenamed[oldName] = newName;
          state.notes.forEach(n => { if (n.author === oldName) n.author = newName; });
          state.replies.forEach(r => { if (r.author === oldName) r.author = newName; });
        }
        break;
      }
      case 'NOTE_ADDED': {
        const noteId = d.noteId as string;
        if (noteId && !state.notes.find(n => n.id === noteId)) {
          state.notes.push({
            id: noteId,
            paragraphId: d.paragraphId as string,
            author: (d.author as string) || e.userName,
            content: d.content as string,
            type: (d.type as Note['type']) || 'THOUGHT',
            likes: [],
            highlighted: false,
            createdAt: (d.createdAt as number) || e.timestamp,
          });
        }
        break;
      }
      case 'REPLY_ADDED': {
        const replyId = d.replyId as string;
        if (replyId && !state.replies.find(r => r.id === replyId)) {
          state.replies.push({
            id: replyId,
            noteId: d.noteId as string,
            parentReplyId: (d.parentReplyId as string) || null,
            author: (d.author as string) || e.userName,
            content: d.content as string,
            likes: [],
            createdAt: (d.createdAt as number) || e.timestamp,
          });
        }
        break;
      }
      case 'LIKE': {
        const noteId = d.noteId as string;
        const replyId = d.replyId as string;
        const liked = d.liked as boolean;
        if (noteId) {
          const note = state.notes.find(n => n.id === noteId);
          if (note) {
            if (liked) {
              if (!note.likes.includes(e.userName)) note.likes.push(e.userName);
            } else {
              note.likes = note.likes.filter(u => u !== e.userName);
            }
          }
        }
        if (replyId) {
          const reply = state.replies.find(r => r.id === replyId);
          if (reply) {
            if (liked) {
              if (!reply.likes.includes(e.userName)) reply.likes.push(e.userName);
            } else {
              reply.likes = reply.likes.filter(u => u !== e.userName);
            }
          }
        }
        break;
      }
      case 'HIGHLIGHT': {
        const noteId = d.noteId as string;
        const highlighted = d.highlighted as boolean;
        const note = state.notes.find(n => n.id === noteId);
        if (note) note.highlighted = !!highlighted;
        break;
      }
      case 'PARAGRAPH_SWITCH': {
        state.highlightedParagraphId = (d.paragraphId as string) || null;
        break;
      }
      case 'DISCUSSION_QUEUE_UPDATED': {
        const q = d.discussionQueue as string[];
        if (Array.isArray(q)) state.discussionQueue = [...q];
        break;
      }
    }
  }

  if (!state.highlightedParagraphId) {
    state.highlightedParagraphId = timeline
      .slice(0, limit + 1)
      .filter(e => e.type === 'PARAGRAPH_SWITCH' && e.data)
      .map(e => (e.data as Record<string, unknown>).paragraphId as string)
      .pop() || null;
  }

  return state;
};

const ReplayView: React.FC<Props> = ({ timeline, article, currentIndex, setCurrentIndex, onExit, roomName }) => {
  const state = useMemo(() => buildReplayState(timeline, currentIndex), [timeline, currentIndex]);
  const currentEvent = timeline[currentIndex];

  const repliesByNote = useMemo(() => {
    const map: Record<string, Reply[]> = {};
    state.replies.forEach(r => {
      if (!map[r.noteId]) map[r.noteId] = [];
      map[r.noteId].push(r);
    });
    return map;
  }, [state.replies]);

  const queueSet = useMemo(() => new Set(state.discussionQueue), [state.discussionQueue]);

  return (
    <div className="replay">
      <header className="replay__header">
        <div className="replay__title">
          <span className="replay__tag">🎬 回放模式</span>
          <h2>{roomName}</h2>
          <span className="lobby__status lobby__status--open" style={{ fontSize: '11px', padding: '4px 10px' }}>
            📝 批注 {state.notes.length} · 💬 回复 {state.replies.length} · 📋 队列 {state.discussionQueue.length}
          </span>
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
                  className={`replay__paragraph ${state.highlightedParagraphId === p.id ? 'replay__paragraph--hl' : ''}`}
                >
                  <div className="replay__paraindex">
                    第 {p.index + 1} 段
                    {state.highlightedParagraphId === p.id && <span style={{ marginLeft: 10, color: 'var(--warn)', fontWeight: 700 }}>📖 当前共读段</span>}
                  </div>
                  <p>{p.content}</p>
                  {state.notes.filter(n => n.paragraphId === p.id).map(n => (
                    <div key={n.id} className={`replay__note replay__note--${n.type.toLowerCase()}`}
                         style={queueSet.has(n.id) ? { borderColor: '#6e5cb8', background: 'linear-gradient(180deg,#fffbe6 0%,#fff9d7 100%)' } : {}}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span className="replay__noteauthor">{n.author}</span>
                        <span className="replay__notetype">
                          {n.type === 'THOUGHT' ? '💭 想法' : n.type === 'QUESTION' ? '❓ 问题' : '📝 补充'}
                        </span>
                        {n.highlighted && <span style={{ color: 'var(--warn)', fontSize: 12, fontWeight: 700 }}>⭐ 重点</span>}
                        {queueSet.has(n.id) && (
                          <span style={{
                            marginLeft: 'auto',
                            padding: '1px 8px',
                            background: '#6e5cb8',
                            color: '#fff',
                            borderRadius: 999,
                            fontSize: 10,
                            fontWeight: 600,
                          }}>
                            队列 #{state.discussionQueue.indexOf(n.id) + 1}
                          </span>
                        )}
                      </div>
                      <p>{n.content}</p>
                      {n.likes.length > 0 && (
                        <div style={{ fontSize: 12, color: 'var(--danger)', marginTop: 4 }}>
                          👍 {n.likes.length}（{n.likes.join('、')}）
                        </div>
                      )}
                      {repliesByNote[n.id] && repliesByNote[n.id].length > 0 && (
                        <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px dashed var(--line)', display: 'flex', flexDirection: 'column', gap: 6 }}>
                          {repliesByNote[n.id].map(r => (
                            <div key={r.id} style={{ padding: '6px 10px', background: 'var(--bg-soft)', borderRadius: 'var(--radius-sm)', fontSize: 12 }}>
                              <b style={{ color: 'var(--accent-deep)' }}>{r.author}</b>
                              <span style={{ color: 'var(--ink-soft)', marginLeft: 6 }}>{r.content}</span>
                              {r.likes.length > 0 && (
                                <span style={{ color: 'var(--danger)', fontSize: 11, marginLeft: 6 }}>👍 {r.likes.length}</span>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
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
                <span className="replay__eventuser">{state.usersRenamed[e.userName] || e.userName}</span>
              </div>
            ))}
          </div>
        </aside>
      </main>

      {currentEvent && (
        <div className="replay__currentevent">
          <span className="replay__currenttime">{formatTime(currentEvent.timestamp)}</span>
          <span className="replay__currenttype">{typeLabel[currentEvent.type] || currentEvent.type}</span>
          <span className="replay__currentuser"><b>{state.usersRenamed[currentEvent.userName] || currentEvent.userName}</b></span>
          {currentEvent.data && (
            <span className="replay__currentdata">
              {typeof currentEvent.data === 'object'
                ? Object.entries(currentEvent.data as Record<string, unknown>)
                    .filter(([k]) => k !== 'noteId' && k !== 'paragraphId' && k !== 'replyId' && k !== 'createdAt')
                    .map(([k, v]) => `${k}: ${typeof v === 'string' && v.length > 40 ? v.slice(0, 40) + '…' : String(v)}`)
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
