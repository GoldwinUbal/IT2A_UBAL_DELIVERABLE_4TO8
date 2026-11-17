package com.arms.ui;

import com.arms.dao.PaymentDao;
import com.arms.dao.TenantDao;
import com.arms.dao.UnitDao;
import com.arms.model.Payment;
import com.arms.model.Tenant;
import com.arms.model.Unit;
import com.arms.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class TenantMenu {

    private static final String NO_PROFILE =
            "No tenant profile found for your account yet. Please ask a property manager to set one up.";

    private final User tenant;
    private final Scanner scanner;
    private final TenantDao tenantDao = new TenantDao();
    private final UnitDao unitDao = new UnitDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public TenantMenu(User tenant, Scanner scanner) {
        this.tenant = tenant;
        this.scanner = scanner;
    }

    public void show() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== TENANT MENU ===");
            System.out.println("1. View personal information");
            System.out.println("2. View assigned unit");
            System.out.println("3. View payment history");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewPersonalInfo();
                case "2" -> viewAssignedUnit();
                case "3" -> viewPaymentHistory();
                case "4" -> active = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Logged out.");
    }

    // A tenant can only ever see their own record: everything is looked up
    // from the logged-in user's ID, never from an ID the tenant types in.

    private void viewPersonalInfo() {
        System.out.println("\n--- Personal Information ---");
        System.out.println("Username : " + tenant.username());
        System.out.println("Role     : " + tenant.role());
        System.out.println("Status   : " + tenant.status());

        Optional<Tenant> profile = tenantDao.findByUserId(tenant.userId());
        if (profile.isEmpty()) {
            System.out.println(NO_PROFILE);
            return;
        }
        Tenant t = profile.get();
        String contact = (t.contact() == null || t.contact().isBlank()) ? "N/A" : t.contact();
        System.out.println("Name     : " + t.name());
        System.out.println("Contact  : " + contact);
        System.out.println("Tenancy  : " + t.status());
    }

    private void viewAssignedUnit() {
        System.out.println("\n--- Assigned Unit ---");
        Optional<Tenant> profile = tenantDao.findByUserId(tenant.userId());
        if (profile.isEmpty()) {
            System.out.println(NO_PROFILE);
            return;
        }
        if ("INACTIVE".equals(profile.get().status())) {
            System.out.println("Your tenancy has ended, so no unit is currently assigned.");
            return;
        }
        Optional<Unit> unit = unitDao.findById(profile.get().unitId());
        if (unit.isEmpty()) {
            System.out.println("You have no unit assigned yet.");
            return;
        }
        Unit u = unit.get();
        System.out.println("Unit number  : " + u.unitNumber());
        System.out.println("Unit type    : " + u.unitType());
        System.out.printf("Monthly rent : P%.2f%n", u.monthlyRent());
        System.out.println("Unit status  : " + u.status());
    }

    private void viewPaymentHistory() {
        System.out.println("\n--- Payment History ---");
        Optional<Tenant> profile = tenantDao.findByUserId(tenant.userId());
        if (profile.isEmpty()) {
            System.out.println(NO_PROFILE);
            return;
        }
        List<Payment> payments = paymentDao.getPaymentsByTenantId(profile.get().tenantId());
        if (payments.isEmpty()) {
            System.out.println("No payments recorded yet.");
            return;
        }
        System.out.printf("%-5s %-10s %-9s %-20s %-20s%n", "ID", "Amount", "Status", "Receipt", "Date");
        for (Payment p : payments) {
            System.out.printf("%-5d %-10.2f %-9s %-20s %-20s%n",
                    p.paymentId(), p.amountPaid(), p.paymentStatus(), p.receiptNumber(), p.paymentDate());
        }
        double totalPaid = payments.stream()
                .filter(p -> "PAID".equals(p.paymentStatus()))
                .mapToDouble(Payment::amountPaid)
                .sum();
        System.out.printf("Total paid (PAID payments only): P%.2f%n", totalPaid);
    }
}