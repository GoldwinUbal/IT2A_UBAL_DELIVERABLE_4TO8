package com.arms.ui;

import com.arms.dao.UserDao;
import com.arms.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final String HEADER_FORMAT = "| %-5s | %-15s | %-18s | %-10s |%n";
    private static final String BORDER = "+-------+-----------------+--------------------+------------+";

    private final User admin;
    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(User admin, UserDao userDao, Scanner scanner) {
        this.admin = admin;
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void show() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== ADMINISTRATOR MENU ===");
            System.out.println("1. View all system users");
            System.out.println("2. View pending registrations");
            System.out.println("3. Approve a user account");
            System.out.println("4. Manage apartment/unit records (coming soon)");
            System.out.println("5. View reports (coming soon)");
            System.out.println("6. Monitor activity logs (coming soon)");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4", "5", "6" -> System.out.println("Coming soon.");
                case "7" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }

    // ------------------------------------------------------------- Views

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n--- All System Users ---");
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUserTable(users);
    }

    private void viewPendingUsers() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n--- Pending Registrations ---");
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        printUserTable(pending);
    }

    // ---------------------------------------------------------- Approval

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n--- Approve User Account ---");
        if (pending.isEmpty()) {
            System.out.println("No pending registrations to approve.");
            return;
        }
        printUserTable(pending);

        while (true) {
            System.out.print("Enter User ID to approve (0 to cancel): ");
            String input = scanner.nextLine().trim();

            int id;
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numeric User ID.");
                continue;
            }

            if (id == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = pending.stream().filter(u -> u.userId() == id).findFirst().orElse(null);
            if (target == null) {
                System.out.println("User ID " + id + " is not in the pending list. Try again.");
                continue;
            }

            if (userDao.approveUser(id)) {
                System.out.println("User '" + target.username() + "' (ID " + id
                        + ", " + target.role() + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ----------------------------------------------------- Table helper

    private void printUserTable(List<User> users) {
        System.out.println(BORDER);
        System.out.printf(HEADER_FORMAT, "ID", "Username", "Role", "Status");
        System.out.println(BORDER);
        for (User u : users) {
            System.out.printf(HEADER_FORMAT, u.userId(), u.username(), u.role(), u.status());
        }
        System.out.println(BORDER);
    }
}
