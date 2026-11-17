package com.arms.dao;

import com.arms.model.Tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Updates a tenant's name and contact, and records TENANT_UPDATED. */
    public boolean updateTenant(int actorId, int tenantId, String name, String contact) {
        Optional<Tenant> before = findById(tenantId);
        if (before.isEmpty()) {
            return false;
        }
        String sql = "UPDATE tenants SET name = ?, contact = ? WHERE tenant_id = ? AND status = 'ACTIVE'";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setInt(3, tenantId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        if (updated) {
            Tenant old = before.get();
            logDao.logAction(actorId, "TENANT_UPDATED",
                    "Manager " + actorId + " updated Tenant ID " + tenantId
                            + ": name " + old.name() + " to " + name
                            + ", contact " + show(old.contact()) + " to " + show(contact));
        }
        return updated;
    }

    /**
     * Ends a tenancy: the tenant becomes INACTIVE and the unit becomes AVAILABLE again,
     * in one transaction. The record and its payments are kept. Records TENANT_ENDED.
     */
    public boolean deactivateTenant(int actorId, int tenantId) {
        String select = "SELECT name, unit_id FROM tenants WHERE tenant_id = ? AND status = 'ACTIVE'";
        String deactivate = "UPDATE tenants SET status = 'INACTIVE' WHERE tenant_id = ? AND status = 'ACTIVE'";
        String free = "UPDATE units SET status = 'AVAILABLE' WHERE unit_id = ? AND status = 'OCCUPIED'";
        String name = null;
        int unitId = 0;
        boolean done = false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement sel = conn.prepareStatement(select);
                 PreparedStatement dea = conn.prepareStatement(deactivate);
                 PreparedStatement fr = conn.prepareStatement(free)) {
                sel.setInt(1, tenantId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    name = rs.getString("name");
                    unitId = rs.getInt("unit_id");
                }
                dea.setInt(1, tenantId);
                if (dea.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                fr.setInt(1, unitId);
                fr.executeUpdate();
                conn.commit();
                done = true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Could not end tenancy: " + e.getMessage());
            return false;
        }
        if (done) {
            logDao.logAction(actorId, "TENANT_ENDED",
                    "Manager " + actorId + " ended tenancy of " + name + " (Tenant ID " + tenantId
                            + "); Unit ID " + unitId + " is available again");
        }
        return done;
    }

    public Optional<Tenant> findById(int tenantId) {
        String sql = "SELECT tenant_id, user_id, name, contact, unit_id, status FROM tenants WHERE tenant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Finds the tenant profile that belongs to a login account, if one exists. */
    public Optional<Tenant> findByUserId(int userId) {
        String sql = "SELECT tenant_id, user_id, name, contact, unit_id, status FROM tenants WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Tenants with an active tenancy. */
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT tenant_id, user_id, name, contact, unit_id, status FROM tenants "
                + "WHERE status = 'ACTIVE' AND unit_id IS NOT NULL ORDER BY tenant_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tenants.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return tenants;
    }

    /** Every tenant profile, active or inactive. */
    public List<Tenant> getAllTenantProfiles() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT tenant_id, user_id, name, contact, unit_id, status FROM tenants ORDER BY tenant_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tenants.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return tenants;
    }

    private Tenant mapRow(ResultSet rs) throws SQLException {
        return new Tenant(
                rs.getInt("tenant_id"),
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("contact"),
                rs.getInt("unit_id"),
                rs.getString("status"));
    }

    private static String show(String contact) {
        return (contact == null || contact.isBlank()) ? "none" : contact;
    }
}