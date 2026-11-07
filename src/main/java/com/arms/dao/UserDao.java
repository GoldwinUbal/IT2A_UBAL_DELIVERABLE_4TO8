package com.arms.dao;

import com.arms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, user.password());
            ps.setString(3, user.role());
            ps.setString(4, user.status());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, username, password, role, status FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
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

    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, username, password, role, status FROM users WHERE user_id = ?";
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

    /** Returns every registered account, regardless of role or status. */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password, role, status FROM users ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return users;
    }

    /** Returns all accounts awaiting approval, across every role. */
    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password, role, status FROM users WHERE status = 'PENDING' ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
        return users;
    }

    /** Approves an account and records USER_APPROVED in the audit log. */
    public boolean approveUser(int adminId, int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE user_id = ?";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        if (updated) {
            logDao.logAction(adminId, "USER_APPROVED",
                    "Admin " + adminId + " approved account for User ID " + userId);
        }
        return updated;
    }

    /** Updates role and status (upper-cased) and records USER_UPDATED in the audit log. */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        Optional<User> before = findById(userId);
        if (before.isEmpty()) {
            return false;
        }
        String sql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        if (updated) {
            User old = before.get();
            logDao.logAction(adminId, "USER_UPDATED",
                    "Admin " + adminId + " changed User " + userId
                            + ": role " + old.role() + " to " + newRole.toUpperCase()
                            + ", status " + old.status() + " to " + newStatus.toUpperCase());
        }
        return updated;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("status"));
    }
}