import React, { useState, useEffect, useRef } from 'react';
import socket from '../shared/socket';
import { Group, Participant } from '../shared/types';

interface GroupBoardProps {
  groups: Group[];
  participants: Participant[];
  isHost: boolean;
  myParticipantId?: string | null;
}

const GroupBoard: React.FC<GroupBoardProps> = ({ groups, participants, isHost, myParticipantId }) => {
  const [selectedPid, setSelectedPid] = useState<string | null>(null);
  const [recentlyMoved, setRecentlyMoved] = useState<Set<string>>(new Set());
  const prevGroupsRef = useRef<Group[]>(groups);

  useEffect(() => {
    const prevGroups = prevGroupsRef.current;
    const movedIds = new Set<string>();

    const prevMap = new Map<string, string>();
    prevGroups.forEach((g) => {
      g.participantIds.forEach((pid) => prevMap.set(pid, g.id));
    });

    const currMap = new Map<string, string>();
    groups.forEach((g) => {
      g.participantIds.forEach((pid) => currMap.set(pid, g.id));
    });

    participants.forEach((p) => {
      const prevGroup = prevMap.get(p.id);
      const currGroup = currMap.get(p.id);
      if (prevGroup !== currGroup && currGroup) {
        movedIds.add(p.id);
      }
    });

    if (movedIds.size > 0) {
      setRecentlyMoved(movedIds);
      const timer = setTimeout(() => {
        setRecentlyMoved(new Set());
      }, 2000);
      return () => clearTimeout(timer);
    }

    prevGroupsRef.current = groups;
  }, [groups, participants]);

  const participantMap = new Map(participants.map((p) => [p.id, p]));

  const handleMemberClick = (pid: string, groupLocked: boolean) => {
    if (!isHost || groupLocked) return;
    if (selectedPid === pid) {
      setSelectedPid(null);
    } else {
      setSelectedPid(pid);
    }
  };

  const handleGroupClick = (groupId: string) => {
    if (!isHost || !selectedPid) return;
    const targetGroup = groups.find((g) => g.id === groupId);
    if (targetGroup && !targetGroup.locked) {
      socket.send({ type: 'move-participant', participantId: selectedPid, targetGroupId: groupId });
      setSelectedPid(null);
    }
  };

  const handleToggleLock = (groupId: string) => {
    if (!isHost) return;
    socket.send({ type: 'toggle-lock', groupId });
  };

  const getGroupColor = (index: number) => {
    const colors = [
      '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316',
      '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6',
    ];
    return colors[index % colors.length];
  };

  const selectedParticipant = selectedPid ? participantMap.get(selectedPid) : null;

  return (
    <div className="group-board-wrapper">
      {selectedPid && selectedParticipant && (
        <div className="move-hint">
          已选择 <strong>{selectedParticipant.name}</strong>，点击目标组移动
          <button className="cancel-move" onClick={() => setSelectedPid(null)}>取消</button>
        </div>
      )}
      <div className="group-board">
        {groups.map((group, index) => (
          <div
            key={group.id}
            className={`group-card ${group.locked ? 'locked' : ''} ${
              selectedPid && !group.locked ? 'selectable' : ''
            }`}
            style={{ borderColor: group.locked ? '#9ca3af' : getGroupColor(index) }}
            onClick={() => handleGroupClick(group.id)}
          >
            <div
              className="group-header"
              style={{ backgroundColor: group.locked ? '#9ca3af' : getGroupColor(index) }}
            >
              <span className="group-name">{group.name}</span>
              <span className="group-count">{group.participantIds.length} 人</span>
              {isHost && (
                <button
                  className={`lock-btn ${group.locked ? 'locked' : ''}`}
                  onClick={(e) => { e.stopPropagation(); handleToggleLock(group.id); }}
                  title={group.locked ? '解锁' : '锁定'}
                >
                  {group.locked ? '🔒' : '🔓'}
                </button>
              )}
            </div>
            <div className="group-members">
              {group.participantIds.length === 0 ? (
                <div className="empty-member">
                  {group.locked ? '锁定中' : '点击加入'}
                </div>
              ) : (
                <ul>
                  {group.participantIds.map((pid, idx) => {
                    const p = participantMap.get(pid);
                    if (!p) return null;
                    const isMoved = recentlyMoved.has(pid);
                    const isSelected = selectedPid === pid;
                    const isMe = myParticipantId === pid;
                    return (
                      <li
                        key={pid}
                        className={`member-item ${isMoved ? 'highlight' : ''} ${
                          isSelected ? 'selected' : ''
                        } ${isMe ? 'is-me' : ''}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMemberClick(pid, group.locked);
                        }}
                      >
                        <span className="member-idx">{idx + 1}</span>
                        <span className="member-name">
                          {p.name}
                          {isMe && <span className="me-badge">我</span>}
                        </span>
                        {p.gender && <span className="member-tag">{p.gender}</span>}
                        {p.department && <span className="member-tag dept">{p.department}</span>}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default GroupBoard;
