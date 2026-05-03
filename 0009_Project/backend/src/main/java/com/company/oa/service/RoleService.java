package com.company.oa.service;

import com.company.oa.model.Role;
import com.company.oa.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }

    public boolean deleteById(Long id) {
        Optional<Role> role = roleRepository.findById(id);
        if (role.isPresent()) {
            roleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}