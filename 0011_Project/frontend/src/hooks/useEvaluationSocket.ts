import { useEffect, useRef, useState, useCallback } from 'react';
import type { DimensionScore, EvaluationVersion, WsMessage, Role } from '../types';

interface UseEvaluationSocketProps {
  formId: string;
  userName: string;
  role: Role;
}

export function useEvaluationSocket({ formId, userName, role }: UseEvaluationSocketProps) {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const [scores, setScores] = useState<DimensionScore[]>([]);
  const [versions, setVersions] = useState<EvaluationVersion[]>([]);
  const [users, setUsers] = useState<Record<string, string>>({});
  const [connected, setConnected] = useState(false);

  const connect = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws/evaluation?formId=${encodeURIComponent(formId)}&userName=${encodeURIComponent(userName)}&role=${role}`;

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        handleMessage(msg);
      } catch (e) {
        console.error('Failed to parse WebSocket message', e);
      }
    };

    ws.onerror = () => {
      setConnected(false);
    };

    ws.onclose = () => {
      setConnected(false);
      reconnectTimerRef.current = window.setTimeout(() => {
        connect();
      }, 2000);
    };
  }, [formId, userName, role]);

  const handleMessage = useCallback((msg: WsMessage) => {
    switch (msg.type) {
      case 'INIT':
        if (msg.scores) {
          setScores(msg.scores);
        }
        sendMessage({ type: 'GET_VERSIONS', formId });
        break;
      case 'SCORE_UPDATE':
        if (msg.score) {
          setScores((prev) =>
            prev.map((s) =>
              s.dimension === msg.score!.dimension ? { ...msg.score! } : s
            )
          );
        }
        break;
      case 'ROLLBACK':
        if (msg.scores) {
          setScores(msg.scores.map((s) => ({ ...s })));
        }
        break;
      case 'VERSIONS_LIST':
        if (msg.versions) {
          setVersions(msg.versions);
        }
        break;
      case 'VERSION_SAVED':
        sendMessage({ type: 'GET_VERSIONS', formId });
        break;
      case 'USERS_UPDATE':
        if (msg.users) {
          setUsers(msg.users);
        }
        break;
      default:
        break;
    }
  }, [formId]);

  const sendMessage = useCallback((msg: Partial<WsMessage>) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  const updateScore = useCallback((score: DimensionScore) => {
    setScores((prev) =>
      prev.map((s) => (s.dimension === score.dimension ? { ...score } : s))
    );
    sendMessage({
      type: 'SCORE_UPDATE',
      formId,
      userName,
      role,
      score,
      timestamp: Date.now()
    });
  }, [sendMessage, formId, userName, role]);

  const commitVersion = useCallback(() => {
    sendMessage({
      type: 'COMMIT_VERSION',
      formId,
      userName,
      role,
      timestamp: Date.now()
    });
  }, [sendMessage, formId, userName, role]);

  const rollbackToVersion = useCallback((versionId: number) => {
    sendMessage({
      type: 'ROLLBACK',
      formId,
      userName,
      role,
      versionId,
      timestamp: Date.now()
    });
  }, [sendMessage, formId, userName, role]);

  const requestVersions = useCallback(() => {
    sendMessage({ type: 'GET_VERSIONS', formId });
  }, [sendMessage, formId]);

  useEffect(() => {
    connect();
    return () => {
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [connect]);

  return {
    scores,
    versions,
    users,
    connected,
    updateScore,
    commitVersion,
    rollbackToVersion,
    requestVersions
  };
}
