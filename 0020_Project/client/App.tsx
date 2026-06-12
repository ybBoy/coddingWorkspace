/**
 * App 主组件
 * 职责：
 * 1. 初始化 WebSocket 连接（StrictMode 下使用 ref 防重复）
 * 2. 订阅 EventBus 上的用户操作事件，转发到后端
 * 3. 整合所有子组件布局：
 *    - 顶部：Header 当前叫号
 *    - 中间左侧：WaitingQueue 等待队列
 *    - 中间右侧：TicketForm 取号 + CounterPanel 窗口操作
 *    - 底部：CallRecords 叫号记录
 *
 * 完整数据流：
 * 用户操作 -> 组件发布 EventBus 事件
 *   -> App.tsx 订阅事件 -> WebSocket.send() 发送到 Java 后端
 *     -> QueueService 更新内存队列
 *       -> FileStore 定时保存到本地 JSON 文件
 *         -> 后端 WebSocket 广播 STATE_UPDATE 给所有前端
 *           -> websocket.ts 接收消息 -> EventBus 发布 QUEUE_STATE_UPDATED
 *             -> 各组件订阅事件更新 UI
 */
import React, { useEffect, useRef } from 'react';
import Header from './Header';
import WaitingQueue from './WaitingQueue';
import CallRecords from './CallRecords';
import CounterPanel from './CounterPanel';
import TicketForm from './TicketForm';
import { eventBus, EVENTS } from './EventBus';
import { wsClient } from './websocket';
import { TakeTicketPayload, CounterActionPayload } from './types';

const App: React.FC = () => {
  const connectedRef = useRef(false);

  useEffect(() => {
    if (connectedRef.current) return;
    connectedRef.current = true;

    wsClient.connect();

    const unsubscribers = [
      eventBus.on(EVENTS.TAKE_TICKET, (data: TakeTicketPayload) => {
        wsClient.send({
          action: 'TAKE_TICKET',
          payload: data,
        });
      }),

      eventBus.on(EVENTS.CALL_NEXT, (data: CounterActionPayload) => {
        wsClient.send({
          action: 'CALL_NEXT',
          payload: data,
        });
      }),

      eventBus.on(EVENTS.COMPLETE_TICKET, (data: CounterActionPayload) => {
        wsClient.send({
          action: 'COMPLETE',
          payload: data,
        });
      }),

      eventBus.on(EVENTS.MISS_TICKET, (data: CounterActionPayload) => {
        wsClient.send({
          action: 'MISS',
          payload: data,
        });
      }),

      eventBus.on(EVENTS.RECALL_TICKET, (data: CounterActionPayload) => {
        wsClient.send({
          action: 'RECALL',
          payload: data,
        });
      }),
    ];

    return () => {
      unsubscribers.forEach((unsub) => unsub());
      wsClient.close();
      connectedRef.current = false;
    };
  }, []);

  return (
    <div className="app-container">
      <Header />
      <div className="main-content">
        <div className="content-left">
          <WaitingQueue />
        </div>
        <div className="content-right">
          <TicketForm />
          <CounterPanel />
        </div>
      </div>
      <CallRecords />
    </div>
  );
};

export default App;
