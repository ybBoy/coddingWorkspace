import React from 'react';
import type { EvaluationVersion } from '../types';
import { StarRating } from './StarRating';

interface VersionHistoryModalProps {
  visible: boolean;
  versions: EvaluationVersion[];
  isInterviewer: boolean;
  onClose: () => void;
  onRollback: (versionId: number) => void;
}

const formatDate = (ts: number) => {
  const d = new Date(ts);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

export const VersionHistoryModal: React.FC<VersionHistoryModalProps> = ({
  visible,
  versions,
  isInterviewer,
  onClose,
  onRollback
}) => {
  if (!visible) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>历史版本</h3>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="modal-body">
          {versions.length === 0 ? (
            <div className="empty-tip">暂无历史版本</div>
          ) : (
            <div className="versions-list">
              {versions.map((v, index) => (
                <div key={v.versionId} className="version-card">
                  <div className="version-header">
                    <div className="version-title">
                      <span className="version-tag">V{versions.length - index}</span>
                      <span className="version-meta">
                        {formatDate(v.createdAt)} · {v.createdBy}
                      </span>
                    </div>
                    {isInterviewer && (
                      <button
                        className="rollback-btn"
                        onClick={() => onRollback(v.versionId)}
                      >
                        回滚至此版本
                      </button>
                    )}
                  </div>
                  <div className="version-scores">
                    {v.scores.map((s) => (
                      <div key={s.dimension} className="version-score-item">
                        <span className="version-dim-name">{s.dimension}</span>
                        <StarRating value={s.score} readOnly />
                        <span className="version-dim-comment">
                          {s.comment || '—'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
