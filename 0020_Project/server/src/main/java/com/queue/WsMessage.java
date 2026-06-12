/**
 * WsMessage WebSocket 消息类
 * 职责：定义前后端通信的消息格式，包含动作类型和载荷数据
 */
package com.queue;

import java.io.Serializable;

public class WsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

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
