package com.monitoring.model;

import java.util.ArrayList;
import java.util.List;

public class AlertCondition {

    private String id;
    private String name;
    private ConditionOperator operator;
    private List<ConditionRule> rules;
    private boolean enabled;

    public AlertCondition() {
        this.operator = ConditionOperator.AND;
        this.rules = new ArrayList<>();
        this.enabled = true;
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

    public ConditionOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }

    public List<ConditionRule> getRules() {
        return rules;
    }

    public void setRules(List<ConditionRule> rules) {
        this.rules = rules;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static class ConditionRule {
        private String monitorType;
        private ComparisonType comparison;
        private double threshold;
        private AlertLevel level;

        public ConditionRule() {
            this.level = AlertLevel.WARNING;
        }

        public String getMonitorType() {
            return monitorType;
        }

        public void setMonitorType(String monitorType) {
            this.monitorType = monitorType;
        }

        public ComparisonType getComparison() {
            return comparison;
        }

        public void setComparison(ComparisonType comparison) {
            this.comparison = comparison;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public AlertLevel getLevel() {
            return level;
        }

        public void setLevel(AlertLevel level) {
            this.level = level;
        }

        public boolean evaluate(double value) {
            switch (comparison) {
                case GREATER_THAN:
                    return value > threshold;
                case GREATER_THAN_OR_EQUAL:
                    return value >= threshold;
                case LESS_THAN:
                    return value < threshold;
                case LESS_THAN_OR_EQUAL:
                    return value <= threshold;
                case EQUAL:
                    return Math.abs(value - threshold) < 0.001;
                case NOT_EQUAL:
                    return Math.abs(value - threshold) >= 0.001;
                default:
                    return false;
            }
        }
    }

    public enum ConditionOperator {
        AND,
        OR
    }

    public enum ComparisonType {
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUAL("=="),
        NOT_EQUAL("!=");

        private final String symbol;

        ComparisonType(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
