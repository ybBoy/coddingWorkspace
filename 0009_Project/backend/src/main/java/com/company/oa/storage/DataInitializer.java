package com.company.oa.storage;

import com.company.oa.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JsonFileStorage jsonFileStorage;

    private static final String DEPARTMENT_FILE = "departments.json";
    private static final String ROLE_FILE = "roles.json";
    private static final String EMPLOYEE_FILE = "employees.json";
    private static final String LEAVE_FILE = "leaves.json";

    @Override
    public void run(String... args) throws Exception {
        initDepartments();
        initRoles();
        initEmployees();
    }

    private void initDepartments() {
        List<Department> departments = jsonFileStorage.load(DEPARTMENT_FILE, Department.class);
        if (departments.isEmpty()) {
            departments = new ArrayList<>();
            
            Department hr = createDepartment(1L, "人力资源部", "负责公司人力资源管理");
            Department tech = createDepartment(2L, "技术研发部", "负责公司技术研发工作");
            Department commerce = createDepartment(3L, "商务部", "负责公司商务合作");
            Department finance = createDepartment(4L, "财务部", "负责公司财务管理");
            Department pm = createDepartment(5L, "项目管理部", "负责公司项目管理");
            
            departments.add(hr);
            departments.add(tech);
            departments.add(commerce);
            departments.add(finance);
            departments.add(pm);
            
            jsonFileStorage.save(DEPARTMENT_FILE, departments, Department.class);
        }
    }

    private void initRoles() {
        List<Role> roles = jsonFileStorage.load(ROLE_FILE, Role.class);
        if (roles.isEmpty()) {
            roles = new ArrayList<>();
            
            Role admin = createRole(1L, "管理员", "系统管理员，拥有全部权限");
            Role employee = createRole(2L, "普通员工", "普通员工角色");
            
            roles.add(admin);
            roles.add(employee);
            
            jsonFileStorage.save(ROLE_FILE, roles, Role.class);
        }
    }

    private void initEmployees() {
        List<Employee> employees = jsonFileStorage.load(EMPLOYEE_FILE, Employee.class);
        if (employees.isEmpty()) {
            employees = new ArrayList<>();
            
            Employee admin = createEmployee(1L, "admin", "123456", "系统管理员", 
                    1L, "人力资源部", 1L, "管理员");
            
            Employee emp1 = createEmployee(2L, "zhangsan", "123456", "张三", 
                    2L, "技术研发部", 2L, "普通员工");
            
            Employee emp2 = createEmployee(3L, "lisi", "123456", "李四", 
                    3L, "商务部", 2L, "普通员工");
            
            employees.add(admin);
            employees.add(emp1);
            employees.add(emp2);
            
            jsonFileStorage.save(EMPLOYEE_FILE, employees, Employee.class);
        }
    }

    private Department createDepartment(Long id, String name, String description) {
        Department dept = new Department();
        dept.setId(id);
        dept.setName(name);
        dept.setDescription(description);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        return dept;
    }

    private Role createRole(Long id, String name, String description) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return role;
    }

    private Employee createEmployee(Long id, String username, String password, String name,
                                     Long departmentId, String departmentName,
                                     Long roleId, String roleName) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setUsername(username);
        emp.setPassword(password);
        emp.setName(name);
        emp.setDepartmentId(departmentId);
        emp.setDepartmentName(departmentName);
        emp.setRoleId(roleId);
        emp.setRoleName(roleName);
        emp.setStatus(EmployeeStatus.NORMAL);
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        return emp;
    }
}