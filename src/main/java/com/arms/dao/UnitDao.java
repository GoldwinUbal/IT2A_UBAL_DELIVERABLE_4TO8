package com.arms.dao;

import com.arms.model.Unit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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