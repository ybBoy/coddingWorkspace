import { useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import type { VoteOptionData } from './App';

/**
 * OptionList 职责：
 * - 渲染投票选项列表
 * - 处理投票按钮点击，通过 EventBus 发出 VOTE 事件
 * - 提供新增选项输入框，通过 EventBus 发出 ADD_OPTION 事件
 * - 提供管理员清空投票按钮
 */
interface Props {
  options: VoteOptionData[];
}

function OptionList({ options }: Props) {
  const [newOptionName, setNewOptionName] = useState('');

  const handleVote = (id: string) => {
    eventBus.emit(EVENTS.VOTE, id);
  };

  const handleAddOption = () => {
    const name = newOptionName.trim();
    if (!name) return;
    eventBus.emit(EVENTS.ADD_OPTION, name);
    setNewOptionName('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleAddOption();
  };

  const handleClearAll = () => {
    if (window.confirm('确定要清空所有投票吗？此操作不可撤销。')) {
      eventBus.emit(EVENTS.CLEAR_ALL);
    }
  };

  return (
    <div>
      <div className="option-list">
        {options.length === 0 && (
          <div className="chart-empty">暂无选项，下方新增一个吧</div>
        )}
        {options.map((opt) => (
          <div className="option-item" key={opt.id}>
            <span className="option-name">{opt.name}</span>
            <div className="option-actions">
              <span className="option-count">{opt.votes} 票</span>
              <button
                className="btn btn-primary"
                onClick={() => handleVote(opt.id)}
              >
                投一票
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="add-option">
        <input
          type="text"
          placeholder="输入新选项名称…"
          value={newOptionName}
          onChange={(e) => setNewOptionName(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <button className="btn btn-secondary" onClick={handleAddOption}>
          新增
        </button>
      </div>

      <div className="admin-actions">
        <button className="btn btn-danger" onClick={handleClearAll}>
          清空投票
        </button>
      </div>
    </div>
  );
}

export default OptionList;
