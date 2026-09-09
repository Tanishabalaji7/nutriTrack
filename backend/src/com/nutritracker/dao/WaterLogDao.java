package com.nutritracker.dao;

import com.nutritracker.database.DatabaseManager;
import com.nutritracker.model.WaterLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WaterLogDao {
    private final DatabaseManager dbManager;

    public WaterLogDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public WaterLog addWaterLog(WaterLog log) {
        String sql = "INSERT INTO water_logs (user_id, date, amount_ml) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getUserId() > 0 ? log.getUserId() : 1);
            ps.setString(2, log.getDate());
            ps.setInt(3, log.getAmountMl());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
            return log;
        } catch (SQLException e) {
            System.err.println("Error adding water log: " + e.getMessage());
            return null;
        }
    }

    public int getTotalWaterForDate(int userId, String date) {
        String sql = "SELECT SUM(amount_ml) FROM water_logs WHERE user_id = ? AND date = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting total water: " + e.getMessage());
        }
        return 0;
    }

    public List<WaterLog> getWaterLogsForDate(int userId, String date) {
        List<WaterLog> list = new ArrayList<>();
        String sql = "SELECT * FROM water_logs WHERE user_id = ? AND date = ? ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WaterLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("date"),
                            rs.getInt("amount_ml"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting water logs: " + e.getMessage());
        }
        return list;
    }

    public boolean resetWaterForDate(int userId, String date) {
        String sql = "DELETE FROM water_logs WHERE user_id = ? AND date = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("Error resetting water logs: " + e.getMessage());
            return false;
        }
    }
}
