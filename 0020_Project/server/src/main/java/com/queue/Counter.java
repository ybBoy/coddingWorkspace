/**
 * Counter 窗口实体类
 * 职责：表示办理窗口，包含窗口ID、名称、状态、当前办理的号票等信息
 */
package com.queue;

import java.io.Serializable;

public class Counter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String status;

    private Ticket currentTicket;

    public Counter() {
    }

    public Counter(String id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Ticket getCurrentTicket() {
        return currentTicket;
    }

    public void setCurrentTicket(Ticket currentTicket) {
        this.currentTicket = currentTicket;
    }
}
