package service;

import model.Recipe;
import store.JsonRecipeStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecipeService {
    private List<Recipe> recipes;
    private JsonRecipeStore store;

    public RecipeService() {
        this.store = JsonRecipeStore.getInstance();
        this.recipes = store.loadAll();
    }

    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }

    public Recipe addRecipe(String name, String taste, int estimatedTime, String mainIngredients, String notes) {
        String id = UUID.randomUUID().toString();
        Recipe recipe = new Recipe(id, name, taste, estimatedTime, mainIngredients, notes);
        recipes.add(recipe);
        saveToFile();
        return recipe;
    }

    public Recipe getRecipeById(String id) {
        for (Recipe recipe : recipes) {
            if (recipe.getId().equals(id)) {
                return recipe;
            }
        }
        return null;
    }

    public boolean updateRecipeNotes(String id, String notes) {
        Recipe recipe = getRecipeById(id);
        if (recipe != null) {
            recipe.setNotes(notes);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean updateRecipe(String id, String name, String taste, int estimatedTime, String mainIngredients, String notes) {
        Recipe recipe = getRecipeById(id);
        if (recipe != null) {
            recipe.setName(name);
            recipe.setTaste(taste);
            recipe.setEstimatedTime(estimatedTime);
            recipe.setMainIngredients(mainIngredients);
            recipe.setNotes(notes);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean deleteRecipe(String id) {
        Recipe recipe = getRecipeById(id);
        if (recipe != null) {
            recipes.remove(recipe);
            saveToFile();
            return true;
        }
        return false;
    }

    public List<Recipe> filterByTaste(String taste) {
        return recipes.stream()
                .filter(r -> r.getTaste().equals(taste))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByTime(int maxTime) {
        return recipes.stream()
                .filter(r -> r.getEstimatedTime() <= maxTime)
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByTasteAndTime(String taste, int maxTime) {
        return recipes.stream()
                .filter(r -> r.getTaste().equals(taste) && r.getEstimatedTime() <= maxTime)
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return recipes.size();
    }

    public int getCountUnder30Minutes() {
        return (int) recipes.stream()
                .filter(r -> r.getEstimatedTime() <= 30)
                .count();
    }

    private void saveToFile() {
        store.saveAll(recipes);
    }
}
