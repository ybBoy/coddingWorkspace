import type { VoteOptionData } from './App';

/**
 * VotePanel 职责：
 * - 根据当前投票数据渲染柱状图
 * - 自动计算百分比，柱子宽度随票数实时变化
 * - 高亮显示当前用户已投票的选项
 */
interface Props {
  options: VoteOptionData[];
  totalVotes: number;
  myVoteId: string | null;
}

function VotePanel({ options, totalVotes, myVoteId }: Props) {
  if (options.length === 0) {
    return <div className="chart-empty">暂无数据</div>;
  }

  const maxVotes = Math.max(...options.map((o) => o.votes), 1);

  return (
    <div className="chart-container">
      {options.map((opt) => {
        const percent = totalVotes > 0 ? (opt.votes / totalVotes) * 100 : 0;
        const barWidth = (opt.votes / maxVotes) * 100;
        const isMyVote = opt.id === myVoteId;
        return (
          <div className="chart-item" key={opt.id}>
            <div className="chart-label">
              <span className={`name ${isMyVote ? 'name-selected' : ''}`}>
                {isMyVote && <span className="vote-indicator">✓ </span>}
                {opt.name}
              </span>
              <span className="count">
                {opt.votes} 票（{percent.toFixed(1)}%）
              </span>
            </div>
            <div className="chart-bar">
              <div
                className={`chart-bar-fill ${isMyVote ? 'chart-bar-fill-selected' : ''}`}
                style={{ width: `${barWidth}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default VotePanel;
