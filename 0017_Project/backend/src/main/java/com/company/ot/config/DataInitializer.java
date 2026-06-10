package com.company.ot.config;

import com.company.ot.model.Department;
import com.company.ot.model.Role;
import com.company.ot.model.User;
import com.company.ot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws IOException {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            initUsers();
        }
    }

    private void initUsers() throws IOException {
        userService.saveUser(new User(1L, "张伟", Department.TECH, Role.MANAGER));
        userService.saveUser(new User(2L, "李娜", Department.TECH, Role.EMPLOYEE));
        userService.saveUser(new User(3L, "王强", Department.TECH, Role.EMPLOYEE));

        userService.saveUser(new User(4L, "赵敏", Department.PRODUCT, Role.MANAGER));
        userService.saveUser(new User(5L, "刘洋", Department.PRODUCT, Role.EMPLOYEE));
        userService.saveUser(new User(6L, "陈静", Department.PRODUCT, Role.EMPLOYEE));

        userService.saveUser(new User(7L, "周磊", Department.OPERATIONS, Role.MANAGER));
        userService.saveUser(new User(8L, "吴芳", Department.OPERATIONS, Role.EMPLOYEE));
        userService.saveUser(new User(9L, "郑涛", Department.OPERATIONS, Role.EMPLOYEE));

        userService.saveUser(new User(10L, "孙丽", Department.HR, Role.MANAGER));
        userService.saveUser(new User(11L, "马超", Department.HR, Role.EMPLOYEE));
        userService.saveUser(new User(12L, "朱琳", Department.HR, Role.EMPLOYEE));
    }
}
