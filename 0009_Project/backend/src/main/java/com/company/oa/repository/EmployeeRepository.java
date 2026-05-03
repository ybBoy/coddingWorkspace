package com.company.oa.repository;

import com.company.oa.model.Employee;
import com.company.oa.storage.JsonFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeRepository {

    private static final String FILE_NAME = "employees.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    public List<Employee> findAll() {
        return jsonFileStorage.load(FILE_NAME, Employee.class);
    }

    public Optional<Employee> findById(Long id) {
        return findAll().stream()
                .filter(emp -> emp.getId().equals(id))
                .findFirst();
    }

    public Optional<Employee> findByUsername(String username) {
        return findAll().stream()
                .filter(emp -> emp.getUsername().equals(username))
                .findFirst();
    }

    public List<Employee> findByDepartmentId(Long departmentId) {
        return findAll().stream()
                .filter(emp -> emp.getDepartmentId() != null && emp.getDepartmentId().equals(departmentId))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Employee> findByRoleId(Long roleId) {
        return findAll().stream()
                .filter(emp -> emp.getRoleId() != null && emp.getRoleId().equals(roleId))
                .collect(java.util.stream.Collectors.toList());
    }

    public Employee save(Employee employee) {
        List<Employee> employees = findAll();
        
        if (employee.getId() == null) {
            long maxId = employees.stream()
                    .mapToLong(Employee::getId)
                    .max()
                    .orElse(0L);
            employee.setId(maxId + 1);
            employee.setCreateTime(LocalDateTime.now());
            employee.setUpdateTime(LocalDateTime.now());
            employees.add(employee);
        } else {
            for (int i = 0; i < employees.size(); i++) {
                if (employees.get(i).getId().equals(employee.getId())) {
                    employee.setUpdateTime(LocalDateTime.now());
                    employees.set(i, employee);
                    break;
                }
            }
        }
        
        jsonFileStorage.save(FILE_NAME, employees, Employee.class);
        return employee;
    }

    public void deleteById(Long id) {
        List<Employee> employees = findAll();
        employees.removeIf(emp -> emp.getId().equals(id));
        jsonFileStorage.save(FILE_NAME, employees, Employee.class);
    }
}