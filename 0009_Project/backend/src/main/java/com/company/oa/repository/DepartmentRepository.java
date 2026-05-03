package com.company.oa.repository;

import com.company.oa.model.Department;
import com.company.oa.storage.JsonFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DepartmentRepository {

    private static final String FILE_NAME = "departments.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    private AtomicLong idGenerator;

    public DepartmentRepository() {
        this.idGenerator = new AtomicLong(100);
    }

    public List<Department> findAll() {
        return jsonFileStorage.load(FILE_NAME, Department.class);
    }

    public Optional<Department> findById(Long id) {
        return findAll().stream()
                .filter(dept -> dept.getId().equals(id))
                .findFirst();
    }

    public Department save(Department department) {
        List<Department> departments = findAll();
        
        if (department.getId() == null) {
            long maxId = departments.stream()
                    .mapToLong(Department::getId)
                    .max()
                    .orElse(0L);
            department.setId(maxId + 1);
            department.setCreateTime(LocalDateTime.now());
            department.setUpdateTime(LocalDateTime.now());
            departments.add(department);
        } else {
            for (int i = 0; i < departments.size(); i++) {
                if (departments.get(i).getId().equals(department.getId())) {
                    department.setUpdateTime(LocalDateTime.now());
                    departments.set(i, department);
                    break;
                }
            }
        }
        
        jsonFileStorage.save(FILE_NAME, departments, Department.class);
        return department;
    }

    public void deleteById(Long id) {
        List<Department> departments = findAll();
        departments.removeIf(dept -> dept.getId().equals(id));
        jsonFileStorage.save(FILE_NAME, departments, Department.class);
    }
}