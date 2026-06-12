/**
 * WsMessage WebSocket 消息类
 * 职责：定义前后端通信的消息格式，包含动作类型和载荷数据
 * 迭代新增：OPERATION_RESULT 操作结果反馈，用于前端 Toast 提示
 */
package com.queue;

import java.io.Serializable;

public class WsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String GET_STATE = "GET_STATE";
    public static final String TAKE_TICKET = "TAKE_TICKET";
    public static final String CALL_NEXT = "CALL_NEXT";
    public static final String CALL_NEXT_BY_TYPE = "CALL_NEXT_BY_TYPE";
    public static final String COMPLETE = "COMPLETE";
    public static final String MISS = "MISS";
    public static final String RECALL = "RECALL";
    public static final String REQUEUE_MISSED = "REQUEUE_MISSED";
    public static final String FINISH_MISSED = "FINISH_MISSED";
    public static final String ADD_COUNTER = "ADD_COUNTER";
    public static final String UPDATE_COUNTER = "UPDATE_COUNTER";
    public static final String TOGGLE_COUNTER = "TOGGLE_COUNTER";
    public static final String OPERATION_RESULT = "OPERATION_RESULT";
    public static final String STATE_UPDATE = "STATE_UPDATE";

    private String action;

    private Object payload;

    public WsMessage() {
    }

    public WsMessage(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
