/**
 * TicketForm 取号表单组件
 * 职责：用户选择业务类型后取号，通过 EventBus 发布取号事件
 * 数据流：用户点击取号 -> EventBus.TAKE_TICKET -> App.tsx -> WebSocket -> 后端
 */
import React, { useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { BusinessType } from './types';

const TicketForm: React.FC = () => {
  const [businessType, setBusinessType] = useState<BusinessType>('咨询');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    eventBus.emit(EVENTS.TAKE_TICKET, { businessType });
    setTimeout(() => setIsSubmitting(false), 500);
  };

  return (
    <div className="ticket-form">
      <h2 className="section-title">取号</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="businessType">业务类型</label>
          <select
            id="businessType"
            value={businessType}
            onChange={(e) => setBusinessType(e.target.value as BusinessType)}
          >
            <option value="咨询">咨询</option>
            <option value="办理">办理</option>
            <option value="售后">售后</option>
          </select>
        </div>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting}
        >
          {isSubmitting ? '取号中...' : '取号'}
        </button>
      </form>
    </div>
  );
};

export default TicketForm;
