import React, { useState, useEffect, useRef } from 'react';
import socket from '../shared/socket';
import { Group, Participant } from '../shared/types';

interface GroupBoardProps {
  groups: Group[];
  participants: Participant[];
  isHost: boolean;
}

const GroupBoard: React.FC<GroupBoardProps> = ({ groups, participants, isHost }) => {
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [dragOverGroup, setDragOverGroup] = useState<string | null>(null);
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

  const handleDragStart = (e: React.DragEvent, participantId: string) => {
    if (!isHost) {
      e.preventDefault();
      return;
    }
    setDraggedId(participantId);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', participantId);
  };

  const handleDragEnd = () => {
    setDraggedId(null);
    setDragOverGroup(null);
  };

  const handleDragOver = (e: React.DragEvent, groupId: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (dragOverGroup !== groupId) {
      setDragOverGroup(groupId);
    }
  };

  const handleDragLeave = () => {
    setDragOverGroup(null);
  };

  const handleDrop = (e: React.DragEvent, groupId: string) => {
    e.preventDefault();
    const pid = e.dataTransfer.getData('text/plain');
    if (pid && isHost) {
      socket.send({ type: 'move-participant', participantId: pid, targetGroupId: groupId });
    }
    setDraggedId(null);
    setDragOverGroup(null);
  };

  const handleToggleLock = (groupId: string) => {
    if (!isHost) return;
    socket.send({ type: 'toggle-lock', groupId });
  };

  const getGroupColor = (index: number) => {
    const colors = [
      '#6366f1',
      '#8b5cf6',
      '#ec4899',
      '#f43f5e',
      '#f97316',
      '#eab308',
      '#22c55e',
      '#14b8a6',
      '#06b6d4',
      '#3b82f6',
    ];
    return colors[index % colors.length];
  };

  return (
    <div className="group-board">
      {groups.map((group, index) => (
        <div
          key={group.id}
          className={`group-card ${group.locked ? 'locked' : ''} ${
            dragOverGroup === group.id ? 'drag-over' : ''
          }`}
          style={{ borderColor: group.locked ? '#9ca3af' : getGroupColor(index) }}
          onDragOver={(e) => handleDragOver(e, group.id)}
          onDragLeave={handleDragLeave}
          onDrop={(e) => handleDrop(e, group.id)}
        >
          <div
            className="group-header"
            style={{ backgroundColor: group.locked ? '#9ca3af' : getGroupColor(index) }}
          >
            <span className="group-name">{group.name}</span>
            <span className="group-count">
              {group.participantIds.length} 人
            </span>
            {isHost && (
              <button
                className={`lock-btn ${group.locked ? 'locked' : ''}`}
                onClick={() => handleToggleLock(group.id)}
                title={group.locked ? '解锁' : '锁定'}
              >
                {group.locked ? '🔒' : '🔓'}
              </button>
            )}
          </div>
          <div className="group-members">
            {group.participantIds.length === 0 ? (
              <div className="empty-member">
                {group.locked ? '锁定中' : '拖入成员'}
              </div>
            ) : (
              <ul>
                {group.participantIds.map((pid, idx) => {
                  const p = participantMap.get(pid);
                  if (!p) return null;
                  const isMoved = recentlyMoved.has(pid);
                  return (
                    <li
                      key={pid}
                      className={`member-item ${isMoved ? 'highlight' : ''} ${
                        draggedId === pid ? 'dragging' : ''
                      }`}
                      draggable={isHost && !group.locked}
                      onDragStart={(e) => handleDragStart(e, pid)}
                      onDragEnd={handleDragEnd}
                    >
                      <span className="member-idx">{idx + 1}</span>
                      <span className="member-name">{p.name}</span>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default GroupBoard;
