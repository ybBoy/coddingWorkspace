package model;

public class Recipe {
    private String id;
    private String name;
    private String taste;
    private int estimatedTime;
    private String mainIngredients;
    private String notes;
    private long createdAt;

    public Recipe() {
    }

    public Recipe(String id, String name, String taste, int estimatedTime, String mainIngredients, String notes) {
        this.id = id;
        this.name = name;
        this.taste = taste;
        this.estimatedTime = estimatedTime;
        this.mainIngredients = mainIngredients;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
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

    public String getTaste() {
        return taste;
    }

    public void setTaste(String taste) {
        this.taste = taste;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public String getMainIngredients() {
        return mainIngredients;
    }

    public void setMainIngredients(String mainIngredients) {
        this.mainIngredients = mainIngredients;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
