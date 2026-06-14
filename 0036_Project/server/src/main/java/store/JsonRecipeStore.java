package store;

import model.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonRecipeStore {
    private static final String DATA_FILE = "recipes.json";
    private static JsonRecipeStore instance;

    private JsonRecipeStore() {
        ensureDataFileExists();
    }

    public static synchronized JsonRecipeStore getInstance() {
        if (instance == null) {
            instance = new JsonRecipeStore();
        }
        return instance;
    }

    private void ensureDataFileExists() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
                Files.write(Paths.get(DATA_FILE), "[]".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Recipe> loadAll() {
        List<Recipe> recipes = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(DATA_FILE)), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                content = "[]";
            }
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Recipe recipe = new Recipe();
                recipe.setId(obj.getString("id"));
                recipe.setName(obj.getString("name"));
                recipe.setTaste(obj.getString("taste"));
                recipe.setEstimatedTime(obj.getInt("estimatedTime"));
                recipe.setMainIngredients(obj.getString("mainIngredients"));
                recipe.setNotes(obj.optString("notes", ""));
                recipe.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
                recipes.add(recipe);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recipes;
    }

    public void saveAll(List<Recipe> recipes) {
        JSONArray array = new JSONArray();
        for (Recipe recipe : recipes) {
            JSONObject obj = new JSONObject();
            obj.put("id", recipe.getId());
            obj.put("name", recipe.getName());
            obj.put("taste", recipe.getTaste());
            obj.put("estimatedTime", recipe.getEstimatedTime());
            obj.put("mainIngredients", recipe.getMainIngredients());
            obj.put("notes", recipe.getNotes());
            obj.put("createdAt", recipe.getCreatedAt());
            array.put(obj);
        }
        try {
            Files.write(Paths.get(DATA_FILE), array.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
