package com.danmaku;

public class OperationLog {
    private long timestamp;
    private String action;
    private String operator;
    private String detail;

    public OperationLog() {}

    public OperationLog(long timestamp, String action, String operator, String detail) {
        this.timestamp = timestamp;
        this.action = action;
        this.operator = operator;
        this.detail = detail;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
