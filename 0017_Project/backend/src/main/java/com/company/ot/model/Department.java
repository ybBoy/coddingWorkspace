package com.company.ot.model;

public enum Department {
    TECH("技术部"),
    PRODUCT("产品部"),
    OPERATIONS("运营部"),
    HR("人事部");

    private final String label;

    Department(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
