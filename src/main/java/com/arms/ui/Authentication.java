package com.arms.ui;

import com.arms.dao.UserDao;
import com.arms.model.User;
import com.arms.util.PasswordUtil;

import java.util.Optional;
import java.util.Scanner;

public class Authentication {

    private static final String ADMIN = "ADMINISTRATOR";
    private static final String MANAGER = "PROPERTY_MANAGER";
    private static final String TENANT = "TENANT";

    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ARMS: Apartment Rental Management System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    // ---------------------------------------------------------------- Register

    private void register() {
        System.out.println("\n--- Register ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        String role = selectRole();
        if (role == null) {
            System.out.println("Invalid role. Registration cancelled.");
            return;
        }

        User newUser = new User(0, username, PasswordUtil.hash(password), role, "PENDING");
        if (userDao.register(newUser)) {
            System.out.println("Registration successful! Status: PENDING (awaiting administrator approval).");
        } else {
            System.out.println("Registration failed.");
        }
    }

    private String selectRole() {
        System.out.println("Select role:");
        System.out.println("1. Administrator");
        System.out.println("2. Property Manager");
        System.out.println("3. Tenant");
        System.out.print("Choose: ");
        return switch (scanner.nextLine().trim()) {
            case "1" -> ADMIN;
            case "2" -> MANAGER;
            case "3" -> TENANT;
            default -> null;
        };
    }

    // ------------------------------------------------------------------- Login

    private void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> found = userDao.findByUsername(username);
        if (found.isEmpty() || !found.get().password().equals(PasswordUtil.hash(password))) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = found.get();
        if (!"APPROVED".equals(user.status())) {
            System.out.println("Your account is " + user.status() + ". Please wait for administrator approval.");
            return;
        }

        System.out.println("Welcome, " + user.username() + "! (" + user.role() + ")");
        routeByRole(user);
    }

    private void routeByRole(User user) {
        switch (user.role()) {
            case ADMIN -> new AdminMenu(user, userDao, scanner).show();
            case MANAGER -> new PropertyManagerMenu(user, scanner).show();
            case TENANT -> new TenantMenu(user, scanner).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }
}
