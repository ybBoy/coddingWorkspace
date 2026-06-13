package model;

public class ReminderConfig {
    private String action;
    private int intervalMinutes;
    private boolean enabled;

    public ReminderConfig() {}

    public ReminderConfig(String action, int intervalMinutes, boolean enabled) {
        this.action = action;
        this.intervalMinutes = intervalMinutes;
        this.enabled = enabled;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
