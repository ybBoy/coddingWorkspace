package service;

import model.Recipe;
import store.JsonRecipeStore;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeService {
    private List<Recipe> recipes;
    private JsonRecipeStore store;

    public RecipeService() {
        this.store = JsonRecipeStore.getInstance();
        this.recipes = store.loadAll();
    }

    public Map<String, String> validateRecipe(String name, Integer estimatedTime, String mainIngredients) {
        Map<String, String> errors = new HashMap<>();
        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "菜名不能为空");
        } else if (name.trim().length() > 50) {
            errors.put("name", "菜名不能超过50个字符");
        }
        if (estimatedTime == null) {
            errors.put("estimatedTime", "预计用时不能为空");
        } else if (estimatedTime <= 0) {
            errors.put("estimatedTime", "预计用时必须大于0分钟");
        } else if (estimatedTime > 1440) {
            errors.put("estimatedTime", "预计用时不能超过1440分钟（24小时）");
        }
        if (mainIngredients == null || mainIngredients.trim().isEmpty()) {
            errors.put("mainIngredients", "主要食材不能为空");
        }
        return errors;
    }

    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }

    public Recipe addRecipe(String name, String taste, String category, Integer difficulty,
                            Integer servings, Double cost, Integer rating,
                            int estimatedTime, String mainIngredients,
                            List<String> steps, String notes, String image) {
        String id = UUID.randomUUID().toString();
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setTaste(taste != null ? taste : "");
        recipe.setCategory(category != null ? category : "");
        recipe.setDifficulty(difficulty != null ? difficulty : 1);
        recipe.setServings(servings != null ? servings : 2);
        recipe.setCost(cost != null ? cost : 0.0);
        recipe.setRating(rating != null ? rating : 0);
        recipe.setEstimatedTime(estimatedTime);
        recipe.setMainIngredients(mainIngredients);
        recipe.setSteps(steps != null ? steps : new ArrayList<String>());
        recipe.setNotes(notes != null ? notes : "");
        recipe.setImage(image != null ? image : "");
        recipe.setCreatedAt(System.currentTimeMillis());
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

    public boolean updateRecipeFull(String id, String name, String taste, String category,
                                    Integer difficulty, Integer servings, Double cost,
                                    Integer rating, Integer estimatedTime, String mainIngredients,
                                    List<String> steps, String notes, String image, Long lastMadeAt) {
        Recipe recipe = getRecipeById(id);
        if (recipe == null) return false;
        if (name != null) recipe.setName(name);
        if (taste != null) recipe.setTaste(taste);
        if (category != null) recipe.setCategory(category);
        if (difficulty != null) recipe.setDifficulty(difficulty);
        if (servings != null) recipe.setServings(servings);
        if (cost != null) recipe.setCost(cost);
        if (rating != null) recipe.setRating(rating);
        if (estimatedTime != null) recipe.setEstimatedTime(estimatedTime);
        if (mainIngredients != null) recipe.setMainIngredients(mainIngredients);
        if (steps != null) recipe.setSteps(steps);
        if (notes != null) recipe.setNotes(notes);
        if (image != null) recipe.setImage(image);
        if (lastMadeAt != null) recipe.setLastMadeAt(lastMadeAt);
        saveToFile();
        return true;
    }

    public Recipe duplicateRecipe(String id) {
        Recipe original = getRecipeById(id);
        if (original == null) return null;
        Recipe copy = new Recipe();
        copy.setId(UUID.randomUUID().toString());
        copy.setName(original.getName() + " (副本)");
        copy.setTaste(original.getTaste());
        copy.setCategory(original.getCategory());
        copy.setDifficulty(original.getDifficulty());
        copy.setServings(original.getServings());
        copy.setCost(original.getCost());
        copy.setRating(original.getRating());
        copy.setEstimatedTime(original.getEstimatedTime());
        copy.setMainIngredients(original.getMainIngredients());
        copy.setSteps(new ArrayList<>(original.getSteps()));
        copy.setNotes(original.getNotes());
        copy.setImage(original.getImage());
        copy.setCreatedAt(System.currentTimeMillis());
        copy.setLastMadeAt(null);
        recipes.add(copy);
        saveToFile();
        return copy;
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

    public boolean markAsMade(String id) {
        Recipe recipe = getRecipeById(id);
        if (recipe != null) {
            recipe.setLastMadeAt(System.currentTimeMillis());
            saveToFile();
            return true;
        }
        return false;
    }

    public List<Recipe> search(String keyword, String taste, String category,
                               Integer maxTime, String sortBy, String sortOrder,
                               String ingredients) {
        List<Recipe> result = new ArrayList<>(recipes);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            result = result.stream().filter(r ->
                r.getName().toLowerCase().contains(kw) ||
                r.getMainIngredients().toLowerCase().contains(kw) ||
                (r.getNotes() != null && r.getNotes().toLowerCase().contains(kw)) ||
                (r.getCategory() != null && r.getCategory().toLowerCase().contains(kw)) ||
                containsStepKeyword(r, kw)
            ).collect(Collectors.toList());
        }

        if (taste != null && !taste.trim().isEmpty()) {
            result = result.stream()
                    .filter(r -> taste.equals(r.getTaste()))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.trim().isEmpty()) {
            result = result.stream()
                    .filter(r -> category.equals(r.getCategory()))
                    .collect(Collectors.toList());
        }

        if (maxTime != null && maxTime > 0) {
            result = result.stream()
                    .filter(r -> r.getEstimatedTime() <= maxTime)
                    .collect(Collectors.toList());
        }

        if (ingredients != null && !ingredients.trim().isEmpty()) {
            Set<String> haveSet = new HashSet<>();
            for (String ing : ingredients.split("[,，、\\s]+")) {
                if (!ing.trim().isEmpty()) haveSet.add(ing.trim().toLowerCase());
            }
            result = result.stream().filter(r -> {
                Set<String> need = new HashSet<>();
                for (String ing : r.getMainIngredients().split("[,，、\\s]+")) {
                    if (!ing.trim().isEmpty()) need.add(ing.trim().toLowerCase());
                }
                int match = 0;
                for (String n : need) {
                    for (String h : haveSet) {
                        if (n.contains(h) || h.contains(n)) { match++; break; }
                    }
                }
                return match > 0 && match * 2 >= need.size();
            }).collect(Collectors.toList());
        }

        if (sortBy == null) sortBy = "createdAt";
        if (sortOrder == null) sortOrder = "desc";
        final String sb = sortBy;
        final String so = sortOrder;

        result.sort((a, b) -> {
            int cmp = 0;
            switch (sb) {
                case "time":
                    cmp = Integer.compare(a.getEstimatedTime(), b.getEstimatedTime());
                    break;
                case "taste":
                    cmp = (a.getTaste() == null ? "" : a.getTaste())
                            .compareTo(b.getTaste() == null ? "" : b.getTaste());
                    break;
                case "category":
                    cmp = (a.getCategory() == null ? "" : a.getCategory())
                            .compareTo(b.getCategory() == null ? "" : b.getCategory());
                    break;
                case "difficulty":
                    cmp = Integer.compare(a.getDifficulty(), b.getDifficulty());
                    break;
                case "rating":
                    cmp = Integer.compare(a.getRating(), b.getRating());
                    break;
                case "name":
                    cmp = a.getName().compareTo(b.getName());
                    break;
                case "createdAt":
                default:
                    cmp = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                    break;
            }
            return "asc".equals(so) ? cmp : -cmp;
        });

        return result;
    }

    private boolean containsStepKeyword(Recipe r, String kw) {
        if (r.getSteps() == null) return false;
        for (String s : r.getSteps()) {
            if (s.toLowerCase().contains(kw)) return true;
        }
        return false;
    }

    public List<String> generateShoppingList(List<String> recipeIds) {
        Set<String> ingredients = new LinkedHashSet<>();
        for (String id : recipeIds) {
            Recipe r = getRecipeById(id);
            if (r != null) {
                for (String ing : r.getMainIngredients().split("[,，、\\s]+")) {
                    if (!ing.trim().isEmpty()) {
                        ingredients.add(ing.trim());
                    }
                }
            }
        }
        return new ArrayList<>(ingredients);
    }

    public int getTotalCount() {
        return recipes.size();
    }

    public int getCountUnder30Minutes() {
        return (int) recipes.stream()
                .filter(r -> r.getEstimatedTime() <= 30)
                .count();
    }

    public String exportJson() {
        return store.exportToJson();
    }

    public boolean importJson(String json, boolean merge) {
        if (merge) {
            try {
                org.json.JSONArray array = new org.json.JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject obj = array.getJSONObject(i);
                    if (!obj.has("name")) continue;
                    Recipe recipe = new Recipe();
                    recipe.setId(UUID.randomUUID().toString());
                    recipe.setName(obj.getString("name"));
                    recipe.setTaste(obj.optString("taste", ""));
                    recipe.setCategory(obj.optString("category", ""));
                    recipe.setDifficulty(obj.optInt("difficulty", 1));
                    recipe.setServings(obj.optInt("servings", 2));
                    recipe.setCost(obj.optDouble("cost", 0.0));
                    recipe.setRating(obj.optInt("rating", 0));
                    recipe.setEstimatedTime(obj.optInt("estimatedTime", 0));
                    recipe.setMainIngredients(obj.optString("mainIngredients", ""));
                    List<String> steps = new ArrayList<>();
                    org.json.JSONArray stepsArr = obj.optJSONArray("steps");
                    if (stepsArr != null) {
                        for (int j = 0; j < stepsArr.length(); j++) {
                            steps.add(stepsArr.getString(j));
                        }
                    }
                    recipe.setSteps(steps);
                    recipe.setNotes(obj.optString("notes", ""));
                    recipe.setImage(obj.optString("image", ""));
                    recipe.setCreatedAt(System.currentTimeMillis());
                    recipes.add(recipe);
                }
                saveToFile();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            boolean ok = store.importFromJson(json);
            if (ok) {
                this.recipes = store.loadAll();
            }
            return ok;
        }
    }

    private void saveToFile() {
        store.saveAll(recipes);
    }
}
