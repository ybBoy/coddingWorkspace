import { useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import type { VoteOptionData } from './App';

/**
 * OptionList 职责：
 * - 渲染投票选项列表，高亮当前用户已投票的选项
 * - 处理投票按钮点击，通过 EventBus 发出 VOTE 事件
 * - 提供新增选项输入框（锁定/断线时禁用）
 * - 管理员可删除/重命名选项
 * - 锁定/断线状态下禁用投票和新增按钮
 */
interface Props {
  options: VoteOptionData[];
  myVoteId: string | null;
  isAdmin: boolean;
  isLocked: boolean;
  connected: boolean;
}

function OptionList({ options, myVoteId, isAdmin, isLocked, connected }: Props) {
  const [newOptionName, setNewOptionName] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState('');

  const disabled = !connected || isLocked;

  const handleVote = (id: string) => {
    if (disabled) return;
    eventBus.emit(EVENTS.VOTE, id);
  };

  const handleAddOption = () => {
    const name = newOptionName.trim();
    if (!name || disabled) return;
    eventBus.emit(EVENTS.ADD_OPTION, name);
    setNewOptionName('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleAddOption();
  };

  const handleClearAll = () => {
    if (!isAdmin || !connected) return;
    eventBus.emit(EVENTS.CLEAR_ALL);
  };

  const handleDelete = (id: string) => {
    if (!isAdmin || !connected) return;
    eventBus.emit(EVENTS.DELETE_OPTION, id);
  };

  const startRename = (opt: VoteOptionData) => {
    if (!isAdmin || !connected) return;
    setEditingId(opt.id);
    setEditingName(opt.name);
  };

  const cancelRename = () => {
    setEditingId(null);
    setEditingName('');
  };

  const saveRename = (id: string) => {
    const name = editingName.trim();
    if (!name || !isAdmin || !connected) {
      cancelRename();
      return;
    }
    eventBus.emit(EVENTS.RENAME_OPTION, { id, name });
    cancelRename();
  };

  const handleRenameKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, id: string) => {
    if (e.key === 'Enter') saveRename(id);
    else if (e.key === 'Escape') cancelRename();
  };

  return (
    <div>
      <div className="option-list">
        {options.length === 0 && (
          <div className="chart-empty">暂无选项，下方新增一个吧</div>
        )}
        {options.map((opt) => {
          const isMyVote = opt.id === myVoteId;
          const isEditing = opt.id === editingId;
          return (
            <div
              className={`option-item ${isMyVote ? 'option-item-selected' : ''}`}
              key={opt.id}
            >
              {isEditing ? (
                <div className="option-edit">
                  <input
                    type="text"
                    value={editingName}
                    onChange={(e) => setEditingName(e.target.value)}
                    onKeyDown={(e) => handleRenameKeyDown(e, opt.id)}
                    autoFocus
                  />
                  <div className="option-edit-actions">
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={() => saveRename(opt.id)}
                    >
                      保存
                    </button>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={cancelRename}
                    >
                      取消
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <span className="option-name">
                    {isMyVote && <span className="vote-indicator">✓ </span>}
                    {opt.name}
                  </span>
                  <div className="option-actions">
                    <span className="option-count">{opt.votes} 票</span>
                    {isAdmin && (
                      <>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => startRename(opt)}
                          disabled={disabled}
                        >
                          重命名
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => handleDelete(opt.id)}
                          disabled={disabled}
                        >
                          删除
                        </button>
                      </>
                    )}
                    <button
                      className={`btn ${
                        isMyVote ? 'btn-secondary' : 'btn-primary'
                      }`}
                      onClick={() => handleVote(opt.id)}
                      disabled={disabled}
                    >
                      {isMyVote ? '已投' : '投一票'}
                    </button>
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>

      <div className="add-option">
        <input
          type="text"
          placeholder="输入新选项名称…"
          value={newOptionName}
          onChange={(e) => setNewOptionName(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />
        <button
          className="btn btn-secondary"
          onClick={handleAddOption}
          disabled={disabled}
        >
          新增
        </button>
      </div>

      {isAdmin && (
        <div className="admin-actions">
          <button
            className="btn btn-danger"
            onClick={handleClearAll}
            disabled={!connected}
          >
            清空投票
          </button>
        </div>
      )}
    </div>
  );
}

export default OptionList;
