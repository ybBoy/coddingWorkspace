/**
 * Ticket 号票实体类
 * 职责：表示用户取的号票，包含号码、业务类型、状态、创建时间等信息
 */
package com.queue;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;

public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private int number;

    private String businessType;

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long createdAt;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long calledAt;

    private String counterId;

    public Ticket() {
    }

    public Ticket(String id, int number, String businessType, String status, long createdAt) {
        this.id = id;
        this.number = number;
        this.businessType = businessType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Long calledAt) {
        this.calledAt = calledAt;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }
}
