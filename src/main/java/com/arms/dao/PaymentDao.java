package com.arms.dao;

import com.arms.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Records a PAID rental payment and logs PAYMENT_RECORDED. */
    public boolean recordPayment(int actorId, int tenantId, int unitId, double amount) {
        String receipt = "RCP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sql = "INSERT INTO payments (tenant_id, unit_id, amount_paid, payment_status, receipt_number, payment_date) "
                + "VALUES (?, ?, ?, 'PAID', ?, datetime('now','localtime'))";
        boolean inserted;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setInt(2, unitId);
            ps.setDouble(3, amount);
            ps.setString(4, receipt);
            inserted = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Could not record payment: " + e.getMessage());
            return false;
        }
        if (inserted) {
            logDao.logAction(actorId, "PAYMENT_RECORDED",
                    String.format("Manager %d recorded payment of P%.2f for Tenant ID %d (Unit ID %d) - Receipt %s",
                            actorId, amount, tenantId, unitId, receipt));
        }
        return inserted;
    }

    /** Changes a payment's status (e.g. PAID to VOIDED) and logs PAYMENT_UPDATED. */
    public boolean updatePaymentStatus(int actorId, int paymentId, String newStatus) {
        String select = "SELECT payment_status FROM payments WHERE payment_id = ?";
        String update = "UPDATE payments SET payment_status = ? WHERE payment_id = ?";
        String oldStatus;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement sel = conn.prepareStatement(select);
             PreparedStatement upd = conn.prepareStatement(update)) {
            sel.setInt(1, paymentId);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                oldStatus = rs.getString("payment_status");
            }
            upd.setString(1, newStatus);
            upd.setInt(2, paymentId);
            if (upd.executeUpdate() == 0) {
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        logDao.logAction(actorId, "PAYMENT_UPDATED",
                "Manager " + actorId + " updated Payment #" + paymentId
                        + " status from " + oldStatus + " to " + newStatus);
        return true;
    }

    public List<Payment> getAllPayments() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT payment_id, tenant_id, unit_id, amount_paid, payment_date, payment_status, receipt_number "
                + "FROM payments ORDER BY payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                payments.add(new Payment(
                        rs.getInt("payment_id"),
                        rs.getInt("tenant_id"),
                        rs.getInt("unit_id"),
                        rs.getDouble("amount_paid"),
                        rs.getString("payment_date"),
                        rs.getString("payment_status"),
                        rs.getString("receipt_number")));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return payments;
    }
}