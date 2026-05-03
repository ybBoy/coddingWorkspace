package com.company.oa.service;

import com.company.oa.dto.LoginDTO;
import com.company.oa.model.Employee;
import com.company.oa.model.EmployeeStatus;
import com.company.oa.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> findByUsername(String username) {
        return employeeRepository.findByUsername(username);
    }

    public List<Employee> findByDepartmentId(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public List<Employee> findByRoleId(Long roleId) {
        return employeeRepository.findByRoleId(roleId);
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public boolean deleteById(Long id) {
        Optional<Employee> emp = employeeRepository.findById(id);
        if (emp.isPresent()) {
            employeeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<Employee> login(LoginDTO loginDTO) {
        Optional<Employee> employee = employeeRepository.findByUsername(loginDTO.getUsername());
        if (employee.isPresent() && employee.get().getPassword().equals(loginDTO.getPassword())) {
            return employee;
        }
        return Optional.empty();
    }

    public boolean isAdmin(Long employeeId) {
        Optional<Employee> employee = findById(employeeId);
        return employee.isPresent() && employee.get().getRoleId() != null && employee.get().getRoleId() == 1L;
    }

    public void updateStatus(Long employeeId, EmployeeStatus status) {
        Optional<Employee> employee = findById(employeeId);
        if (employee.isPresent()) {
            Employee emp = employee.get();
            emp.setStatus(status);
            save(emp);
        }
    }
}