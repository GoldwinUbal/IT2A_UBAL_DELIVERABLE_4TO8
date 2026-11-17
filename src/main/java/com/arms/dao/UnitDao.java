package com.arms.dao;

import com.arms.model.Unit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UnitDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Adds an AVAILABLE unit and records UNIT_CREATED. */
    public boolean addUnit(int actorId, String unitNumber, String unitType, double monthlyRent) {
        String sql = "INSERT INTO units (unit_number, unit_type, monthly_rent) VALUES (?, ?, ?)";
        boolean inserted;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, unitNumber);
            ps.setString(2, unitType);
            ps.setDouble(3, monthlyRent);
            inserted = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Could not add unit: " + e.getMessage());
            return false;
        }
        if (inserted) {
            logDao.logAction(actorId, "UNIT_CREATED",
                    String.format("Manager %d added unit %s (%s) - Rent: P%.2f",
                            actorId, unitNumber, unitType, monthlyRent));
        }
        return inserted;
    }

    /** Updates a unit's number, type and rent, and records UNIT_UPDATED. */
    public boolean updateUnit(int actorId, int unitId, String unitNumber, String unitType, double monthlyRent) {
        Optional<Unit> before = findById(unitId);
        if (before.isEmpty()) {
            return false;
        }
        String sql = "UPDATE units SET unit_number = ?, unit_type = ?, monthly_rent = ? WHERE unit_id = ?";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, unitNumber);
            ps.setString(2, unitType);
            ps.setDouble(3, monthlyRent);
            ps.setInt(4, unitId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Could not update unit: " + e.getMessage());
            return false;
        }
        if (updated) {
            Unit old = before.get();
            logDao.logAction(actorId, "UNIT_UPDATED",
                    String.format("Manager %d updated Unit ID %d: number %s to %s, type %s to %s, rent P%.2f to P%.2f",
                            actorId, unitId, old.unitNumber(), unitNumber, old.unitType(), unitType,
                            old.monthlyRent(), monthlyRent));
        }
        return updated;
    }

    /** Deactivates an AVAILABLE unit (soft delete, the record is kept) and records UNIT_DEACTIVATED. */
    public boolean deactivateUnit(int actorId, int unitId) {
        String sql = "UPDATE units SET status = 'INACTIVE' WHERE unit_id = ? AND status = 'AVAILABLE'";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        if (updated) {
            logDao.logAction(actorId, "UNIT_DEACTIVATED",
                    "Manager " + actorId + " deactivated Unit ID " + unitId);
        }
        return updated;
    }

    /** Makes an INACTIVE unit AVAILABLE again and records UNIT_REACTIVATED. */
    public boolean reactivateUnit(int actorId, int unitId) {
        String sql = "UPDATE units SET status = 'AVAILABLE' WHERE unit_id = ? AND status = 'INACTIVE'";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        if (updated) {
            logDao.logAction(actorId, "UNIT_REACTIVATED",
                    "Manager " + actorId + " reactivated Unit ID " + unitId);
        }
        return updated;
    }

    public Optional<Unit> findById(int unitId) {
        String sql = "SELECT unit_id, unit_number, unit_type, monthly_rent, status FROM units WHERE unit_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Unit(
                            rs.getInt("unit_id"),
                            rs.getString("unit_number"),
                            rs.getString("unit_type"),
                            rs.getDouble("monthly_rent"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Unit> getAllUnits() {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT unit_id, unit_number, unit_type, monthly_rent, status FROM units ORDER BY unit_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                units.add(new Unit(
                        rs.getInt("unit_id"),
                        rs.getString("unit_number"),
                        rs.getString("unit_type"),
                        rs.getDouble("monthly_rent"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return units;
    }
}