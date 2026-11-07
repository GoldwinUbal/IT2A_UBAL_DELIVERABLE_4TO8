package com.arms.ui;

import com.arms.dao.ActivityLogDao;
import com.arms.dao.UserDao;
import com.arms.model.ActivityLog;
import com.arms.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Scanner;

public class AdminMenu {

    private static final String HEADER_FORMAT = "| %-5s | %-15s | %-18s | %-10s |%n";
    private static final String BORDER = "+-------+-----------------+--------------------+------------+";

    private static final Set<String> VALID_ROLES = Set.of("ADMINISTRATOR", "PROPERTY_MANAGER", "TENANT");
    private static final Set<String> VALID_STATUSES = Set.of("APPROVED", "PENDING");

    private static final String LOG_FORMAT = "| %-5s | %-8s | %-18s | %-35s | %-20s |%n";
    private static final String LOG_BORDER = border(5, 8, 18, 35, 20);
    private static final int DESCRIPTION_WIDTH = 35;

    private final ActivityLogDao logDao = new ActivityLogDao();
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
            System.out.println("4. Update user role and status");
            System.out.println("5. Manage apartment/unit records (coming soon)");
            System.out.println("6. View reports (coming soon)");
            System.out.println("7. View system audit logs");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> updateUser();
                case "5", "6" -> System.out.println("Coming soon.");
                case "7" -> viewAuditLogs();
                case "8" -> active = false;
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

            if (userDao.approveUser(admin.userId(), id)) {
                System.out.println("User '" + target.username() + "' (ID " + id
                        + ", " + target.role() + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ------------------------------------------------------- Update user

    private void updateUser() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n--- Update User Role and Status ---");
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUserTable(users);

        while (true) {
            int id = readIntInput("Enter User ID to update (0 to cancel): ");
            if (id == 0) {
                cancelled();
                return;
            }

            // Self-edit restriction
            if (id == admin.userId()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }

            User target = users.stream().filter(u -> u.userId() == id).findFirst().orElse(null);
            if (target == null) {
                System.out.println("User ID " + id + " does not exist. Try again.");
                continue;
            }

            String newRole = readValidRole();
            if (newRole == null) {
                cancelled();
                return;
            }

            String newStatus = readValidStatus();
            if (newStatus == null) {
                cancelled();
                return;
            }

            if (userDao.updateUserRoleAndStatus(admin.userId(), id, newRole, newStatus)) {
                System.out.println("User '" + target.username() + "' (ID " + id
                        + ") updated successfully: role = " + newRole + ", status = " + newStatus + ".");
            } else {
                System.out.println("Update failed. Please try again.");
            }
            return;
        }
    }

    private void cancelled() {
        System.out.println("Update cancelled.");
    }

    // ------------------------------------------------------- Audit logs

    private void viewAuditLogs() {
        boolean active = true;
        while (active) {
            System.out.println("\n--- System Audit Logs ---");
            System.out.println("1. View all system logs");
            System.out.println("2. Filter logs by User ID");
            System.out.println("3. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printLogTable(logDao.getAllLogs());
                case "2" -> filterLogsByUser();
                case "3" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void filterLogsByUser() {
        int id = readIntInput("Enter User ID to filter (0 to cancel): ");
        if (id == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        List<ActivityLog> logs = logDao.getLogsByUserId(id);
        if (logs.isEmpty()) {
            System.out.println("No logs found for User ID " + id + ".");
            return;
        }
        printLogTable(logs);
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No logs recorded yet.");
            return;
        }
        System.out.println(LOG_BORDER);
        System.out.printf(LOG_FORMAT, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(LOG_BORDER);
        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.description(), DESCRIPTION_WIDTH);
            System.out.printf(LOG_FORMAT, log.id(), log.userId(), log.actionType(), lines.get(0), log.timestamp());
            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(LOG_FORMAT, "", "", "", lines.get(i), "");
            }
            System.out.println(LOG_BORDER);
        }
    }

    /** Splits long text into lines so the description column stays aligned. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : (text == null ? "" : text).split(" ")) {
            while (word.length() > width) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0 || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static String border(int... widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    // --------------------------------------------------- Input validation

    /** Keeps asking until the input is a whole number. Never throws. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns a valid role (upper-case), or null if the admin enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (ADMINISTRATOR, PROPERTY_MANAGER, TENANT) or 0 to cancel: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Valid roles: ADMINISTRATOR, PROPERTY_MANAGER, TENANT.");
        }
    }

    /** Returns a valid status (upper-case), or null if the admin enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED, PENDING) or 0 to cancel: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Valid statuses: APPROVED, PENDING.");
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