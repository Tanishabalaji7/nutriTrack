package com.nutritracker.dao;

import com.nutritracker.database.DatabaseManager;
import com.nutritracker.model.UserProfile;

import java.sql.*;

public class UserDao {
    private final DatabaseManager dbManager;

    public UserDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public UserProfile getUserProfile(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user profile: " + e.getMessage());
        }
        // Return default if not found
        return new UserProfile();
    }

    public UserProfile getDefaultUser() {
        String sql = "SELECT * FROM users ORDER BY id ASC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToProfile(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting default user: " + e.getMessage());
        }
        return new UserProfile();
    }

    public UserProfile findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ? COLLATE NOCASE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    public UserProfile createUser(UserProfile profile) {
        String sql = "INSERT INTO users (name, email, password, age, gender, height_cm, weight_kg, activity_level, goal, dietary_pref, custom_calorie_target, water_goal_liters, starting_weight_kg, target_weight_kg, gemini_api_key) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getEmail() != null && !profile.getEmail().isEmpty() ? profile.getEmail() : "user" + System.currentTimeMillis() + "@example.com");
            ps.setString(3, profile.getPassword() != null ? profile.getPassword() : "");
            ps.setInt(4, profile.getAge() > 0 ? profile.getAge() : 25);
            ps.setString(5, profile.getGender() != null ? profile.getGender() : "female");
            ps.setDouble(6, profile.getHeightCm() > 0 ? profile.getHeightCm() : 165);
            ps.setDouble(7, profile.getWeightKg() > 0 ? profile.getWeightKg() : 60);
            ps.setString(8, profile.getActivityLevel() != null ? profile.getActivityLevel() : "moderate");
            ps.setString(9, profile.getGoal() != null ? profile.getGoal() : "maintain");
            ps.setString(10, profile.getDietaryPref() != null ? profile.getDietaryPref() : "omnivore");
            ps.setInt(11, profile.getCustomCalorieTarget());
            ps.setDouble(12, profile.getWaterGoalLiters() > 0 ? profile.getWaterGoalLiters() : 2.5);
            ps.setDouble(13, profile.getStartingWeightKg());
            ps.setDouble(14, profile.getTargetWeightKg());
            ps.setString(15, profile.getGeminiApiKey() != null ? profile.getGeminiApiKey() : "");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    profile.setId(rs.getInt(1));
                }
            }
            return profile;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return null;
        }
    }

    public UserProfile authenticate(String email, String password) {
        UserProfile user = findByEmail(email);
        if (user != null) {
            // Simple match or blank password bypass for demo convenience
            if (user.getPassword() == null || user.getPassword().isEmpty() || user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public boolean updateUserProfile(UserProfile profile) {
        String sql = "UPDATE users SET name = ?, email = ?, age = ?, gender = ?, height_cm = ?, weight_kg = ?, " +
                "activity_level = ?, goal = ?, dietary_pref = ?, custom_calorie_target = ?, water_goal_liters = ?, " +
                "starting_weight_kg = ?, target_weight_kg = ?, gemini_api_key = ? " +
                "WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getEmail());
            ps.setInt(3, profile.getAge());
            ps.setString(4, profile.getGender());
            ps.setDouble(5, profile.getHeightCm());
            ps.setDouble(6, profile.getWeightKg());
            ps.setString(7, profile.getActivityLevel());
            ps.setString(8, profile.getGoal());
            ps.setString(9, profile.getDietaryPref());
            ps.setInt(10, profile.getCustomCalorieTarget());
            ps.setDouble(11, profile.getWaterGoalLiters());
            ps.setDouble(12, profile.getStartingWeightKg());
            ps.setDouble(13, profile.getTargetWeightKg());
            ps.setString(14, profile.getGeminiApiKey() != null ? profile.getGeminiApiKey() : "");
            ps.setInt(15, profile.getId() > 0 ? profile.getId() : 1);

            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user profile: " + e.getMessage());
            return false;
        }
    }

    public boolean updateWeight(int userId, double currentWeightKg, double targetWeightKg, double startingWeightKg) {
        String sql = "UPDATE users SET weight_kg = ?, target_weight_kg = ?" + 
                     (startingWeightKg > 0 ? ", starting_weight_kg = ? " : " ") + 
                     "WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, currentWeightKg);
            ps.setDouble(2, targetWeightKg);
            if (startingWeightKg > 0) {
                ps.setDouble(3, startingWeightKg);
                ps.setInt(4, userId);
            } else {
                ps.setInt(3, userId);
            }
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating weights: " + e.getMessage());
            return false;
        }
    }

    public boolean updateGeminiApiKey(int userId, String apiKey) {
        String sql = "UPDATE users SET gemini_api_key = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, apiKey != null ? apiKey.trim() : "");
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating gemini api key: " + e.getMessage());
            return false;
        }
    }

    private UserProfile mapResultSetToProfile(ResultSet rs) throws SQLException {
        UserProfile p = new UserProfile();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        try { p.setEmail(rs.getString("email")); } catch (Exception ignored) {}
        try { p.setPassword(rs.getString("password")); } catch (Exception ignored) {}
        p.setAge(rs.getInt("age"));
        p.setGender(rs.getString("gender"));
        p.setHeightCm(rs.getDouble("height_cm"));
        p.setWeightKg(rs.getDouble("weight_kg"));
        p.setActivityLevel(rs.getString("activity_level"));
        p.setGoal(rs.getString("goal"));
        p.setDietaryPref(rs.getString("dietary_pref"));
        p.setCustomCalorieTarget(rs.getInt("custom_calorie_target"));
        p.setWaterGoalLiters(rs.getDouble("water_goal_liters"));
        try { p.setStartingWeightKg(rs.getDouble("starting_weight_kg")); } catch (Exception ignored) {}
        try { p.setTargetWeightKg(rs.getDouble("target_weight_kg")); } catch (Exception ignored) {}
        try { p.setGeminiApiKey(rs.getString("gemini_api_key")); } catch (Exception ignored) {}
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }
}
