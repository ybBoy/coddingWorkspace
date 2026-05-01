package com.monitoring.service;

import com.monitoring.model.MonitorItem;
import com.monitoring.model.ServerConfig;
import com.monitoring.storage.JsonFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerConfigService {

    private static final String SERVER_CONFIG_FILE = "server_configs.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    private List<ServerConfig> serverConfigs;

    @PostConstruct
    public void init() {
        loadConfigs();
        if (serverConfigs.isEmpty()) {
            initializeSampleData();
        }
    }

    private void loadConfigs() {
        serverConfigs = jsonFileStorage.loadList(SERVER_CONFIG_FILE, ServerConfig.class);
        if (serverConfigs == null) {
            serverConfigs = new ArrayList<>();
        }
    }

    private void saveConfigs() {
        jsonFileStorage.save(SERVER_CONFIG_FILE, serverConfigs);
    }

    private void initializeSampleData() {
        ServerConfig dbServer = new ServerConfig();
        dbServer.setId(UUID.randomUUID().toString());
        dbServer.setName("数据库服务器");
        dbServer.setIpAddress("192.168.1.100");
        dbServer.setDescription("MySQL主数据库服务器");
        dbServer.getMonitorItems().add(MonitorItem.createCpuItem());
        dbServer.getMonitorItems().add(MonitorItem.createMemoryItem());
        dbServer.getMonitorItems().add(MonitorItem.createDiskItem());
        dbServer.setCreateTime(LocalDateTime.now());
        dbServer.setUpdateTime(LocalDateTime.now());

        ServerConfig appServer = new ServerConfig();
        appServer.setId(UUID.randomUUID().toString());
        appServer.setName("应用服务器");
        appServer.setIpAddress("192.168.1.101");
        appServer.setDescription("Web应用服务器");
        appServer.getMonitorItems().add(MonitorItem.createCpuItem());
        appServer.getMonitorItems().add(MonitorItem.createMemoryItem());
        appServer.setCreateTime(LocalDateTime.now());
        appServer.setUpdateTime(LocalDateTime.now());

        ServerConfig cacheServer = new ServerConfig();
        cacheServer.setId(UUID.randomUUID().toString());
        cacheServer.setName("缓存服务器");
        cacheServer.setIpAddress("192.168.1.102");
        cacheServer.setDescription("Redis缓存服务器");
        cacheServer.getMonitorItems().add(MonitorItem.createCpuItem());
        cacheServer.getMonitorItems().add(MonitorItem.createMemoryItem());
        cacheServer.setCreateTime(LocalDateTime.now());
        cacheServer.setUpdateTime(LocalDateTime.now());

        serverConfigs.add(dbServer);
        serverConfigs.add(appServer);
        serverConfigs.add(cacheServer);
        saveConfigs();
    }

    public List<ServerConfig> getAllServers() {
        return new ArrayList<>(serverConfigs);
    }

    public List<ServerConfig> getEnabledServers() {
        List<ServerConfig> enabled = new ArrayList<>();
        for (ServerConfig config : serverConfigs) {
            if (config.isEnabled()) {
                enabled.add(config);
            }
        }
        return enabled;
    }

    public Optional<ServerConfig> getServerById(String id) {
        for (ServerConfig config : serverConfigs) {
            if (config.getId().equals(id)) {
                return Optional.of(config);
            }
        }
        return Optional.empty();
    }

    public ServerConfig createServer(ServerConfig config) {
        config.setId(UUID.randomUUID().toString());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        serverConfigs.add(config);
        saveConfigs();
        return config;
    }

    public Optional<ServerConfig> updateServer(String id, ServerConfig updatedConfig) {
        for (int i = 0; i < serverConfigs.size(); i++) {
            ServerConfig existing = serverConfigs.get(i);
            if (existing.getId().equals(id)) {
                existing.setName(updatedConfig.getName());
                existing.setIpAddress(updatedConfig.getIpAddress());
                existing.setDescription(updatedConfig.getDescription());
                existing.setEnabled(updatedConfig.isEnabled());
                if (updatedConfig.getMonitorItems() != null && !updatedConfig.getMonitorItems().isEmpty()) {
                    existing.setMonitorItems(updatedConfig.getMonitorItems());
                }
                existing.setUpdateTime(LocalDateTime.now());
                saveConfigs();
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    public boolean deleteServer(String id) {
        for (int i = 0; i < serverConfigs.size(); i++) {
            if (serverConfigs.get(i).getId().equals(id)) {
                serverConfigs.remove(i);
                saveConfigs();
                return true;
            }
        }
        return false;
    }
}
