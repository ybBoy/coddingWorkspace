import type { VoteOptionData } from './App';

/**
 * VotePanel 职责：
 * - 根据当前投票数据渲染柱状图
 * - 自动计算百分比，柱子宽度随票数实时变化
 */
interface Props {
  options: VoteOptionData[];
  totalVotes: number;
}

function VotePanel({ options, totalVotes }: Props) {
  if (options.length === 0) {
    return <div className="chart-empty">暂无数据</div>;
  }

  // 找出最大票数，用于计算柱状图相对宽度
  const maxVotes = Math.max(...options.map((o) => o.votes), 1);

  return (
    <div className="chart-container">
      {options.map((opt) => {
        const percent = totalVotes > 0 ? (opt.votes / totalVotes) * 100 : 0;
        const barWidth = (opt.votes / maxVotes) * 100;
        return (
          <div className="chart-item" key={opt.id}>
            <div className="chart-label">
              <span className="name">{opt.name}</span>
              <span className="count">
                {opt.votes} 票（{percent.toFixed(1)}%）
              </span>
            </div>
            <div className="chart-bar">
              <div
                className="chart-bar-fill"
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
