package store;

import model.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JsonRecipeStore {
    private static final String DATA_FILE = "recipes.json";
    private static final String BACKUP_DIR = "backup";
    private static JsonRecipeStore instance;

    private JsonRecipeStore() {
        ensureDataFileExists();
        ensureBackupDirExists();
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

    private void ensureBackupDirExists() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
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
                recipe.setTaste(obj.optString("taste", ""));
                recipe.setCategory(obj.optString("category", ""));
                recipe.setDifficulty(obj.optInt("difficulty", 1));
                recipe.setServings(obj.optInt("servings", 2));
                recipe.setCost(obj.optDouble("cost", 0.0));
                recipe.setRating(obj.optInt("rating", 0));
                recipe.setEstimatedTime(obj.optInt("estimatedTime", 0));
                recipe.setMainIngredients(obj.optString("mainIngredients", ""));
                JSONArray stepsArr = obj.optJSONArray("steps");
                List<String> steps = new ArrayList<>();
                if (stepsArr != null) {
                    for (int j = 0; j < stepsArr.length(); j++) {
                        steps.add(stepsArr.getString(j));
                    }
                }
                recipe.setSteps(steps);
                recipe.setNotes(obj.optString("notes", ""));
                recipe.setImage(obj.optString("image", ""));
                recipe.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
                if (obj.has("lastMadeAt") && !obj.isNull("lastMadeAt")) {
                    recipe.setLastMadeAt(obj.getLong("lastMadeAt"));
                }
                recipes.add(recipe);
            }
        } catch (Exception e) {
            System.err.println("加载数据失败，尝试从备份恢复...");
            Recipe restored = tryRestoreFromBackup();
            if (restored != null) {
                recipes.add(restored);
            }
            e.printStackTrace();
        }
        return recipes;
    }

    private Recipe tryRestoreFromBackup() {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("recipes_") && name.endsWith(".json"));
        if (files != null && files.length > 0) {
            File latest = files[0];
            for (File f : files) {
                if (f.lastModified() > latest.lastModified()) latest = f;
            }
            try {
                String content = new String(Files.readAllBytes(latest.toPath()), StandardCharsets.UTF_8);
                System.out.println("已从备份恢复: " + latest.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void saveAll(List<Recipe> recipes) {
        createBackup();
        JSONArray array = new JSONArray();
        for (Recipe recipe : recipes) {
            JSONObject obj = new JSONObject();
            obj.put("id", recipe.getId());
            obj.put("name", recipe.getName());
            obj.put("taste", recipe.getTaste());
            obj.put("category", recipe.getCategory());
            obj.put("difficulty", recipe.getDifficulty());
            obj.put("servings", recipe.getServings());
            obj.put("cost", recipe.getCost());
            obj.put("rating", recipe.getRating());
            obj.put("estimatedTime", recipe.getEstimatedTime());
            obj.put("mainIngredients", recipe.getMainIngredients());
            JSONArray stepsArr = new JSONArray();
            if (recipe.getSteps() != null) {
                for (String step : recipe.getSteps()) {
                    stepsArr.put(step);
                }
            }
            obj.put("steps", stepsArr);
            obj.put("notes", recipe.getNotes());
            obj.put("image", recipe.getImage());
            obj.put("createdAt", recipe.getCreatedAt());
            if (recipe.getLastMadeAt() != null) {
                obj.put("lastMadeAt", recipe.getLastMadeAt());
            } else {
                obj.put("lastMadeAt", JSONObject.NULL);
            }
            array.put(obj);
        }
        try {
            Files.write(Paths.get(DATA_FILE), array.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createBackup() {
        File source = new File(DATA_FILE);
        if (!source.exists()) return;
        ensureBackupDirExists();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backup = new File(BACKUP_DIR, "recipes_" + timestamp + ".json");
        try {
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cleanupOldBackups();
        } catch (IOException e) {
            System.err.println("备份失败: " + e.getMessage());
        }
    }

    private void cleanupOldBackups() {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("recipes_") && name.endsWith(".json"));
        if (files != null && files.length > 10) {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = 10; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    public String exportToJson() {
        try {
            return new String(Files.readAllBytes(Paths.get(DATA_FILE)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[]";
        }
    }

    public boolean importFromJson(String jsonStr) {
        try {
            JSONArray array = new JSONArray(jsonStr);
            List<Recipe> imported = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.has("id") || !obj.has("name")) continue;
                Recipe recipe = new Recipe();
                recipe.setId(obj.getString("id"));
                recipe.setName(obj.getString("name"));
                recipe.setTaste(obj.optString("taste", ""));
                recipe.setCategory(obj.optString("category", ""));
                recipe.setDifficulty(obj.optInt("difficulty", 1));
                recipe.setServings(obj.optInt("servings", 2));
                recipe.setCost(obj.optDouble("cost", 0.0));
                recipe.setRating(obj.optInt("rating", 0));
                recipe.setEstimatedTime(obj.optInt("estimatedTime", 0));
                recipe.setMainIngredients(obj.optString("mainIngredients", ""));
                JSONArray stepsArr = obj.optJSONArray("steps");
                List<String> steps = new ArrayList<>();
                if (stepsArr != null) {
                    for (int j = 0; j < stepsArr.length(); j++) {
                        steps.add(stepsArr.getString(j));
                    }
                }
                recipe.setSteps(steps);
                recipe.setNotes(obj.optString("notes", ""));
                recipe.setImage(obj.optString("image", ""));
                recipe.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
                if (obj.has("lastMadeAt") && !obj.isNull("lastMadeAt")) {
                    recipe.setLastMadeAt(obj.getLong("lastMadeAt"));
                }
                imported.add(recipe);
            }
            if (!imported.isEmpty()) {
                saveAll(imported);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
