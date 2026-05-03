package com.company.oa.repository;

import com.company.oa.model.Role;
import com.company.oa.storage.JsonFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepository {

    private static final String FILE_NAME = "roles.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    public List<Role> findAll() {
        return jsonFileStorage.load(FILE_NAME, Role.class);
    }

    public Optional<Role> findById(Long id) {
        return findAll().stream()
                .filter(role -> role.getId().equals(id))
                .findFirst();
    }

    public Role save(Role role) {
        List<Role> roles = findAll();
        
        if (role.getId() == null) {
            long maxId = roles.stream()
                    .mapToLong(Role::getId)
                    .max()
                    .orElse(0L);
            role.setId(maxId + 1);
            role.setCreateTime(LocalDateTime.now());
            role.setUpdateTime(LocalDateTime.now());
            roles.add(role);
        } else {
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getId().equals(role.getId())) {
                    role.setUpdateTime(LocalDateTime.now());
                    roles.set(i, role);
                    break;
                }
            }
        }
        
        jsonFileStorage.save(FILE_NAME, roles, Role.class);
        return role;
    }

    public void deleteById(Long id) {
        List<Role> roles = findAll();
        roles.removeIf(role -> role.getId().equals(id));
        jsonFileStorage.save(FILE_NAME, roles, Role.class);
    }
}