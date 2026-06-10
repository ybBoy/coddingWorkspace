import React from 'react';

interface StarRatingProps {
  value: number;
  readOnly?: boolean;
  onChange?: (value: number) => void;
}

export const StarRating: React.FC<StarRatingProps> = ({ value, readOnly = false, onChange }) => {
  const handleClick = (index: number) => {
    if (!readOnly && onChange) {
      onChange(index + 1);
    }
  };

  return (
    <div className="star-rating">
      {[0, 1, 2, 3, 4].map((i) => (
        <span
          key={i}
          className={`star ${i < value ? 'filled' : ''} ${readOnly ? 'readonly' : ''}`}
          onClick={() => handleClick(i)}
        >
          ★
        </span>
      ))}
    </div>
  );
};
