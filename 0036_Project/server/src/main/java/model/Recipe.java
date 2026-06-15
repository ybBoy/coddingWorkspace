package model;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    private String id;
    private String name;
    private String taste;
    private String category;
    private int difficulty;
    private int servings;
    private double cost;
    private int rating;
    private int estimatedTime;
    private String mainIngredients;
    private List<String> steps;
    private String notes;
    private String image;
    private long createdAt;
    private Long lastMadeAt;

    public Recipe() {
        this.steps = new ArrayList<>();
        this.rating = 0;
        this.difficulty = 1;
        this.servings = 2;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaste() { return taste; }
    public void setTaste(String taste) { this.taste = taste; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(int estimatedTime) { this.estimatedTime = estimatedTime; }

    public String getMainIngredients() { return mainIngredients; }
    public void setMainIngredients(String mainIngredients) { this.mainIngredients = mainIngredients; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getLastMadeAt() { return lastMadeAt; }
    public void setLastMadeAt(Long lastMadeAt) { this.lastMadeAt = lastMadeAt; }
}
