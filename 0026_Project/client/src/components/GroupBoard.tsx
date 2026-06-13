import React, { useState, useEffect, useRef, useMemo } from 'react';
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
  const [movedSet, setMovedSet] = useState<Set<string>>(new Set());
  const prevGroupsRef = useRef<Group[]>(groups);
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    const prevGroups = prevGroupsRef.current;
    const movedIds: string[] = [];

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
        movedIds.push(p.id);
      }
    });

    if (movedIds.length > 0) {
      setMovedSet((prev) => {
        const next = new Set(prev);
        movedIds.forEach((id) => {
          next.add(id);
          const existingTimer = timersRef.current.get(id);
          if (existingTimer) clearTimeout(existingTimer);
          const timer = setTimeout(() => {
            setMovedSet((s) => {
              const n = new Set(s);
              n.delete(id);
              return n;
            });
            timersRef.current.delete(id);
          }, 2000);
          timersRef.current.set(id, timer);
        });
        return next;
      });
    }

    prevGroupsRef.current = groups;
    return () => {};
  }, [groups, participants]);

  useEffect(() => {
    return () => {
      timersRef.current.forEach((t) => clearTimeout(t));
      timersRef.current.clear();
    };
  }, []);

  const participantMap = useMemo(
    () => new Map(participants.map((p) => [p.id, p])),
    [participants]
  );

  const myGroupId = useMemo(() => {
    if (!myParticipantId) return null;
    for (const g of groups) {
      if (g.participantIds.includes(myParticipantId)) return g.id;
    }
    return null;
  }, [groups, myParticipantId]);

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

  const isTeammate = (pid: string, groupId: string) => {
    if (!myParticipantId || !myGroupId || groupId !== myGroupId) return false;
    if (pid === myParticipantId) return false;
    return true;
  };

  return (
    <div className="group-board-wrapper">
      {selectedPid && selectedParticipant && (
        <div className="move-hint">
          已选择 <strong>{selectedParticipant.name}</strong>，点击目标组移动
          <button className="cancel-move" onClick={() => setSelectedPid(null)}>取消</button>
        </div>
      )}
      <div className="group-board">
        {groups.map((group, index) => {
          const isMyGroup = myGroupId === group.id;
          const dimOtherGroup = myParticipantId && !isMyGroup;
          return (
            <div
              key={group.id}
              className={`group-card ${group.locked ? 'locked' : ''} ${
                selectedPid && !group.locked && isHost ? 'selectable' : ''
              } ${isMyGroup ? 'my-group' : ''} ${dimOtherGroup ? 'dim-other-group' : ''}`}
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
                      const isMoved = movedSet.has(pid);
                      const isSelected = selectedPid === pid;
                      const isMe = myParticipantId === pid;
                      const isTeammateFlag = isTeammate(pid, group.id);
                      return (
                        <li
                          key={pid}
                          className={`member-item ${isMoved ? 'moved-member' : ''} ${
                            isSelected ? 'selected' : ''
                          } ${isMe ? 'is-me' : ''} ${isTeammateFlag ? 'teammate' : ''}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleMemberClick(pid, group.locked);
                          }}
                        >
                          <span className="member-idx">{idx + 1}</span>
                          <span className="member-name">
                            {isMoved && <span className="moved-sparkle">✨</span>}
                            {p.name}
                            {isMe && <span className="me-badge">我</span>}
                          </span>
                          {isTeammateFlag && <span className="teammate-icon">👥</span>}
                          {p.gender && <span className="member-tag">{p.gender}</span>}
                          {p.department && <span className="member-tag dept">{p.department}</span>}
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default GroupBoard;
