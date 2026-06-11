import { useEffect, useState, useRef, useCallback } from 'react';
import { eventBus, EVENTS } from './EventBus';
import OptionList from './OptionList';
import VotePanel from './VotePanel';

/**
 * 投票选项的数据结构
 */
export interface VoteOptionData {
  id: string;
  name: string;
  votes: number;
}

/**
 * WebSocket 消息结构
 */
interface WsMessage {
  type: 'INIT' | 'VOTE' | 'ADD' | 'CLEAR' | 'UPDATE';
  data?: any;
}

/**
 * App 主组件职责：
 * - 建立和维护 WebSocket 连接
 * - 持有全局投票数据状态
 * - 监听 EventBus 的用户操作事件，转发给后端
 * - 接收后端广播，更新状态并通知组件重渲染
 */
function App() {
  const [options, setOptions] = useState<VoteOptionData[]>([]);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  // 发送消息到后端
  const sendWs = useCallback((msg: WsMessage) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  // 初始化 WebSocket 连接
  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
      eventBus.emit(EVENTS.WS_CONNECTED);
    };

    ws.onclose = () => {
      setConnected(false);
    };

    ws.onerror = () => {
      eventBus.emit(EVENTS.WS_ERROR);
      setConnected(false);
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        if (msg.type === 'INIT' || msg.type === 'UPDATE') {
          const list: VoteOptionData[] = msg.data || [];
          setOptions(list);
          eventBus.emit(EVENTS.DATA_UPDATED, list);
        }
      } catch (e) {
        console.error('解析 WebSocket 消息失败', e);
      }
    };

    return () => {
      ws.close();
    };
  }, []);

  // 监听 EventBus：用户投票
  useEffect(() => {
    const handleVote = (optionId: string) => {
      sendWs({ type: 'VOTE', data: optionId });
    };
    eventBus.on(EVENTS.VOTE, handleVote);
    return () => eventBus.off(EVENTS.VOTE, handleVote);
  }, [sendWs]);

  // 监听 EventBus：新增选项
  useEffect(() => {
    const handleAdd = (name: string) => {
      sendWs({ type: 'ADD', data: name });
    };
    eventBus.on(EVENTS.ADD_OPTION, handleAdd);
    return () => eventBus.off(EVENTS.ADD_OPTION, handleAdd);
  }, [sendWs]);

  // 监听 EventBus：清空投票
  useEffect(() => {
    const handleClear = () => {
      sendWs({ type: 'CLEAR' });
    };
    eventBus.on(EVENTS.CLEAR_ALL, handleClear);
    return () => eventBus.off(EVENTS.CLEAR_ALL, handleClear);
  }, [sendWs]);

  const totalVotes = options.reduce((sum, o) => sum + o.votes, 0);

  return (
    <div className="app-container">
      <div className="app-header">
        <h1>实时投票看板</h1>
        <p className={`status ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? '● 已连接，数据实时同步' : '○ 连接断开，正在重连…'}
        </p>
      </div>

      <div className="main-layout">
        <div className="panel">
          <div className="panel-title">投票选项</div>
          <OptionList options={options} />
          <div className="total-votes">总票数：{totalVotes}</div>
        </div>

        <div className="panel">
          <div className="panel-title">投票结果</div>
          <VotePanel options={options} totalVotes={totalVotes} />
        </div>
      </div>
    </div>
  );
}

export default App;
