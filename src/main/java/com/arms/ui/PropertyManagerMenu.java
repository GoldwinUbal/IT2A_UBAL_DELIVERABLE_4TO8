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
            System.out.println("1. Manage units");
            System.out.println("2. Manage tenants");
            System.out.println("3. Manage payments");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> unitsMenu();
                case "2" -> tenantsMenu();
                case "3" -> paymentsMenu();
                case "4" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }

    // ================================================================ Submenus

    private void unitsMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Manage Units ---");
            System.out.println("1. Add apartment unit");
            System.out.println("2. View units");
            System.out.println("3. Edit unit information");
            System.out.println("4. Deactivate unit");
            System.out.println("5. Reactivate unit");
            System.out.println("6. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> addUnit();
                case "2" -> printUnits(unitDao.getAllUnits());
                case "3" -> editUnit();
                case "4" -> deactivateUnit();
                case "5" -> reactivateUnit();
                case "6" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void tenantsMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Manage Tenants ---");
            System.out.println("1. Create tenant profile and assign unit");
            System.out.println("2. View tenants");
            System.out.println("3. Edit tenant information");
            System.out.println("4. End tenancy (deactivate tenant)");
            System.out.println("5. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createTenant();
                case "2" -> printTenants(tenantDao.getAllTenantProfiles());
                case "3" -> editTenant();
                case "4" -> endTenancy();
                case "5" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void paymentsMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Manage Payments ---");
            System.out.println("1. Record rental payment");
            System.out.println("2. View payments");
            System.out.println("3. Update payment status");
            System.out.println("4. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> recordPayment();
                case "2" -> printPayments(paymentDao.getAllPayments());
                case "3" -> updatePaymentStatus();
                case "4" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
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

    private void editUnit() {
        System.out.println("\n--- Edit Unit Information ---");
        List<Unit> units = unitDao.getAllUnits();
        if (units.isEmpty()) {
            System.out.println("No units found.");
            return;
        }
        printUnits(units);
        int id = readIntInput("Enter Unit ID to edit (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Unit unit = units.stream().filter(u -> u.unitId() == id).findFirst().orElse(null);
        if (unit == null) {
            System.out.println("Unit ID not found.");
            return;
        }

        System.out.println("Press Enter to keep the current value.");
        String number = readKeep("Unit number [" + unit.unitNumber() + "]: ", unit.unitNumber());
        String type = readKeep("Unit type [" + unit.unitType() + "]: ", unit.unitType());
        Double rent = readOptionalAmount(String.format("Monthly rent [%.2f]: ", unit.monthlyRent()));
        double newRent = (rent == null) ? unit.monthlyRent() : rent;

        if (number.equals(unit.unitNumber()) && type.equals(unit.unitType()) && newRent == unit.monthlyRent()) {
            System.out.println("No changes made.");
            return;
        }
        if (unitDao.updateUnit(manager.userId(), id, number, type, newRent)) {
            System.out.println("Unit updated.");
        } else {
            System.out.println("Could not update unit (the unit number may already exist).");
        }
    }

    private void deactivateUnit() {
        System.out.println("\n--- Deactivate Unit ---");
        List<Unit> units = unitDao.getAllUnits();
        if (units.isEmpty()) {
            System.out.println("No units found.");
            return;
        }
        printUnits(units);
        int id = readIntInput("Enter Unit ID to deactivate (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Unit unit = units.stream().filter(u -> u.unitId() == id).findFirst().orElse(null);
        if (unit == null) {
            System.out.println("Unit ID not found.");
            return;
        }
        if ("OCCUPIED".equals(unit.status())) {
            System.out.println("An occupied unit cannot be deactivated. End the tenancy first.");
            return;
        }
        if ("INACTIVE".equals(unit.status())) {
            System.out.println("That unit is already inactive.");
            return;
        }
        if (!confirm("Deactivate unit " + unit.unitNumber() + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        if (unitDao.deactivateUnit(manager.userId(), id)) {
            System.out.println("Unit " + unit.unitNumber() + " deactivated. The record is kept.");
        } else {
            System.out.println("Could not deactivate unit.");
        }
    }

    private void reactivateUnit() {
        System.out.println("\n--- Reactivate Unit ---");
        List<Unit> inactive = unitDao.getAllUnits().stream()
                .filter(u -> "INACTIVE".equals(u.status()))
                .toList();
        if (inactive.isEmpty()) {
            System.out.println("There are no inactive units.");
            return;
        }
        printUnits(inactive);
        int id = readIntInput("Enter Unit ID to reactivate (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (inactive.stream().noneMatch(u -> u.unitId() == id)) {
            System.out.println("That Unit ID is not in the inactive list.");
            return;
        }
        if (unitDao.reactivateUnit(manager.userId(), id)) {
            System.out.println("Unit reactivated and is now AVAILABLE.");
        } else {
            System.out.println("Could not reactivate unit.");
        }
    }

    // ---------------------------------------------------------------- Tenants

    private void createTenant() {
        System.out.println("\n--- Create Tenant Profile ---");

        List<Integer> taken = tenantDao.getAllTenantProfiles().stream().map(Tenant::userId).toList();
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

    private void editTenant() {
        System.out.println("\n--- Edit Tenant Information ---");
        List<Tenant> tenants = tenantDao.getAllTenants();
        if (tenants.isEmpty()) {
            System.out.println("There are no active tenants.");
            return;
        }
        printTenants(tenants);
        int id = readIntInput("Enter Tenant ID to edit (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Tenant tenant = tenants.stream().filter(t -> t.tenantId() == id).findFirst().orElse(null);
        if (tenant == null) {
            System.out.println("That Tenant ID is not in the active list.");
            return;
        }

        String currentContact = tenant.contact() == null ? "" : tenant.contact();
        System.out.println("Press Enter to keep the current value.");
        String name = readKeep("Name [" + tenant.name() + "]: ", tenant.name());
        String contact = readKeep("Contact [" + (currentContact.isEmpty() ? "none" : currentContact) + "]: ",
                currentContact);

        if (name.equals(tenant.name()) && contact.equals(currentContact)) {
            System.out.println("No changes made.");
            return;
        }
        if (tenantDao.updateTenant(manager.userId(), id, name, contact)) {
            System.out.println("Tenant information updated.");
        } else {
            System.out.println("Could not update tenant.");
        }
    }

    private void endTenancy() {
        System.out.println("\n--- End Tenancy ---");
        List<Tenant> tenants = tenantDao.getAllTenants();
        if (tenants.isEmpty()) {
            System.out.println("There are no active tenants.");
            return;
        }
        printTenants(tenants);
        int id = readIntInput("Enter Tenant ID to deactivate (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        Tenant tenant = tenants.stream().filter(t -> t.tenantId() == id).findFirst().orElse(null);
        if (tenant == null) {
            System.out.println("That Tenant ID is not in the active list.");
            return;
        }
        if (!confirm("End the tenancy of " + tenant.name() + "? Their unit will become available.")) {
            System.out.println("Cancelled.");
            return;
        }
        if (tenantDao.deactivateTenant(manager.userId(), id)) {
            System.out.println("Tenancy ended. The record and payment history are kept, and the unit is AVAILABLE.");
        } else {
            System.out.println("Could not end tenancy.");
        }
    }

    // --------------------------------------------------------------- Payments

    private void recordPayment() {
        System.out.println("\n--- Record Rental Payment ---");
        List<Tenant> tenants = tenantDao.getAllTenants();
        if (tenants.isEmpty()) {
            System.out.println("No active tenants with an assigned unit yet.");
            return;
        }
        printTenants(tenants);

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

    private void printTenants(List<Tenant> tenants) {
        if (tenants.isEmpty()) {
            System.out.println("No tenants found.");
            return;
        }
        System.out.printf("%-10s %-20s %-16s %-8s %-9s%n", "Tenant ID", "Name", "Contact", "Unit ID", "Status");
        for (Tenant t : tenants) {
            String contact = (t.contact() == null || t.contact().isBlank()) ? "N/A" : t.contact();
            System.out.printf("%-10d %-20s %-16s %-8d %-9s%n",
                    t.tenantId(), t.name(), contact, t.unitId(), t.status());
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

    /** Returns a positive amount, or null if the user just presses Enter (keep current). */
    private Double readOptionalAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                double value = Double.parseDouble(input);
                if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("Amount must be a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns the typed text, or the current value if the user just presses Enter. */
    private String readKeep(String prompt, String current) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? current : input;
    }

    private boolean confirm(String question) {
        System.out.print(question + " Type YES to confirm: ");
        return scanner.nextLine().trim().equalsIgnoreCase("YES");
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