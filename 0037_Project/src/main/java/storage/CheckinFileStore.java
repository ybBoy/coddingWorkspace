package storage;

import domain.FitnessCheckin;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class CheckinFileStore {
    private final File dataFile;
    private final File tmpFile;
    private final File backupFile;

    public CheckinFileStore(String filePath) {
        this.dataFile = new File(filePath);
        this.tmpFile = new File(filePath + ".tmp");
        this.backupFile = new File(filePath + ".bak");
    }

    public List<FitnessCheckin> loadAll() {
        if (!dataFile.exists()) {
            if (backupFile.exists()) {
                System.out.println("主文件不存在，尝试从备份文件恢复: " + backupFile.getName());
                try {
                    Files.copy(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("从备份恢复失败: " + e.getMessage());
                }
            } else {
                return new java.util.ArrayList<>();
            }
        }

        try {
            String content = readFile(dataFile);
            return JsonUtil.parseList(content);
        } catch (Exception e) {
            System.err.println("主文件解析失败，尝试读取备份文件: " + e.getMessage());
            if (backupFile.exists()) {
                try {
                    String content = readFile(backupFile);
                    List<FitnessCheckin> result = JsonUtil.parseList(content);
                    if (!result.isEmpty()) {
                        System.out.println("从备份文件恢复成功，共 " + result.size() + " 条记录");
                        return result;
                    }
                } catch (Exception ex) {
                    System.err.println("备份文件也解析失败: " + ex.getMessage());
                }
            }
            return new java.util.ArrayList<>();
        }
    }

    public void saveAll(List<FitnessCheckin> records) {
        try {
            File parentDir = dataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String jsonContent = JsonUtil.toJson(records);

        synchronized (this) {
            try {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8))) {
                    writer.write(jsonContent);
                    writer.flush();
                    try {
                        writer.flush();
                    } catch (IOException e) {
                    }
                }

                if (tmpFile.length() == 0) {
                    throw new IOException("临时文件为空，写入可能失败");
                }

                if (dataFile.exists()) {
                    Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                Files.move(tmpFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            } catch (IOException e) {
                System.err.println("保存文件失败: " + e.getMessage());
                e.printStackTrace();

                if (tmpFile.exists()) {
                    try {
                        tmpFile.delete();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    public String getFilePath() {
        return dataFile.getAbsolutePath();
    }
}
