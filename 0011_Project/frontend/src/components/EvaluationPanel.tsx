import React, { useState } from 'react';
import { useEvaluationSocket } from '../hooks/useEvaluationSocket';
import { ScoreRow } from './ScoreRow';
import { VersionHistoryModal } from './VersionHistoryModal';
import type { Role } from '../types';

interface EvaluationPanelProps {
  formId: string;
  userName: string;
  role: Role;
}

export const EvaluationPanel: React.FC<EvaluationPanelProps> = ({ formId, userName, role }) => {
  const isInterviewer = role === 'INTERVIEWER';
  const [showVersions, setShowVersions] = useState(false);

  const {
    scores,
    versions,
    users,
    connected,
    updateScore,
    commitVersion,
    rollbackToVersion
  } = useEvaluationSocket({ formId, userName, role });

  const handleRollback = (versionId: number) => {
    if (window.confirm('确定要回滚到此版本吗？当前未保存的修改将丢失。')) {
      rollbackToVersion(versionId);
      setShowVersions(false);
    }
  };

  return (
    <div className="evaluation-page">
      <header className="page-header">
        <div className="header-left">
          <h1 className="page-title">面试评价表</h1>
          <span className="form-id">表单ID: {formId}</span>
        </div>
        <div className="header-right">
          <div className="connection-status">
            <span className={`status-dot ${connected ? 'online' : 'offline'}`} />
            <span className="status-text">{connected ? '已连接' : '连接中断'}</span>
          </div>
          <span className="user-name">
            {userName} <span className="role-badge">({isInterviewer ? '面试官' : '候选人'})</span>
          </span>
          {isInterviewer && (
            <>
              <button
                className="btn btn-secondary"
                onClick={() => setShowVersions(true)}
              >
                历史版本
              </button>
              <button className="btn btn-primary" onClick={commitVersion}>
                提交版本
              </button>
            </>
          )}
        </div>
      </header>

      <div className="users-bar">
        <span className="users-label">在线用户：</span>
        {Object.values(users).map((u, i) => (
          <span key={i} className="user-chip">{u}</span>
        ))}
      </div>

      <main className="evaluation-form">
        <div className="form-card">
          {scores.map((s) => (
            <ScoreRow
              key={s.dimension}
              score={s}
              role={role}
              onScoreChange={updateScore}
            />
          ))}
        </div>
      </main>

      <VersionHistoryModal
        visible={showVersions}
        versions={versions}
        isInterviewer={isInterviewer}
        onClose={() => setShowVersions(false)}
        onRollback={handleRollback}
      />
    </div>
  );
};
