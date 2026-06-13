package model;

public class Visitor {
    private String name;
    private String phoneSuffix;

    public Visitor() {
    }

    public Visitor(String name, String phoneSuffix) {
        this.name = name;
        this.phoneSuffix = phoneSuffix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneSuffix() {
        return phoneSuffix;
    }

    public void setPhoneSuffix(String phoneSuffix) {
        this.phoneSuffix = phoneSuffix;
    }
}
