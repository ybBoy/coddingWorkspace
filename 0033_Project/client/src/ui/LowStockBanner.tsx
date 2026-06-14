import React from 'react';
import { WarningSummary } from '../types';

interface LowStockBannerProps {
  summary: WarningSummary | null;
}

const LowStockBanner: React.FC<LowStockBannerProps> = ({ summary }) => {
  if (!summary) return null;
  const { approachingCount, lowStockCount, emptyCount } = summary;
  const total = approachingCount + lowStockCount + emptyCount;
  if (total === 0) return null;

  return (
    <div className="low-stock-banner">
      <span className="banner-icon">⚠️</span>
      <div className="banner-content">
        <span className="banner-text">
          共 <strong>{total}</strong> 款咖啡豆需要关注：
        </span>
        {emptyCount > 0 && (
          <span className="warning-tag tag-empty">
            已耗尽 {emptyCount}
          </span>
        )}
        {lowStockCount > 0 && (
          <span className="warning-tag tag-low">
            已不足 {lowStockCount}
          </span>
        )}
        {approachingCount > 0 && (
          <span className="warning-tag tag-approaching">
            即将不足 {approachingCount}
          </span>
        )}
      </div>
    </div>
  );
};

export default LowStockBanner;
