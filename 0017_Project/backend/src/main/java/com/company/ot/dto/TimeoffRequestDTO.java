package com.company.ot.dto;

import com.company.ot.model.TimeoffType;

import java.time.LocalDate;

public class TimeoffRequestDTO {
    private Long userId;
    private LocalDate timeoffDate;
    private TimeoffType timeoffType;
    private String reason;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getTimeoffDate() {
        return timeoffDate;
    }

    public void setTimeoffDate(LocalDate timeoffDate) {
        this.timeoffDate = timeoffDate;
    }

    public TimeoffType getTimeoffType() {
        return timeoffType;
    }

    public void setTimeoffType(TimeoffType timeoffType) {
        this.timeoffType = timeoffType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
