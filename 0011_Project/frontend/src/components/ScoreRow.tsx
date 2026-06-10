import React, { useState, useCallback } from 'react';
import { StarRating } from './StarRating';
import type { DimensionScore, Role } from '../types';

interface ScoreRowProps {
  score: DimensionScore;
  role: Role;
  onScoreChange: (score: DimensionScore) => void;
}

export const ScoreRow: React.FC<ScoreRowProps> = ({ score, role, onScoreChange }) => {
  const isInterviewer = role === 'INTERVIEWER';
  const [localComment, setLocalComment] = useState(score.comment);

  const handleSliderChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newScore = parseInt(e.target.value, 10);
      onScoreChange({ ...score, score: newScore });
    },
    [score, onScoreChange]
  );

  const handleStarChange = useCallback(
    (newValue: number) => {
      onScoreChange({ ...score, score: newValue });
    },
    [score, onScoreChange]
  );

  const handleCommentBlur = useCallback(
    (e: React.FocusEvent<HTMLTextAreaElement>) => {
      const newComment = e.target.value;
      if (newComment !== score.comment) {
        onScoreChange({ ...score, comment: newComment });
      }
    },
    [score, onScoreChange]
  );

  return (
    <div className="score-row">
      <div className="dimension-info">
        <span className="dimension-name">{score.dimension}</span>
        <StarRating value={score.score} readOnly={!isInterviewer} onChange={handleStarChange} />
      </div>
      <div className="dimension-score">
        {isInterviewer ? (
        <>
          <input
            type="range"
            min="1"
            max="5"
            value={score.score}
            onChange={handleSliderChange}
            className="score-slider"
          />
          <span className="score-value">{score.score}</span>
        </>
      ) : (
        <span className="score-value readonly-score">{score.score} 分</span>
      )}
      </div>
      <div className="dimension-comment">
        {isInterviewer ? (
          <textarea
            value={localComment}
            onChange={(e) => setLocalComment(e.target.value)}
            onBlur={handleCommentBlur}
            placeholder="请输入评语..."
            className="comment-textarea"
            rows={3}
          />
        ) : (
          <div className="comment-readonly">
          {score.comment || <span className="comment-placeholder">暂无评语</span>}
          </div>
        )}
      </div>
    </div>
  );
};
