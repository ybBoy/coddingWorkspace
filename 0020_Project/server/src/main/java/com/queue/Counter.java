/**
 * Counter 窗口实体类
 * 职责：表示一个业务办理窗口，包含窗口ID、名称、状态、当前正在处理的号票
 * 迭代新增：enabled 启用状态、supportedBusinessTypes 支持的业务类型列表
 */
package com.queue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Counter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String status;

    private boolean enabled;

    private List<String> supportedBusinessTypes;

    private Ticket currentTicket;

    public Counter() {
        this.enabled = true;
        this.supportedBusinessTypes = new ArrayList<>();
        this.supportedBusinessTypes.add("咨询");
        this.supportedBusinessTypes.add("办理");
        this.supportedBusinessTypes.add("售后");
    }

    public Counter(String id, String name) {
        this();
        this.id = id;
        this.name = name;
        this.status = "idle";
    }

    public Counter(String id, String name, List<String> supportedBusinessTypes) {
        this.id = id;
        this.name = name;
        this.status = "idle";
        this.enabled = true;
        this.supportedBusinessTypes = supportedBusinessTypes;
    }

    /**
     * 深拷贝构造方法
     */
    public Counter(Counter other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.status = other.status;
            this.enabled = other.enabled;
            this.supportedBusinessTypes = other.supportedBusinessTypes != null
                    ? new ArrayList<>(other.supportedBusinessTypes)
                    : new ArrayList<>();
            this.currentTicket = other.currentTicket != null
                    ? new Ticket(other.currentTicket)
                    : null;
        }
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getSupportedBusinessTypes() {
        return supportedBusinessTypes;
    }

    public void setSupportedBusinessTypes(List<String> supportedBusinessTypes) {
        this.supportedBusinessTypes = supportedBusinessTypes;
    }

    public Ticket getCurrentTicket() {
        return currentTicket;
    }

    public void setCurrentTicket(Ticket currentTicket) {
        this.currentTicket = currentTicket;
    }
}
