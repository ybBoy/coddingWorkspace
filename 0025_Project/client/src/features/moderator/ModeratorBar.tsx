import React, { useState } from 'react';
import type { Article, SocketStatus, Presence } from '../../core/types';
import eventBus from '../../core/EventBus';

interface Props {
  article: Article | null;
  isModerator: boolean;
  isOwner: boolean;
  setIsModerator: (v: boolean) => void;
  moderators: string[];
  userName: string;
  setUserName: (v: string) => void;
  socketStatus: SocketStatus;
  onlineCount: number;
  onlineNames?: string[];
  typingUsers?: string[];
  presences?: Presence[];
  roomName: string;
  ownerName: string;
  roomId: string;
  onOpenNotesMobile?: () => void;
  onLeaveRoom?: () => void;
  onStartReplay?: () => void;
}

const STATUS_META: Record<SocketStatus, { label: string; cls: string }> = {
  connecting: { label: '连接中', cls: 'status--connecting' },
  open:       { label: '已连接', cls: 'status--open' },
  closed:     { label: '已断开', cls: 'status--closed' },
  error:      { label: '连接错误', cls: 'status--error' }
};

const ModeratorBar: React.FC<Props> = ({
  article, isModerator, isOwner, setIsModerator, moderators,
  userName, setUserName, socketStatus, onlineCount, onlineNames,
  typingUsers, roomName, ownerName, presences,
  onOpenNotesMobile, onLeaveRoom, onStartReplay
}) => {
  const [showMenu, setShowMenu] = useState(false);
  const [showModPanel, setShowModPanel] = useState(false);
  const currentIdx = article ? article.paragraphs.findIndex(p => p.id === article.currentParagraphId) : -1;
  const total = article?.paragraphs.length || 0;
  const canPrev = currentIdx > 0;
  const canNext = currentIdx >= 0 && currentIdx < total - 1;
  const status = STATUS_META[socketStatus];
  const currentModeratorName = moderators?.[0];

  const typingText = typingUsers && typingUsers.length > 0
    ? typingUsers.slice(0, 3).join('、') + (typingUsers.length > 3 ? ` 等${typingUsers.length}人` : '') + ' 正在输入…'
    : '';

  const onlinePresences = (presences || []).filter(p => p.userName !== userName);
  const moderatorSet = new Set(moderators || []);

  const handleToggleMod = (target: string, makeMod: boolean) => {
    eventBus.emit('REQUEST_SET_MODERATOR', { moderator: makeMod, target });
  };

  return (
    <header className="topbar">
      <div className="topbar__brand">
        <button className="topbar__back" onClick={onLeaveRoom} title="返回房间列表">
          ←
        </button>
        <div className="topbar__title-group">
          <h1 className="topbar__title">{roomName || '共读标注板'}</h1>
          <span className="topbar__subtitle">
            房主: {ownerName || 'system'} · {article?.title || '加载中'}
          </span>
        </div>
      </div>

      <div className="topbar__center">
        <div
          className={['status-chip', status.cls].join(' ')}
          title={onlineNames && onlineNames.length ? '在线：' + onlineNames.join('、') : undefined}
        >
          <span className="status-chip__dot" />
          <span>{status.label}</span>
          {onlineCount > 0 && (
            <span className="status-chip__count">👥 {onlineCount}</span>
          )}
          {currentModeratorName && (
            <span className="status-chip__mod" title={`当前主持人：${currentModeratorName}`}>
              🎙️ {currentModeratorName}
            </span>
          )}
        </div>

        {typingText && (
          <span className="typing-indicator">
            <span className="typing-indicator__dots">
              <span /><span /><span />
            </span>
            {typingText}
          </span>
        )}

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
        <div className="topbar__menu">
          <button
            className="btn btn--ghost topbar__menubtn"
            onClick={() => setShowMenu(v => !v)}
          >
            ⋯ 更多
          </button>
          {showMenu && (
            <div className="topbar__dropdown" onClick={() => setShowMenu(false)}>
              {isOwner && (
                <button className="topbar__menuitem" onClick={() => setShowModPanel(v => !v)}>
                  👑 管理主持人
                </button>
              )}
              <button className="topbar__menuitem" onClick={() => { eventBus.emit('REQUEST_EXPORT_MD'); }}>
                📄 导出 Markdown
              </button>
              <button className="topbar__menuitem" onClick={() => { eventBus.emit('REQUEST_EXPORT_JSON'); }}>
                💾 导出 JSON
              </button>
              <button className="topbar__menuitem" onClick={() => { onStartReplay && onStartReplay(); }}>
                🎬 回放模式
              </button>
              <button className="topbar__menuitem topbar__menuitem--danger" onClick={() => { onLeaveRoom && onLeaveRoom(); }}>
                🚪 离开房间
              </button>
            </div>
          )}
        </div>

        {showModPanel && isOwner && (
          <div className="mod-panel">
            <div className="mod-panel__header">
              <span>👑 管理主持人</span>
              <button className="mod-panel__close" onClick={() => setShowModPanel(false)}>✕</button>
            </div>
            <div className="mod-panel__body">
              {onlinePresences.length === 0 && (
                <div className="mod-panel__empty">暂无其他在线用户</div>
              )}
              {onlinePresences.map(p => (
                <div key={p.userName} className="mod-panel__row">
                  <span className="mod-panel__name">
                    {p.isOwner && '👑 '}{p.isModerator && '🎙️ '}{p.userName}
                  </span>
                  {!p.isOwner && (
                    <button
                      className={`btn btn--xs ${p.isModerator ? 'btn--warn' : 'btn--primary'}`}
                      onClick={() => handleToggleMod(p.userName, !p.isModerator)}
                    >
                      {p.isModerator ? '取消主持' : '设为主持人'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

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

        <div className="role-badge" title={
          isOwner ? '你是房主，拥有所有权限' :
          isModerator ? '你是主持人，可控制阅读进度' :
          '主持人权限由房主授予'
        }>
          {isOwner ? '👑 房主' : isModerator ? '🎙️ 主持人' : '👤 读者'}
        </div>

        <button className="icon-btn notes-toggle" onClick={onOpenNotesMobile} aria-label="打开批注">
          💬
        </button>
      </div>
    </header>
  );
};

export default ModeratorBar;
