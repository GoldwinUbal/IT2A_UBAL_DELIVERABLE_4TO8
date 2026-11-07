package com.arms.ui;

import com.arms.model.User;

import java.util.Scanner;

public class TenantMenu {

    private final User tenant;
    private final Scanner scanner;

    public TenantMenu(User tenant, Scanner scanner) {
        this.tenant = tenant;
        this.scanner = scanner;
    }

    public void show() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== TENANT MENU ===");
            System.out.println("1. View personal information (coming soon)");
            System.out.println("2. View assigned unit (coming soon)");
            System.out.println("3. View payment history (coming soon)");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1", "2", "3" -> System.out.println("Coming soon.");
                case "4" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }
}
