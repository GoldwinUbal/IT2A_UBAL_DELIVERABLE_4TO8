package com.arms.dao;

import com.arms.model.Tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TenantDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    /**
     * Creates a tenant profile and marks the unit OCCUPIED in one transaction,
     * then records TENANT_ASSIGNED. Fails if the unit is not AVAILABLE.
     */
    public boolean createTenant(int actorId, int userId, String name, String contact, int unitId) {
        String occupy = "UPDATE units SET status = 'OCCUPIED' WHERE unit_id = ? AND status = 'AVAILABLE'";
        String insert = "INSERT INTO tenants (user_id, name, contact, unit_id) VALUES (?, ?, ?, ?)";
        boolean created = false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement occ = conn.prepareStatement(occupy);
                 PreparedStatement ins = conn.prepareStatement(insert)) {
                occ.setInt(1, unitId);
                if (occ.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                ins.setInt(1, userId);
                ins.setString(2, name);
                ins.setString(3, contact);
                ins.setInt(4, unitId);
                ins.executeUpdate();
                conn.commit();
                created = true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Could not create tenant: " + e.getMessage());
            return false;
        }
        if (created) {
            logDao.logAction(actorId, "TENANT_ASSIGNED",
                    "Manager " + actorId + " assigned tenant " + name
                            + " (User ID " + userId + ") to Unit ID " + unitId);
        }
        return created;
    }

    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT tenant_id, user_id, name, contact, unit_id FROM tenants "
                + "WHERE unit_id IS NOT NULL ORDER BY tenant_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tenants.add(new Tenant(
                        rs.getInt("tenant_id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("contact"),
                        rs.getInt("unit_id")));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return tenants;
    }
}