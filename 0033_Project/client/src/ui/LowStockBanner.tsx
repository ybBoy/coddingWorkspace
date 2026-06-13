import React from 'react';

interface LowStockBannerProps {
  lowStockCount: number;
}

const LowStockBanner: React.FC<LowStockBannerProps> = ({ lowStockCount }) => {
  if (lowStockCount === 0) return null;

  return (
    <div className="low-stock-banner">
      <span className="banner-icon">⚠️</span>
      <span className="banner-text">
        当前有 <strong>{lowStockCount}</strong> 款咖啡豆库存不足，请及时补货！
      </span>
    </div>
  );
};

export default LowStockBanner;
