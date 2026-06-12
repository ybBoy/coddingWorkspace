/**
 * CallRecord 叫号记录实体类
 * 职责：记录每次叫号、完成、过号、重新叫号的操作历史
 */
package com.queue;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;

public class CallRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Ticket ticket;

    private String counterName;

    private String action;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long timestamp;

    public CallRecord() {
    }

    public CallRecord(Ticket ticket, String counterName, String action, long timestamp) {
        this.ticket = ticket;
        this.counterName = counterName;
        this.action = action;
        this.timestamp = timestamp;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
