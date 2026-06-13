import React from 'react';
import type { Article, SocketStatus } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  isModerator: boolean;
  setIsModerator: (v: boolean) => void;
  userName: string;
  setUserName: (v: string) => void;
  socketStatus: SocketStatus;
  onlineCount: number;
  onOpenNotesMobile?: () => void;
}

const STATUS_META: Record<SocketStatus, { label: string; cls: string; dot: string }> = {
  connecting: { label: '连接中', cls: 'status--connecting', dot: '' },
  open:       { label: '已连接', cls: 'status--open',       dot: '' },
  closed:     { label: '已断开', cls: 'status--closed',     dot: '' },
  error:      { label: '连接错误', cls: 'status--error',    dot: '' }
};

const ModeratorBar: React.FC<Props> = ({
  article, isModerator, setIsModerator,
  userName, setUserName, socketStatus, onlineCount, onOpenNotesMobile
}) => {
  const currentIdx = article ? article.paragraphs.findIndex(p => p.id === article.currentParagraphId) : -1;
  const total = article?.paragraphs.length || 0;
  const canPrev = currentIdx > 0;
  const canNext = currentIdx >= 0 && currentIdx < total - 1;

  const status = STATUS_META[socketStatus];

  return (
    <header className="topbar">
      <div className="topbar__brand">
        <span className="topbar__logo">📚</span>
        <div className="topbar__title-group">
          <h1 className="topbar__title">共读标注板</h1>
          <span className="topbar__subtitle">线上读书会 · 实时互动</span>
        </div>
      </div>

      <div className="topbar__center">
        <div className={['status-chip', status.cls].join(' ')}>
          <span className="status-chip__dot" />
          <span>{status.label}</span>
          {onlineCount > 0 && (
            <span className="status-chip__count">👥 {onlineCount}</span>
          )}
        </div>

        {isModerator && article && (
          <div className="moderator-ctrl">
            <span className="moderator-ctrl__label">主持人控制</span>
            <div className="moderator-ctrl__btns">
              <button
                className="btn btn--ghost"
                disabled={!canPrev}
                onClick={() => eventBus.emit('REQUEST_MOVE_PREV', undefined as any)}
              >
                ← 上一段
              </button>
              <span className="moderator-ctrl__idx">
                {currentIdx >= 0 ? `第 ${currentIdx + 1} / ${total} 段` : '--'}
              </span>
              <button
                className="btn btn--ghost"
                disabled={!canNext}
                onClick={() => eventBus.emit('REQUEST_MOVE_NEXT', undefined as any)}
              >
                下一段 →
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="topbar__right">
        <label className="name-input">
          <span className="name-input__label">昵称</span>
          <input
            type="text"
            value={userName}
            maxLength={12}
            placeholder="输入昵称…"
            onChange={e => setUserName(e.target.value)}
          />
        </label>

        <label className="switch-wrap" title="切换主持人身份">
          <input
            type="checkbox"
            checked={isModerator}
            onChange={e => setIsModerator(e.target.checked)}
          />
          <span className="switch-wrap__slider" />
          <span className="switch-wrap__text">主持人模式</span>
        </label>

        <button className="icon-btn notes-toggle" onClick={onOpenNotesMobile} aria-label="打开批注">
          💬
        </button>
      </div>
    </header>
  );
};

export default ModeratorBar;
