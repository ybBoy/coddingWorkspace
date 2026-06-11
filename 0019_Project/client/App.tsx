import { useEffect, useState, useRef, useCallback } from 'react';
import { eventBus, EVENTS } from './EventBus';
import OptionList from './OptionList';
import VotePanel from './VotePanel';
import AdminPanel from './AdminPanel';

/**
 * 投票选项的数据结构
 */
export interface VoteOptionData {
  id: string;
  name: string;
  votes: number;
}

/**
 * 后端完整状态数据结构
 */
export interface VoteStateData {
  options: VoteOptionData[];
  locked: boolean;
  remainingSeconds: number;
}

/**
 * WebSocket 消息结构
 */
interface WsMessage {
  type:
    | 'INIT'
    | 'UPDATE'
    | 'VOTE'
    | 'ADD'
    | 'CLEAR'
    | 'ADMIN_LOGIN_OK'
    | 'ADMIN_LOGIN_FAIL'
    | 'DELETE'
    | 'RENAME'
    | 'LOCK'
    | 'SET_TIMER';
  data?: any;
}

/**
 * 生成或获取 clientId，保存在 localStorage
 */
function getOrCreateClientId(): string {
  let id = localStorage.getItem('vote_client_id');
  if (!id) {
    id =
      'c_' +
      Date.now().toString(36) +
      Math.random().toString(36).substring(2, 8);
    localStorage.setItem('vote_client_id', id);
  }
  return id;
}

/**
 * App 主组件职责：
 * - 建立和维护 WebSocket 连接（带自动重连）
 * - 生成并管理 clientId（localStorage 持久化）
 * - 持有全局状态：选项、锁定状态、倒计时、管理员状态、用户已投票的选项
 * - 监听 EventBus 的用户操作事件，转发给后端（带上 clientId / adminToken）
 * - 接收后端广播，更新状态并通知组件重渲染
 * - 断线期间禁用操作按钮，重连后自动同步数据
 */
function App() {
  const [options, setOptions] = useState<VoteOptionData[]>([]);
  const [isLocked, setIsLocked] = useState(false);
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const [connected, setConnected] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [adminToken, setAdminToken] = useState<string | null>(null);
  const [myVoteId, setMyVoteId] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const clientIdRef = useRef<string>(getOrCreateClientId());
  const reconnectAttemptsRef = useRef(0);
  const reconnectTimerRef = useRef<number | null>(null);
  const manualCloseRef = useRef(false);

  // 发送消息到后端
  const sendWs = useCallback(
    (msg: Record<string, any>) => {
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        const fullMsg = {
          ...msg,
          clientId: clientIdRef.current,
          adminToken: adminToken || undefined,
        };
        wsRef.current.send(JSON.stringify(fullMsg));
      }
    },
    [adminToken]
  );

  // 连接 WebSocket
  const connectWs = useCallback(() => {
    if (manualCloseRef.current) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
      setReconnecting(false);
      reconnectAttemptsRef.current = 0;
      eventBus.emit(EVENTS.WS_CONNECTED);
      console.log('[WS] 已连接');
    };

    ws.onclose = () => {
      setConnected(false);
      setMyVoteId(null);
      eventBus.emit(EVENTS.WS_DISCONNECTED);
      console.log('[WS] 连接断开');

      if (!manualCloseRef.current) {
        // 自动重连，指数退避
        const delay = Math.min(
          1000 * Math.pow(2, reconnectAttemptsRef.current),
          10000
        );
        reconnectAttemptsRef.current++;
        setReconnecting(true);
        eventBus.emit(EVENTS.WS_RECONNECTING, {
          attempt: reconnectAttemptsRef.current,
          delay,
        });
        console.log(
          `[WS] ${delay / 1000} 秒后第 ${reconnectAttemptsRef.current} 次重连…`
        );
        reconnectTimerRef.current = window.setTimeout(() => {
          connectWs();
        }, delay);
      }
    };

    ws.onerror = () => {
      eventBus.emit(EVENTS.WS_ERROR);
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);

        if (msg.type === 'INIT' || msg.type === 'UPDATE') {
          const state: VoteStateData = msg.data;
          setOptions(state.options || []);
          setIsLocked(state.locked || false);
          setRemainingSeconds(state.remainingSeconds || 0);
          eventBus.emit(EVENTS.DATA_UPDATED, state);
        } else if (msg.type === 'ADMIN_LOGIN_OK') {
          const token = msg.data as string;
          setAdminToken(token);
          setIsAdmin(true);
          localStorage.setItem('vote_admin_token', token);
          eventBus.emit(EVENTS.ADMIN_LOGIN_OK);
        } else if (msg.type === 'ADMIN_LOGIN_FAIL') {
          setAdminToken(null);
          setIsAdmin(false);
          localStorage.removeItem('vote_admin_token');
          eventBus.emit(EVENTS.ADMIN_LOGIN_FAIL);
        }
      } catch (e) {
        console.error('解析 WebSocket 消息失败', e);
      }
    };
  }, []);

  // 初始化 WebSocket 连接
  useEffect(() => {
    // 恢复管理员登录状态
    const savedToken = localStorage.getItem('vote_admin_token');
    if (savedToken) {
      setAdminToken(savedToken);
      setIsAdmin(true);
    }

    connectWs();

    return () => {
      manualCloseRef.current = true;
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [connectWs]);

  // 监听 EventBus：用户投票
  useEffect(() => {
    const handleVote = (optionId: string) => {
      if (!connected || isLocked) return;
      sendWs({ type: 'VOTE', data: optionId });
      setMyVoteId(optionId);
    };
    eventBus.on(EVENTS.VOTE, handleVote);
    return () => eventBus.off(EVENTS.VOTE, handleVote);
  }, [sendWs, connected, isLocked]);

  // 监听 EventBus：新增选项
  useEffect(() => {
    const handleAdd = (name: string) => {
      if (!connected || isLocked) return;
      sendWs({ type: 'ADD', data: name });
    };
    eventBus.on(EVENTS.ADD_OPTION, handleAdd);
    return () => eventBus.off(EVENTS.ADD_OPTION, handleAdd);
  }, [sendWs, connected, isLocked]);

  // 监听 EventBus：清空投票
  useEffect(() => {
    const handleClear = () => {
      if (!connected || !isAdmin) return;
      if (window.confirm('确定要清空所有投票吗？此操作不可撤销。')) {
        sendWs({ type: 'CLEAR' });
        setMyVoteId(null);
      }
    };
    eventBus.on(EVENTS.CLEAR_ALL, handleClear);
    return () => eventBus.off(EVENTS.CLEAR_ALL, handleClear);
  }, [sendWs, connected, isAdmin]);

  // 监听 EventBus：删除选项
  useEffect(() => {
    const handleDelete = (optionId: string) => {
      if (!connected || !isAdmin) return;
      if (window.confirm('确定要删除这个选项吗？')) {
        sendWs({ type: 'DELETE', data: optionId });
        if (myVoteId === optionId) setMyVoteId(null);
      }
    };
    eventBus.on(EVENTS.DELETE_OPTION, handleDelete);
    return () => eventBus.off(EVENTS.DELETE_OPTION, handleDelete);
  }, [sendWs, connected, isAdmin, myVoteId]);

  // 监听 EventBus：重命名选项
  useEffect(() => {
    const handleRename = (data: { id: string; name: string }) => {
      if (!connected || !isAdmin) return;
      sendWs({ type: 'RENAME', data });
    };
    eventBus.on(EVENTS.RENAME_OPTION, handleRename);
    return () => eventBus.off(EVENTS.RENAME_OPTION, handleRename);
  }, [sendWs, connected, isAdmin]);

  // 监听 EventBus：锁定/解锁
  useEffect(() => {
    const handleLock = (locked: boolean) => {
      if (!connected || !isAdmin) return;
      sendWs({ type: 'LOCK', data: locked });
    };
    eventBus.on(EVENTS.LOCK_VOTE, handleLock);
    return () => eventBus.off(EVENTS.LOCK_VOTE, handleLock);
  }, [sendWs, connected, isAdmin]);

  // 监听 EventBus：设置倒计时
  useEffect(() => {
    const handleSetTimer = (seconds: number) => {
      if (!connected || !isAdmin) return;
      sendWs({ type: 'SET_TIMER', data: seconds });
    };
    eventBus.on(EVENTS.SET_TIMER, handleSetTimer);
    return () => eventBus.off(EVENTS.SET_TIMER, handleSetTimer);
  }, [sendWs, connected, isAdmin]);

  // 监听 EventBus：管理员登录
  useEffect(() => {
    const handleAdminLogin = (password: string) => {
      if (!connected) return;
      sendWs({ type: 'ADMIN_LOGIN', data: password });
    };
    eventBus.on(EVENTS.ADMIN_LOGIN, handleAdminLogin);
    return () => eventBus.off(EVENTS.ADMIN_LOGIN, handleAdminLogin);
  }, [sendWs, connected]);

  // 监听 EventBus：管理员登出
  useEffect(() => {
    const handleAdminLogout = () => {
      setAdminToken(null);
      setIsAdmin(false);
      localStorage.removeItem('vote_admin_token');
    };
    eventBus.on(EVENTS.ADMIN_LOGOUT, handleAdminLogout);
    return () => eventBus.off(EVENTS.ADMIN_LOGOUT, handleAdminLogout);
  }, []);

  const totalVotes = options.reduce((sum, o) => sum + o.votes, 0);

  // 格式化倒计时显示
  const formatTime = (seconds: number): string => {
    if (seconds <= 0) return '—';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // 状态文字
  const getStatusText = () => {
    if (reconnecting) return `○ 正在重连…（第 ${reconnectAttemptsRef.current} 次）`;
    if (!connected) return '○ 连接断开';
    if (isLocked)
      return `● 已连接 · 投票已锁定${remainingSeconds > 0 ? ' · 倒计时已结束' : ''}`;
    if (remainingSeconds > 0)
      return `● 已连接 · 倒计时 ${formatTime(remainingSeconds)}`;
    return '● 已连接，数据实时同步';
  };

  const statusClass = reconnecting
    ? 'reconnecting'
    : connected
    ? 'connected'
    : 'disconnected';

  return (
    <div className="app-container">
      <div className="app-header">
        <h1>实时投票看板</h1>
        <p className={`status ${statusClass}`}>{getStatusText()}</p>
      </div>

      <div className="main-layout">
        <div className="panel">
          <div className="panel-title">
            投票选项
            {isLocked && (
              <span className="lock-badge">
                {remainingSeconds > 0 ? '⏱ 投票中' : '🔒 已锁定'}
              </span>
            )}
            {!isLocked && remainingSeconds > 0 && (
              <span className="timer-badge">⏱ {formatTime(remainingSeconds)}</span>
            )}
          </div>
          <OptionList
            options={options}
            myVoteId={myVoteId}
            isAdmin={isAdmin}
            isLocked={isLocked}
            connected={connected}
          />
          <div className="total-votes">总票数：{totalVotes}</div>
        </div>

        <div className="panel">
          <div className="panel-title">投票结果</div>
          <VotePanel
            options={options}
            totalVotes={totalVotes}
            myVoteId={myVoteId}
          />
          <AdminPanel isAdmin={isAdmin} isLocked={isLocked} connected={connected} />
        </div>
      </div>
    </div>
  );
}

export default App;
