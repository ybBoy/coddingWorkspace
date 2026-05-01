package com.monitoring.controller;

import com.monitoring.model.ServerConfig;
import com.monitoring.service.ServerConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servers")
@CrossOrigin(origins = "*")
public class ServerController {

    @Autowired
    private ServerConfigService serverConfigService;

    @GetMapping
    public List<ServerConfig> getAllServers() {
        return serverConfigService.getAllServers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerConfig> getServerById(@PathVariable String id) {
        Optional<ServerConfig> server = serverConfigService.getServerById(id);
        return server.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ServerConfig createServer(@RequestBody ServerConfig serverConfig) {
        return serverConfigService.createServer(serverConfig);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerConfig> updateServer(
            @PathVariable String id,
            @RequestBody ServerConfig serverConfig) {
        Optional<ServerConfig> updated = serverConfigService.updateServer(id, serverConfig);
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable String id) {
        boolean deleted = serverConfigService.deleteServer(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
