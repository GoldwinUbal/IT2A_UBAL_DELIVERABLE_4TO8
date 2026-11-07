package com.arms.ui;

import com.arms.model.User;

import java.util.Scanner;

public class PropertyManagerMenu {

    private final User manager;
    private final Scanner scanner;

    public PropertyManagerMenu(User manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    public void show() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== PROPERTY MANAGER MENU ===");
            System.out.println("1. Manage tenant records (coming soon)");
            System.out.println("2. Manage unit assignments (coming soon)");
            System.out.println("3. Record/update rental payments (coming soon)");
            System.out.println("4. View rental reports (coming soon)");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1", "2", "3", "4" -> System.out.println("Coming soon.");
                case "5" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }
}
