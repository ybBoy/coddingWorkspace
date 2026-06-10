package com.company.ot.model;

public enum Role {
    EMPLOYEE("普通员工"),
    MANAGER("部门主管");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
