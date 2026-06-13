package com.demo.model;

public class RoastLevel {
    public static final String LIGHT = "LIGHT";
    public static final String MEDIUM = "MEDIUM";
    public static final String MEDIUM_DARK = "MEDIUM_DARK";
    public static final String DARK = "DARK";

    public static boolean isValid(String level) {
        return LIGHT.equals(level) || MEDIUM.equals(level) || MEDIUM_DARK.equals(level) || DARK.equals(level);
    }
}
