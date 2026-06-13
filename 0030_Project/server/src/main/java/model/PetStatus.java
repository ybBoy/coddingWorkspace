package model;

public enum PetStatus {
    NORMAL("正常"),
    NEED_ATTENTION("需要关注"),
    PICKED_UP("已接走");

    private final String label;

    PetStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
