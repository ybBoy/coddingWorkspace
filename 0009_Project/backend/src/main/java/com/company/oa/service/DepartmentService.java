package com.company.oa.service;

import com.company.oa.model.Department;
import com.company.oa.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    public Department save(Department department) {
        return departmentRepository.save(department);
    }

    public boolean deleteById(Long id) {
        Optional<Department> dept = departmentRepository.findById(id);
        if (dept.isPresent()) {
            departmentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}