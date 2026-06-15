package com.overtime.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonFileUtil {

    private static String dataDir;

    static {
        try {
            File classPath = ResourceUtils.getFile("classpath:");
            dataDir = classPath.getAbsolutePath() + File.separator + "data" + File.separator;
            File dir = new File(dataDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (FileNotFoundException e) {
            dataDir = "data" + File.separator;
            new File(dataDir).mkdirs();
        }
    }

    public static JSONArray readArray(String fileName) {
        File file = new File(dataDir + fileName);
        if (!file.exists()) {
            return new JSONArray();
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
            String content = sb.toString().trim();
            if (content.isEmpty()) {
                return new JSONArray();
            }
            return JSON.parseArray(content);
        } catch (IOException e) {
            return new JSONArray();
        }
    }

    public static void writeArray(String fileName, JSONArray array) {
        File file = new File(dataDir + fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(JSON.toJSONString(array, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDataDir() {
        return dataDir;
    }
}
