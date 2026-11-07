package com.arms.ui;

import com.arms.dao.PaymentDao;
import com.arms.dao.TenantDao;
import com.arms.dao.UnitDao;
import com.arms.dao.UserDao;
import com.arms.model.Payment;
import com.arms.model.Tenant;
import com.arms.model.Unit;
import com.arms.model.User;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class PropertyManagerMenu {

    private static final Set<String> PAYMENT_STATUSES = Set.of("PAID", "PENDING", "VOIDED");

    private final User manager;
    private final Scanner scanner;
    private final UserDao userDao = new UserDao();
    private final UnitDao unitDao = new UnitDao();
    private final TenantDao tenantDao = new TenantDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public PropertyManagerMenu(User manager, Scanner scanner) {
        this.manager = manager;
        this.scanner = scanner;
    }

    public void show() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== PROPERTY MANAGER MENU ===");
            System.out.println("1. Add apartment unit");
            System.out.println("2. View units");
            System.out.println("3. Create tenant profile and assign unit");
            System.out.println("4. Record rental payment");
            System.out.println("5. View payments");
            System.out.println("6. Update payment status");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> addUnit();
                case "2" -> printUnits(unitDao.getAllUnits());
                case "3" -> createTenant();
                case "4" -> recordPayment();
                case "5" -> printPayments(paymentDao.getAllPayments());
                case "6" -> updatePaymentStatus();
                case "7" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }

    // ------------------------------------------------------------------ Units

    private void addUnit() {
        System.out.println("\n--- Add Apartment Unit ---");
        System.out.print("Unit number (0 to cancel): ");
        String number = scanner.nextLine().trim();
        if (number.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }
        if (number.isEmpty()) {
            System.out.println("Unit number cannot be empty.");
            return;
        }
        System.out.print("Unit type (e.g. Studio, 1BR): ");
        String type = scanner.nextLine().trim();
        if (type.isEmpty()) {
            System.out.println("Unit type cannot be empty.");
            return;
        }
        Double rent = readAmount("Monthly rent (0 to cancel): ");
        if (rent == null) {
            System.out.println("Cancelled.");
            return;
        }
        if (unitDao.addUnit(manager.userId(), number, type, rent)) {
            System.out.println("Unit " + number + " added.");
        } else {
            System.out.println("Could not add unit (the unit number may already exist).");
        }
    }

    // ---------------------------------------------------------------- Tenants

    private void createTenant() {
        System.out.println("\n--- Create Tenant Profile ---");

        List<Integer> taken = tenantDao.getAllTenants().stream().map(Tenant::userId).toList();
        List<User> candidates = userDao.getAllUsers().stream()
                .filter(u -> "TENANT".equals(u.role()) && "APPROVED".equals(u.status()))
                .filter(u -> !taken.contains(u.userId()))
                .toList();
        if (candidates.isEmpty()) {
            System.out.println("No approved tenant accounts without a profile.");
            return;
        }
        System.out.printf("%-6s %-20s%n", "ID", "Username");
        for (User u : candidates) {
            System.out.printf("%-6d %-20s%n", u.userId(), u.username());
        }

        int userId = readIntInput("Enter tenant User ID (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (candidates.stream().noneMatch(u -> u.userId() == userId)) {
            System.out.println("That User ID is not in the list.");
            return;
        }

        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        System.out.print("Contact (optional): ");
        String contact = scanner.nextLine().trim();

        List<Unit> available = unitDao.getAllUnits().stream()
                .filter(u -> "AVAILABLE".equals(u.status()))
                .toList();
        if (available.isEmpty()) {
            System.out.println("No available units. Add a unit first.");
            return;
        }
        printUnits(available);
        int unitId = readIntInput("Enter Unit ID to assign (0 to cancel): ");
        if (unitId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (available.stream().noneMatch(u -> u.unitId() == unitId)) {
            System.out.println("That Unit ID is not available.");
            return;
        }

        if (tenantDao.createTenant(manager.userId(), userId, name, contact, unitId)) {
            System.out.println("Tenant profile created and unit assigned.");
        } else {
            System.out.println("Could not create tenant profile.");
        }
    }

    // --------------------------------------------------------------- Payments

    private void recordPayment() {
        System.out.println("\n--- Record Rental Payment ---");
        List<Tenant> tenants = tenantDao.getAllTenants();
        if (tenants.isEmpty()) {
            System.out.println("No tenants with an assigned unit yet.");
            return;
        }
        System.out.printf("%-10s %-20s %-8s%n", "Tenant ID", "Name", "Unit ID");
        for (Tenant t : tenants) {
            System.out.printf("%-10d %-20s %-8d%n", t.tenantId(), t.name(), t.unitId());
        }

        int tenantId = readIntInput("Enter Tenant ID (0 to cancel): ");
        if (tenantId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Tenant tenant = tenants.stream().filter(t -> t.tenantId() == tenantId).findFirst().orElse(null);
        if (tenant == null) {
            System.out.println("Tenant ID not found.");
            return;
        }
        Double amount = readAmount("Amount paid (0 to cancel): ");
        if (amount == null) {
            System.out.println("Cancelled.");
            return;
        }
        if (paymentDao.recordPayment(manager.userId(), tenant.tenantId(), tenant.unitId(), amount)) {
            System.out.printf("Payment of P%.2f recorded for %s.%n", amount, tenant.name());
        } else {
            System.out.println("Could not record payment.");
        }
    }

    private void updatePaymentStatus() {
        System.out.println("\n--- Update Payment Status ---");
        List<Payment> payments = paymentDao.getAllPayments();
        if (payments.isEmpty()) {
            System.out.println("No payments recorded yet.");
            return;
        }
        printPayments(payments);

        int paymentId = readIntInput("Enter Payment ID (0 to cancel): ");
        if (paymentId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Payment payment = payments.stream().filter(p -> p.paymentId() == paymentId).findFirst().orElse(null);
        if (payment == null) {
            System.out.println("Payment ID not found.");
            return;
        }
        String status = readValidPaymentStatus();
        if (status == null) {
            System.out.println("Cancelled.");
            return;
        }
        if (status.equals(payment.paymentStatus())) {
            System.out.println("Payment #" + paymentId + " is already " + status + ".");
            return;
        }
        if (paymentDao.updatePaymentStatus(manager.userId(), paymentId, status)) {
            System.out.println("Payment #" + paymentId + " status changed from "
                    + payment.paymentStatus() + " to " + status + ".");
        } else {
            System.out.println("Could not update payment.");
        }
    }

    // ----------------------------------------------------------------- Output

    private void printUnits(List<Unit> units) {
        if (units.isEmpty()) {
            System.out.println("No units found.");
            return;
        }
        System.out.printf("%-6s %-12s %-12s %-12s %-10s%n", "ID", "Unit No.", "Type", "Rent", "Status");
        for (Unit u : units) {
            System.out.printf("%-6d %-12s %-12s %-12.2f %-10s%n",
                    u.unitId(), u.unitNumber(), u.unitType(), u.monthlyRent(), u.status());
        }
    }

    private void printPayments(List<Payment> payments) {
        if (payments.isEmpty()) {
            System.out.println("No payments recorded yet.");
            return;
        }
        System.out.printf("%-5s %-8s %-6s %-10s %-9s %-20s %-20s%n",
                "ID", "Tenant", "Unit", "Amount", "Status", "Receipt", "Date");
        for (Payment p : payments) {
            System.out.printf("%-5d %-8d %-6d %-10.2f %-9s %-20s %-20s%n",
                    p.paymentId(), p.tenantId(), p.unitId(), p.amountPaid(),
                    p.paymentStatus(), p.receiptNumber(), p.paymentDate());
        }
    }

    // ------------------------------------------------------ Input validation

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns a positive amount, or null if the user enters 0 to cancel. */
    private Double readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value == 0) {
                    return null;
                }
                if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("Amount must be a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private String readValidPaymentStatus() {
        while (true) {
            System.out.print("Enter new status (PAID, PENDING, VOIDED) or 0 to cancel: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (PAYMENT_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Valid statuses: PAID, PENDING, VOIDED.");
        }
    }
}